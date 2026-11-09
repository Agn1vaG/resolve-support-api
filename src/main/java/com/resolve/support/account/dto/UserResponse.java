package com.resolve.support.account.dto;

import com.resolve.support.account.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Safe user information. Passwords and password hashes are never included.", example = "{\"id\":1,\"name\":\"Ari Example\",\"email\":\"ari@example.com\",\"role\":\"USER\",\"createdAt\":\"2026-11-09T10:00:00Z\",\"updatedAt\":\"2026-11-09T10:00:00Z\"}")
public record UserResponse(Long id, String name, String email, UserRole role,
                           Instant createdAt, Instant updatedAt) {
}
