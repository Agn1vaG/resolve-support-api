package com.resolve.support.history.dto;

import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.history.entity.TicketHistoryEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Read-only audit event for creation, status changes, or ticket assignment changes.", example = "{\"id\":12,\"ticketId\":42,\"eventType\":\"STATUS_CHANGED\",\"actor\":{\"id\":2,\"name\":\"Sam Agent\",\"role\":\"AGENT\"},\"oldStatus\":\"OPEN\",\"newStatus\":\"IN_PROGRESS\",\"oldAssignee\":null,\"newAssignee\":null,\"changedAt\":\"2026-11-09T10:10:00Z\"}")
public record TicketHistoryResponse(Long id, Long ticketId, TicketHistoryEventType eventType,
                                    UserSummaryResponse actor, TicketStatus oldStatus, TicketStatus newStatus,
                                    UserSummaryResponse oldAssignee, UserSummaryResponse newAssignee, Instant changedAt) {
}
