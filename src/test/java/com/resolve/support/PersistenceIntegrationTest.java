package com.resolve.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import com.resolve.support.account.repository.UserRepository;
import com.resolve.support.comment.entity.Comment;
import com.resolve.support.comment.repository.CommentRepository;
import com.resolve.support.history.entity.TicketHistory;
import com.resolve.support.history.repository.TicketHistoryRepository;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@AutoConfigureTestDatabase(replace = NONE)
@AutoConfigureMockMvc
@WithMockUser(username = "admin@example.com", roles = "ADMIN")
class PersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET_BASE64", () -> java.util.Base64.getEncoder().encodeToString(
                "resolve-test-jwt-signing-secret-32-bytes!".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Autowired UserRepository userRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired TicketHistoryRepository ticketHistoryRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired MockMvc mockMvc;

    @BeforeEach
    void createMockAuthenticatedUser() {
        userRepository.saveAndFlush(user("admin@example.com", UserRole.ADMIN));
    }

    @Test
    void applicationContextLoadsAndActuatorHealthIsUp() throws Exception {
        assertThat(userRepository).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void userEmailMustBeUnique() {
        userRepository.saveAndFlush(user("same@example.com", UserRole.USER));
        assertThatThrownBy(() -> userRepository.saveAndFlush(user(" SAME@example.com ", UserRole.AGENT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ticketCreatorAndAssigneeRelationshipsPersist() {
        User creator = userRepository.saveAndFlush(user("creator@example.com", UserRole.USER));
        User assignee = userRepository.saveAndFlush(user("agent@example.com", UserRole.AGENT));
        Ticket ticket = new Ticket("Cannot sign in", "Login fails", TicketStatus.OPEN,
                TicketPriority.HIGH, creator);
        ticket.setAssignee(assignee);

        Ticket saved = ticketRepository.saveAndFlush(ticket);
        Ticket reloaded = ticketRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getCreator().getId()).isEqualTo(creator.getId());
        assertThat(reloaded.getAssignee().getId()).isEqualTo(assignee.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE id = ?", String.class, saved.getId()))
                .isEqualTo("OPEN");
        assertThat(jdbcTemplate.queryForObject("SELECT priority FROM tickets WHERE id = ?", String.class, saved.getId()))
                .isEqualTo("HIGH");
        assertThat(ticketRepository.findByCreator_Id(creator.getId(), PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(ticketRepository.findByAssignee_Id(assignee.getId(), PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test
    void commentAndHistoryBelongToTheExpectedTicket() {
        User creator = userRepository.saveAndFlush(user("owner@example.com", UserRole.USER));
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket("Issue", "Details", TicketStatus.OPEN,
                TicketPriority.MEDIUM, creator));

        Comment comment = commentRepository.saveAndFlush(new Comment("I can reproduce this", ticket, creator));
        TicketHistory history = ticketHistoryRepository.saveAndFlush(
                new TicketHistory(ticket, creator, TicketStatus.OPEN, TicketStatus.IN_PROGRESS));

        assertThat(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId()))
                .singleElement().satisfies(saved -> {
                    assertThat(saved.getTicket().getId()).isEqualTo(ticket.getId());
                    assertThat(saved.getAuthor().getId()).isEqualTo(creator.getId());
                });
        assertThat(ticketHistoryRepository.findByTicket_IdOrderByChangedAtAscIdAsc(ticket.getId()))
                .singleElement().satisfies(saved -> {
                    assertThat(saved.getOldStatus()).isEqualTo(TicketStatus.OPEN);
                    assertThat(saved.getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
                    assertThat(saved.getActor().getId()).isEqualTo(creator.getId());
                });
        assertThat(comment.getUpdatedAt()).isNotNull();
        assertThat(history.getChangedAt()).isNotNull();
    }

    @Test
    void enumValuesArePersistedAsStringsAndPasswordIsIgnoredByJackson() throws Exception {
        User saved = userRepository.saveAndFlush(user("enum@example.com", UserRole.ADMIN));
        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, saved.getId());
        String json = objectMapper.writeValueAsString(saved);

        assertThat(role).isEqualTo("ADMIN");
        assertThat(json).doesNotContain("password");
    }

    @Test
    void userEndpointsCreateRetrieveAndPaginate() throws Exception {
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"New User","email":"new-user@example.com","password":"temporary"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new-user@example.com"));
        mockMvc.perform(get("/api/v1/users").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void ticketEndpointsCreateWithDefaultsFilterAndRecordStatusHistory() throws Exception {
        User creator = userRepository.findByEmail("admin@example.com").orElseThrow();
        User agent = userRepository.saveAndFlush(user("api-agent@example.com", UserRole.AGENT));
        String createdJson = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Printer issue","description":"Paper jam","assigneeId":%d}
                                """.formatted(agent.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.assignee.name").value("Test User"))
                .andReturn().getResponse().getContentAsString();
        long ticketId = objectMapper.readTree(createdJson).get("id").asLong();
        String createdAt = objectMapper.readTree(createdJson).get("createdAt").asText();

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Printer issue"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creator.name").value("Test User"));
        mockMvc.perform(get("/api/v1/tickets").param("status", "IN_PROGRESS")
                        .param("priority", "MEDIUM").param("assigneeId", agent.getId().toString())
                        .param("creatorId", creator.getId().toString())
                        .param("createdFrom", java.time.Instant.parse(createdAt).minusSeconds(1).toString())
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/tickets/created-by/{id}", creator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/tickets/assigned-to/{id}", agent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/tickets").param("page", "0").param("size", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
        mockMvc.perform(get("/api/v1/tickets/{ticketId}/history", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].oldStatus").doesNotExist())
                .andExpect(jsonPath("$[0].newStatus").value("OPEN"))
                .andExpect(jsonPath("$[1].oldStatus").value("OPEN"))
                .andExpect(jsonPath("$[1].newStatus").value("IN_PROGRESS"));
    }

    @Test
    void ticketPaginationSupportsDefaultsCustomPagesSortingAndRejectsInvalidProperties() throws Exception {
        User creator = userRepository.findByEmail("admin@example.com").orElseThrow();
        Ticket oldest = ticketRepository.saveAndFlush(new Ticket("Oldest ticket", "Details", TicketStatus.OPEN,
                TicketPriority.LOW, creator));
        Ticket middle = ticketRepository.saveAndFlush(new Ticket("Middle ticket", "Details", TicketStatus.OPEN,
                TicketPriority.MEDIUM, creator));
        Ticket newest = ticketRepository.saveAndFlush(new Ticket("Newest ticket", "Details", TicketStatus.OPEN,
                TicketPriority.HIGH, creator));

        jdbcTemplate.update("UPDATE tickets SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")), oldest.getId());
        jdbcTemplate.update("UPDATE tickets SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-02T00:00:00Z")), middle.getId());
        jdbcTemplate.update("UPDATE tickets SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-03T00:00:00Z")), newest.getId());

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/tickets").param("page", "1").param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Middle ticket"));

        mockMvc.perform(get("/api/v1/tickets").param("sort", "missingProperty,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocumentsPageableArgumentsAsSeparateQueryParameters() throws Exception {
        JsonNode openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertPageableQueryParameters(openApi, "/api/v1/tickets");
        assertPageableQueryParameters(openApi, "/api/v1/tickets/created-by/{userId}");
        assertPageableQueryParameters(openApi, "/api/v1/tickets/assigned-to/{agentId}");
        assertPageableQueryParameters(openApi, "/api/v1/users");
        assertPageableQueryParameters(openApi, "/api/v1/admin/users");
    }

    private void assertPageableQueryParameters(JsonNode openApi, String path) {
        JsonNode parameters = openApi.at("/paths/" + path.replace("/", "~1") + "/get/parameters");
        Set<String> queryNames = new HashSet<>();
        parameters.forEach(parameter -> {
            if ("query".equals(parameter.path("in").asText())) {
                queryNames.add(parameter.path("name").asText());
            }
        });
        assertThat(queryNames).contains("page", "size", "sort").doesNotContain("pageable");
    }

    @Test
    void commentEndpointsCreateAndRetrieveForTicket() throws Exception {
        User creator = userRepository.saveAndFlush(user("comment-owner@example.com", UserRole.USER));
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket("Comment ticket", "Description", TicketStatus.OPEN,
                TicketPriority.MEDIUM, creator));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticket.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"First reply\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("First reply"));
        mockMvc.perform(get("/api/v1/tickets/{id}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketId").value(ticket.getId()));
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticket.getId())
                        .contentType(APPLICATION_JSON).content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingResourcesAndInvalidRequestsReturnStructuredErrors() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/tickets/999999"));
        mockMvc.perform(post("/api/v1/users").contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
        mockMvc.perform(get("/api/v1/tickets").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request contains an invalid or unsupported value."));
    }

    private User user(String email, UserRole role) {
        return new User("Test User", email, "test-hash-value", role);
    }
}
