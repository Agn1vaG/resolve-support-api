package com.resolve.support.ticket.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;

import java.time.Instant;

public record TicketResponse(Long id, String title, String description, TicketStatus status,
                             TicketPriority priority, UserSummaryResponse creator,
                             UserSummaryResponse assignee, Instant createdAt, Instant updatedAt) {
}
