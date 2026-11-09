package com.resolve.support.account.dto;

import com.resolve.support.account.entity.UserRole;

public record UserSummaryResponse(Long id, String name, UserRole role) {
}
