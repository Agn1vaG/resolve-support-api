package com.resolve.support.history.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.history.entity.TicketHistoryEventType;

import java.time.Instant;

public record TicketHistoryResponse(Long id, Long ticketId, TicketHistoryEventType eventType,
                                    UserSummaryResponse actor, TicketStatus oldStatus, TicketStatus newStatus,
                                    UserSummaryResponse oldAssignee, UserSummaryResponse newAssignee, Instant changedAt) {
}
