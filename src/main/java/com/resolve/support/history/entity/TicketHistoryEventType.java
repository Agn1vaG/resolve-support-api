package com.resolve.support.history.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket audit event types emitted by the workflow.")
public enum TicketHistoryEventType {
    TICKET_CREATED,
    STATUS_CHANGED,
    ASSIGNED,
    UNASSIGNED,
    REASSIGNED
}
