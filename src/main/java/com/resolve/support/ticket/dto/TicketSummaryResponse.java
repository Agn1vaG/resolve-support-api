package com.resolve.support.ticket.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;

import java.time.Instant;

public record TicketSummaryResponse(Long id, String title, TicketStatus status, TicketPriority priority,
                                    UserSummaryResponse creator, UserSummaryResponse assignee,
                                    Instant createdAt, Instant updatedAt) {
}
