package com.resolve.support.history.controller;

import com.resolve.support.history.dto.TicketHistoryResponse;
import com.resolve.support.history.mapper.TicketHistoryMapper;
import com.resolve.support.history.service.TicketHistoryService;
import com.resolve.support.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/history")
@Tag(name = "Ticket History")
@SecurityRequirement(name = "bearerAuth")
public class TicketHistoryController {

    private final TicketHistoryService historyService;
    private final TicketHistoryMapper historyMapper;

    public TicketHistoryController(TicketHistoryService historyService, TicketHistoryMapper historyMapper) {
        this.historyService = historyService;
        this.historyMapper = historyMapper;
    }

    @GetMapping
    @Operation(summary = "Get ticket audit history", description = "Read-only chronological audit events. Access is limited to the ticket creator, assigned agent, or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chronological ticket history"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No access to this ticket", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public List<TicketHistoryResponse> getForTicket(@PathVariable Long ticketId) {
        return historyService.getByTicket(ticketId).stream().map(historyMapper::toResponse).toList();
    }
}
