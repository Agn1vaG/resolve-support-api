package com.resolve.support.ticket.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket priority, ordered from LOW to URGENT.")
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
