package com.resolve.support.ticket.controller;

import com.resolve.support.ticket.dto.CreateTicketRequest;
import com.resolve.support.ticket.dto.AssignTicketRequest;
import com.resolve.support.ticket.dto.ChangeTicketStatusRequest;
import com.resolve.support.ticket.dto.TicketResponse;
import com.resolve.support.ticket.dto.TicketSummaryResponse;
import com.resolve.support.ticket.dto.UpdateTicketRequest;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.ticket.mapper.TicketMapper;
import com.resolve.support.ticket.service.TicketService;
import com.resolve.support.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    public TicketController(TicketService ticketService, TicketMapper ticketMapper) {
        this.ticketService = ticketService;
        this.ticketMapper = ticketMapper;
    }

    @PostMapping
    @Operation(summary = "Create a ticket", description = "USER or ADMIN creates a ticket. The creator comes from the JWT. New tickets start OPEN; priority defaults to MEDIUM. Only admins may set assigneeId.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateTicketRequest.class), examples = @ExampleObject(value = "{\"title\":\"Cannot sign in\",\"description\":\"Login fails after password reset\",\"priority\":\"HIGH\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created", content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to create this ticket or assign during creation", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketMapper.toResponse(ticketService.create(request));
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ticket", description = "Ticket creator, assigned agent, or admin may retrieve the ticket.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket details", content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Ticket is outside the caller's access scope", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TicketResponse getById(@PathVariable Long id) {
        return ticketMapper.toResponse(ticketService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Search tickets", description = "Returns a page of tickets, scoped by the authenticated role. Filters are applied in PostgreSQL. Page size is capped at 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of ticket summaries"),
            @ApiResponse(responseCode = "400", description = "Invalid enum, date range, page, or sort value", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Requested owner/assignee filter is outside the caller's scope", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<TicketSummaryResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @Parameter(description = "Filter by ticket status")
            @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Filter by ticket priority")
            @RequestParam(required = false) TicketPriority priority,
            @Parameter(description = "Filter by assigned agent id")
            @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "Filter by creator user id")
            @RequestParam(required = false) Long creatorId,
            @Parameter(description = "Inclusive creation lower bound (ISO-8601 instant)", example = "2026-11-01T00:00:00Z")
            @RequestParam(required = false) Instant createdFrom,
            @Parameter(description = "Inclusive creation upper bound (ISO-8601 instant)", example = "2026-11-30T23:59:59Z")
            @RequestParam(required = false) Instant createdTo) {
        return ticketService.getAll(pageable, status, priority, assigneeId, creatorId, createdFrom, createdTo)
                .map(ticketMapper::toSummary);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket details", description = "Admin-only partial update of title, description, and/or priority. Omitted properties are preserved; status and assignment have dedicated PATCH operations.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = UpdateTicketRequest.class), examples = @ExampleObject(value = "{\"title\":\"Updated sign-in issue\",\"priority\":\"URGENT\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket updated", content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or no fields supplied", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketMapper.toResponse(ticketService.update(id, request));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign or unassign a ticket", description = "Admin-only. agentId must identify an AGENT; explicitly set it to null to unassign. Changes are audited.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = AssignTicketRequest.class), examples = @ExampleObject(value = "{\"agentId\":42}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment updated", content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing agentId or target is not an AGENT", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket or target user not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TicketResponse assign(@PathVariable Long id, @RequestBody AssignTicketRequest request) {
        return ticketMapper.toResponse(ticketService.assign(id, request.isAgentIdProvided(), request.getAgentId()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change ticket status", description = "Admin or the currently assigned agent may change status using the allowed transition matrix. A repeated status is a no-op.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChangeTicketStatusRequest.class), examples = @ExampleObject(value = "{\"status\":\"IN_PROGRESS\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket status updated", content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or status transition", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Status change is not allowed for this role or assignment", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TicketResponse changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody ChangeTicketStatusRequest request) {
        return ticketMapper.toResponse(ticketService.changeStatus(id, request.status()));
    }

    @GetMapping("/created-by/{userId}")
    @Operation(summary = "List tickets created by a user", description = "Users can request their own tickets; admins can request any user's tickets. Paginated, up to 100 tickets per page.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of ticket summaries"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Requested creator is outside the caller's scope", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<TicketSummaryResponse> getCreatedBy(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.getCreatedBy(userId, pageable).map(ticketMapper::toSummary);
    }

    @GetMapping("/assigned-to/{agentId}")
    @Operation(summary = "List tickets assigned to an agent", description = "Agents can request their own queue; admins can request any agent's queue. Paginated, up to 100 tickets per page.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of ticket summaries"),
            @ApiResponse(responseCode = "400", description = "Target account is not an AGENT", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Requested agent queue is outside the caller's scope", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<TicketSummaryResponse> getAssignedTo(
            @PathVariable Long agentId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.getAssignedTo(agentId, pageable).map(ticketMapper::toSummary);
    }
}
