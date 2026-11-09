package com.resolve.support.history.service;

import com.resolve.support.history.entity.TicketHistory;
import com.resolve.support.history.entity.TicketHistoryEventType;
import com.resolve.support.history.repository.TicketHistoryRepository;
import com.resolve.support.exception.ResourceNotFoundException;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.ticket.repository.TicketRepository;
import com.resolve.support.account.entity.User;
import com.resolve.support.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TicketHistoryService {

    private final TicketHistoryRepository historyRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    public TicketHistoryService(TicketHistoryRepository historyRepository, TicketRepository ticketRepository,
                                CurrentUserService currentUserService) {
        this.historyRepository = historyRepository;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
    }

    public List<TicketHistory> getByTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        User actor = currentUserService.getCurrentUser();
        boolean allowed = switch (actor.getRole()) {
            case ADMIN -> true;
            case USER -> ticket.getCreator().getId().equals(actor.getId());
            case AGENT -> ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actor.getId());
        };
        if (!allowed) throw new AccessDeniedException("You do not have access to this ticket's history.");
        return historyRepository.findByTicket_IdOrderByChangedAtAscIdAsc(ticketId);
    }

    @Transactional
    public TicketHistory recordStatusChange(Ticket ticket, User actor,
                                            TicketStatus oldStatus, TicketStatus newStatus) {
        TicketHistoryEventType type = oldStatus == null
                ? TicketHistoryEventType.TICKET_CREATED : TicketHistoryEventType.STATUS_CHANGED;
        return historyRepository.save(new TicketHistory(ticket, actor, type, oldStatus, newStatus, null, null));
    }

    @Transactional
    public TicketHistory recordTicketCreated(Ticket ticket, User actor) {
        return historyRepository.save(new TicketHistory(ticket, actor, TicketHistoryEventType.TICKET_CREATED,
                null, TicketStatus.OPEN, null, ticket.getAssignee()));
    }

    @Transactional
    public TicketHistory recordAssignmentChange(Ticket ticket, User actor, User oldAssignee, User newAssignee) {
        TicketHistoryEventType type = oldAssignee == null ? TicketHistoryEventType.ASSIGNED
                : newAssignee == null ? TicketHistoryEventType.UNASSIGNED : TicketHistoryEventType.REASSIGNED;
        return historyRepository.save(new TicketHistory(ticket, actor, type, null, null, oldAssignee, newAssignee));
    }
}
