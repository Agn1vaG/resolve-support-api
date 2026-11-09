package com.resolve.support.comment.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Comment including its authenticated author and timestamps.", example = "{\"id\":6,\"message\":\"I restarted the device and can sign in now.\",\"ticketId\":42,\"author\":{\"id\":2,\"name\":\"Sam Agent\",\"role\":\"AGENT\"},\"createdAt\":\"2026-11-09T10:10:00Z\",\"updatedAt\":\"2026-11-09T10:10:00Z\"}")
public record CommentResponse(Long id, String message, Long ticketId, UserSummaryResponse author,
                              Instant createdAt, Instant updatedAt) {
}
