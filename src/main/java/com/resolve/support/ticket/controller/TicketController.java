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
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    public TicketController(TicketService ticketService, TicketMapper ticketMapper) {
        this.ticketService = ticketService;
        this.ticketMapper = ticketMapper;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketMapper.toResponse(ticketService.create(request));
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketMapper.toResponse(ticketService.getById(id));
    }

    @GetMapping
    public Page<TicketSummaryResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo) {
        return ticketService.getAll(pageable, status, priority, assigneeId, creatorId, createdFrom, createdTo)
                .map(ticketMapper::toSummary);
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketMapper.toResponse(ticketService.update(id, request));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assign(@PathVariable Long id, @RequestBody AssignTicketRequest request) {
        return ticketMapper.toResponse(ticketService.assign(id, request.isAgentIdProvided(), request.getAgentId()));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody ChangeTicketStatusRequest request) {
        return ticketMapper.toResponse(ticketService.changeStatus(id, request.status()));
    }

    @GetMapping("/created-by/{userId}")
    public Page<TicketSummaryResponse> getCreatedBy(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.getCreatedBy(userId, pageable).map(ticketMapper::toSummary);
    }

    @GetMapping("/assigned-to/{agentId}")
    public Page<TicketSummaryResponse> getAssignedTo(
            @PathVariable Long agentId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.getAssignedTo(agentId, pageable).map(ticketMapper::toSummary);
    }
}
