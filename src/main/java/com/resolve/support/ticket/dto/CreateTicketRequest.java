package com.resolve.support.ticket.dto;

import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket submission. Creator is derived from the authenticated user. New tickets start OPEN; priority defaults to MEDIUM.", example = "{\"title\":\"Cannot sign in\",\"description\":\"Login fails after password reset\",\"priority\":\"HIGH\"}")
public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @Schema(description = "Only OPEN is accepted on creation; omit this property for the default.", example = "OPEN")
        TicketStatus status,
        @Schema(description = "Defaults to MEDIUM when omitted.", example = "HIGH")
        TicketPriority priority,
        @Schema(description = "Optional initial AGENT assignment; only admins may supply it.", example = "42")
        Long assigneeId) {
}
