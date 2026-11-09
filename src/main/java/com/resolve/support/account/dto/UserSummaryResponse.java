package com.resolve.support.account.dto;

import com.resolve.support.account.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Minimal user identity used in ticket, comment, and audit responses.", example = "{\"id\":1,\"name\":\"Ari Example\",\"role\":\"USER\"}")
public record UserSummaryResponse(Long id, String name, UserRole role) {
}
