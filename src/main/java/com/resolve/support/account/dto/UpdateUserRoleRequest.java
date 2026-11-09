package com.resolve.support.account.dto;

import com.resolve.support.account.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull UserRole role) {
}
