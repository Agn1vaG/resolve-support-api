package com.resolve.support.account.dto;

import com.resolve.support.account.entity.UserRole;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, UserRole role,
                           Instant createdAt, Instant updatedAt) {
}
