package com.resolve.support.ticket.service;

import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import com.resolve.support.account.repository.UserRepository;
import com.resolve.support.exception.InvalidRequestException;
import com.resolve.support.exception.ResourceNotFoundException;
import com.resolve.support.history.service.TicketHistoryService;
import com.resolve.support.security.CurrentUserService;
import com.resolve.support.ticket.dto.CreateTicketRequest;
import com.resolve.support.ticket.dto.UpdateTicketRequest;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.ticket.repository.TicketRepository;
import com.resolve.support.ticket.repository.TicketSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;
    private final CurrentUserService currentUserService;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository,
                         TicketHistoryService ticketHistoryService, CurrentUserService currentUserService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketHistoryService = ticketHistoryService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Ticket create(CreateTicketRequest request) {
        User creator = currentUserService.getCurrentUser();
        if (creator.getRole() == UserRole.AGENT) throw new AccessDeniedException("Agents cannot create tickets.");
        if (request.assigneeId() != null && creator.getRole() != UserRole.ADMIN)
            throw new AccessDeniedException("Only admins can assign tickets.");
        if (request.status() != null && request.status() != TicketStatus.OPEN)
            throw new InvalidRequestException("New tickets must start in OPEN status.");

        Ticket ticket = new Ticket(request.title(), request.description(), TicketStatus.OPEN,
                request.priority() == null ? TicketPriority.MEDIUM : request.priority(), creator);
        if (request.assigneeId() != null) ticket.setAssignee(findAgent(request.assigneeId()));
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.recordTicketCreated(saved, creator);
        return saved;
    }

    public Ticket getById(Long id) {
        Ticket ticket = findTicket(id);
        requireCanRead(ticket, currentUserService.getCurrentUser());
        return ticket;
    }

    public Page<Ticket> getAll(Pageable pageable, TicketStatus status, TicketPriority priority,
                              Long assigneeId, Long creatorId, Instant createdFrom, Instant createdTo) {
        User actor = currentUserService.getCurrentUser();
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo))
            throw new InvalidRequestException("createdFrom must be earlier than or equal to createdTo.");
        if (actor.getRole() == UserRole.USER) {
            if (creatorId != null && !creatorId.equals(actor.getId()))
                throw new AccessDeniedException("You may only filter tickets you created.");
            creatorId = actor.getId();
        } else if (actor.getRole() == UserRole.AGENT) {
            if (assigneeId != null && !assigneeId.equals(actor.getId()))
                throw new AccessDeniedException("Agents may only filter tickets assigned to them.");
            assigneeId = actor.getId();
        }
        Specification<Ticket> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(TicketSpecifications.statusIs(status))
                .and(TicketSpecifications.priorityIs(priority))
                .and(TicketSpecifications.assigneeIs(assigneeId))
                .and(TicketSpecifications.creatorIs(creatorId))
                .and(TicketSpecifications.createdAtFrom(createdFrom))
                .and(TicketSpecifications.createdAtTo(createdTo));
        return ticketRepository.findAll(spec, pageable);
    }

    @Transactional
    public Ticket update(Long id, UpdateTicketRequest request) {
        User actor = currentUserService.getCurrentUser();
        requireAdmin(actor);
        if (!request.hasChanges()) throw new InvalidRequestException("At least one ticket field must be provided.");
        Ticket ticket = findTicket(id);
        if (request.title() != null) ticket.setTitle(request.title());
        if (request.description() != null) ticket.setDescription(request.description());
        if (request.priority() != null) ticket.setPriority(request.priority());
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket assign(Long id, boolean agentIdProvided, Long agentId) {
        User actor = currentUserService.getCurrentUser();
        requireAdmin(actor);
        if (!agentIdProvided) throw new InvalidRequestException("agentId must be provided; use null to unassign.");
        Ticket ticket = findTicket(id);
        User assignee = agentId == null ? null : findAgent(agentId);
        if (ticket.getAssignee() == assignee || (ticket.getAssignee() != null && assignee != null
                && ticket.getAssignee().getId().equals(assignee.getId()))) return ticket;
        User previousAssignee = ticket.getAssignee();
        ticket.setAssignee(assignee);
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.recordAssignmentChange(saved, actor, previousAssignee, assignee);
        return saved;
    }

    @Transactional
    public Ticket changeStatus(Long id, TicketStatus newStatus) {
        User actor = currentUserService.getCurrentUser();
        Ticket ticket = findTicket(id);
        if (actor.getRole() == UserRole.USER) throw new AccessDeniedException("Users cannot change ticket status.");
        if (actor.getRole() == UserRole.AGENT
                && (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(actor.getId())))
            throw new AccessDeniedException("Agents may change status only on tickets assigned to them.");
        TicketStatus oldStatus = ticket.getStatus();
        if (oldStatus == newStatus) return ticket;
        if (!isAllowedTransition(oldStatus, newStatus))
            throw new InvalidRequestException("Status transition from " + oldStatus + " to " + newStatus + " is not allowed.");
        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.recordStatusChange(saved, actor, oldStatus, newStatus);
        return saved;
    }

    public Page<Ticket> getCreatedBy(Long userId, Pageable pageable) {
        User actor = currentUserService.getCurrentUser();
        if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("User", userId);
        if (actor.getRole() != UserRole.ADMIN && (actor.getRole() != UserRole.USER || !actor.getId().equals(userId)))
            throw new AccessDeniedException("You may only view tickets you created.");
        return ticketRepository.findAll(TicketSpecifications.creatorIs(userId), pageable);
    }

    public Page<Ticket> getAssignedTo(Long agentId, Pageable pageable) {
        User agent = findUser(agentId);
        if (agent.getRole() != UserRole.AGENT) throw new InvalidRequestException("The requested assignee is not an AGENT.");
        User actor = currentUserService.getCurrentUser();
        if (actor.getRole() != UserRole.ADMIN && (actor.getRole() != UserRole.AGENT || !actor.getId().equals(agentId)))
            throw new AccessDeniedException("Agents may only view their assigned tickets.");
        return ticketRepository.findAll(TicketSpecifications.assigneeIs(agentId), pageable);
    }

    private boolean isAllowedTransition(TicketStatus oldStatus, TicketStatus next) {
        return switch (oldStatus) {
            case OPEN -> next == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED || next == TicketStatus.OPEN;
            case RESOLVED -> next == TicketStatus.CLOSED || next == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    private void requireCanRead(Ticket ticket, User actor) {
        boolean allowed = switch (actor.getRole()) {
            case ADMIN -> true;
            case USER -> ticket.getCreator().getId().equals(actor.getId());
            case AGENT -> ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actor.getId());
        };
        if (!allowed) throw new AccessDeniedException("You do not have access to this ticket.");
    }

    private void requireAdmin(User actor) {
        if (actor.getRole() != UserRole.ADMIN) throw new AccessDeniedException("Only admins may perform this operation.");
    }

    private User findAgent(Long id) {
        User agent = findUser(id);
        if (agent.getRole() != UserRole.AGENT)
            throw new InvalidRequestException("Tickets can only be assigned to users with the AGENT role.");
        return agent;
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
