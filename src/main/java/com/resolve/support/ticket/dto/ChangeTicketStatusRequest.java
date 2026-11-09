package com.resolve.support.ticket.dto;

import com.resolve.support.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTicketStatusRequest(@NotNull TicketStatus status) { }
