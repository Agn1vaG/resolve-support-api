package com.resolve.support.account.dto;

import com.resolve.support.account.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Role update request. The admin role-management endpoint accepts USER or AGENT only.")
public record UpdateUserRoleRequest(@Schema(example = "AGENT") @NotNull UserRole role) {
}
