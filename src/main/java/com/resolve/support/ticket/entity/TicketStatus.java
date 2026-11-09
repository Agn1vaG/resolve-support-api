package com.resolve.support.ticket.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket status. Allowed transitions: OPEN to IN_PROGRESS; IN_PROGRESS to RESOLVED or OPEN; RESOLVED to CLOSED or IN_PROGRESS; CLOSED is terminal.")
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
