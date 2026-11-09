package com.resolve.support.ticket.dto;

import com.resolve.support.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requested ticket status transition.", example = "{\"status\":\"IN_PROGRESS\"}")
public record ChangeTicketStatusRequest(@Schema(example = "IN_PROGRESS") @NotNull TicketStatus status) { }
