package com.resolve.support.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = java.util.Base64.getEncoder().encodeToString(
            "jwt-unit-test-secret-should-be-32-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private JwtProperties properties;
    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecretBase64(SECRET);
        properties.setExpirationMs(900000);
        jwtService = new JwtService(properties);
        userDetails = User.withUsername("person@example.com").password("unused").roles("USER").build();
    }

    @Test
    void generatesAndValidatesSignedToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractUsername(token)).isEqualTo("person@example.com");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();
        String expired = Jwts.builder().subject(userDetails.getUsername())
                .issuedAt(Date.from(now.minusSeconds(120))).expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();

        assertThatThrownBy(() -> jwtService.extractUsername(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsMalformedAndInvalidSignatureTokens() {
        String wrongSignature = Jwts.builder().subject(userDetails.getUsername())
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(
                        "a-different-32-byte-key-for-tests-123".getBytes(java.nio.charset.StandardCharsets.UTF_8)))))
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername("not-a-jwt")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jwtService.extractUsername(wrongSignature)).isInstanceOf(RuntimeException.class);
    }
}
