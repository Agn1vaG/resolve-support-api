package com.resolve.support.ticket.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Ticket details with safe creator and optional assignee summaries.", example = "{\"id\":42,\"title\":\"Cannot sign in\",\"description\":\"Login fails after password reset\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"creator\":{\"id\":1,\"name\":\"Ari Example\",\"role\":\"USER\"},\"assignee\":null,\"createdAt\":\"2026-11-09T10:00:00Z\",\"updatedAt\":\"2026-11-09T10:00:00Z\"}")
public record TicketResponse(Long id, String title, String description, TicketStatus status,
                             TicketPriority priority, UserSummaryResponse creator,
                             UserSummaryResponse assignee, Instant createdAt, Instant updatedAt) {
}
