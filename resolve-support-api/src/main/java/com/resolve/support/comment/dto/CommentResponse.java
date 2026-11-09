package com.resolve.support.comment.dto;

import com.resolve.support.account.dto.UserSummaryResponse;

import java.time.Instant;

public record CommentResponse(Long id, String message, Long ticketId, UserSummaryResponse author,
                              Instant createdAt, Instant updatedAt) {
}
