package com.resolve.support.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import com.resolve.support.account.repository.UserRepository;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.ticket.repository.TicketRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@AutoConfigureTestDatabase(replace = NONE)
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {
    private static final String TEST_SECRET = java.util.Base64.getEncoder().encodeToString(
            "resolve-security-integration-test-secret-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET_BASE64", () -> TEST_SECRET);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registrationHashesPasswordForcesUserRoleAndRejectsDuplicate() throws Exception {
        String email = email("registered");
        String body = """
                {"name":"New Account","email":"%s","password":"SecurePass123","role":"ADMIN"}
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(UserRole.USER);
        assertThat(stored.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("SecurePass123", stored.getPassword())).isTrue();

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsJwtAndDoesNotRevealWhichCredentialFailed() throws Exception {
        String email = email("login");
        register(email);
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "SecurePass123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        String wrongPassword = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "WrongPass123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email("missing"), "WrongPass123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(wrongPassword).get("message").asText())
                .isEqualTo(objectMapper.readTree(unknownEmail).get("message").asText());
    }

    @Test
    void protectedEndpointRejectsMissingMalformedExpiredAndWrongSignatureTokens() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        assertUnauthorized("not-a-jwt");

        Instant now = Instant.now();
        String expired = Jwts.builder().subject("nobody@example.com")
                .issuedAt(Date.from(now.minusSeconds(120))).expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET))).compact();
        assertUnauthorized(expired);

        String invalidSignature = Jwts.builder().subject("nobody@example.com").issuedAt(new Date())
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(
                        "different-key-for-integration-test-32bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8)))))
                .compact();
        assertUnauthorized(invalidSignature);
    }

    @Test
    void userCanReadOwnTicketButCannotReadAnotherUsersOrAssign() throws Exception {
        String ownerEmail = email("owner");
        String otherEmail = email("other");
        long ownerId = register(ownerEmail);
        register(otherEmail);
        String ownerToken = login(ownerEmail, "SecurePass123");
        String otherToken = login(otherEmail, "SecurePass123");
        String created = mockMvc.perform(post("/api/v1/tickets").header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My issue\",\"description\":\"Details\",\"creatorId\":999999}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long ticketId = objectMapper.readTree(created).get("id").asLong();
        assertThat(objectMapper.readTree(created).get("creator").get("id").asLong()).isEqualTo(ownerId);
        User agent = saveUser(email("agent"), UserRole.AGENT, "AgentPass123");

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets").header("Authorization", bearer(ownerToken))
                        .param("creatorId", Long.toString(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/tickets").header("Authorization", bearer(otherToken))
                        .param("creatorId", Long.toString(ownerId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/tickets/{id}", ticketId).header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":%d}".formatted(agent.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticketId)
                        .header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Owned comment\",\"authorId\":999999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.id").value(ownerId));
        mockMvc.perform(get("/api/v1/tickets/{id}/comments", ticketId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/{id}/history", ticketId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(ownerId)).isPresent();
    }

    @Test
    void agentsAreScopedToTheirAssignmentsAndCanOnlyUpdateAssignedTicketStatus() throws Exception {
        User owner = saveUser(email("ticket-owner"), UserRole.USER, "OwnerPass123");
        User agentOne = saveUser(email("agent-one"), UserRole.AGENT, "AgentPass123");
        User agentTwo = saveUser(email("agent-two"), UserRole.AGENT, "AgentPass123");
        Ticket assignedToTwo = ticketRepository.saveAndFlush(new Ticket("Assigned", "Details", TicketStatus.OPEN,
                TicketPriority.MEDIUM, owner));
        assignedToTwo.setAssignee(agentTwo);
        ticketRepository.saveAndFlush(assignedToTwo);
        String agentOneToken = login(agentOne.getEmail(), "AgentPass123");
        String agentTwoToken = login(agentTwo.getEmail(), "AgentPass123");

        mockMvc.perform(get("/api/v1/tickets/{id}", assignedToTwo.getId())
                        .header("Authorization", bearer(agentOneToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", assignedToTwo.getId())
                        .header("Authorization", bearer(agentOneToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(agentOneToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/{id}", assignedToTwo.getId())
                        .header("Authorization", bearer(agentTwoToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", assignedToTwo.getId())
                        .header("Authorization", bearer(agentTwoToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tickets/{ticketId}/history", assignedToTwo.getId())
                        .header("Authorization", bearer(agentTwoToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor.id").value(agentTwo.getId()));
    }

    @Test
    void adminCanManageUsersAndExistingPlaintextPasswordsCannotAuthenticate() throws Exception {
        User admin = saveUser(email("admin"), UserRole.ADMIN, "AdminPass123");
        User target = saveUser(email("target"), UserRole.USER, "TargetPass123");
        User legacy = userRepository.saveAndFlush(new User("Legacy", email("legacy"), "old-plaintext", UserRole.USER));
        String adminToken = login(admin.getEmail(), "AdminPass123");

        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", target.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket("Admin view", "Details", TicketStatus.OPEN,
                TicketPriority.LOW, target));
        mockMvc.perform(get("/api/v1/tickets/{id}", ticket.getId()).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(legacy.getEmail(), "old-plaintext")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminAssignmentIsRoleCheckedAuditedAndSupportsUnassignment() throws Exception {
        User admin = saveUser(email("assignment-admin"), UserRole.ADMIN, "AdminPass123");
        User owner = saveUser(email("assignment-owner"), UserRole.USER, "OwnerPass123");
        User agent = saveUser(email("assignment-agent"), UserRole.AGENT, "AgentPass123");
        User otherAgent = saveUser(email("assignment-agent-two"), UserRole.AGENT, "AgentPass123");
        User nonAgent = saveUser(email("assignment-user"), UserRole.USER, "OtherPass123");
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket("Assignment", "Details", TicketStatus.OPEN,
                TicketPriority.MEDIUM, owner));
        String adminToken = login(admin.getEmail(), "AdminPass123");
        String ownerToken = login(owner.getEmail(), "OwnerPass123");
        String agentToken = login(agent.getEmail(), "AgentPass123");

        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", ticket.getId())
                        .header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":%d}".formatted(agent.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", ticket.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":%d}".formatted(nonAgent.getId())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", ticket.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":999999}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", 999999L)
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":%d}".formatted(agent.getId())))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", ticket.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":%d}".formatted(agent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(agent.getId()));
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticket.getId())
                        .header("Authorization", bearer(agentToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Agent reply\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.id").value(agent.getId()))
                .andExpect(jsonPath("$.createdAt").exists());
        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", ticket.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":%d}".formatted(otherAgent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(otherAgent.getId()));
        mockMvc.perform(patch("/api/v1/tickets/{id}/assign", ticket.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value(org.hamcrest.Matchers.nullValue()));
        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getAssignee()).isNull();
        mockMvc.perform(get("/api/v1/tickets/{id}/history", ticket.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].eventType").value("REASSIGNED"))
                .andExpect(jsonPath("$[2].eventType").value("UNASSIGNED"));
    }

    @Test
    void adminRoleManagementCannotCreateAdminAndNeverReturnsPasswordHash() throws Exception {
        User admin = saveUser(email("role-admin"), UserRole.ADMIN, "AdminPass123");
        User target = saveUser(email("role-target"), UserRole.USER, "TargetPass123");
        User agent = saveUser(email("role-agent"), UserRole.AGENT, "AgentPass123");
        String adminToken = login(admin.getEmail(), "AdminPass123");
        String agentToken = login(agent.getEmail(), "AgentPass123");

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(agentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", target.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", target.getId())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/v1/tickets").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    private long register(String email) throws Exception {
        String body = """
                {"name":"Account","email":"%s","password":"SecurePass123"}
                """.formatted(email);
        String response = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private User saveUser(String email, UserRole role, String password) {
        return userRepository.saveAndFlush(new User("Test " + role, email, passwordEncoder.encode(password), role));
    }

    private String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String bearer(String token) { return "Bearer " + token; }

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
