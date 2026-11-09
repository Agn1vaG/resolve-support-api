package com.resolve.support.ticket.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Compact ticket representation used in paginated lists.")
public record TicketSummaryResponse(Long id, String title, TicketStatus status, TicketPriority priority,
                                    UserSummaryResponse creator, UserSummaryResponse assignee,
                                    Instant createdAt, Instant updatedAt) {
}
