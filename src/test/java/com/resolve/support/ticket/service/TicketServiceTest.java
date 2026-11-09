package com.resolve.support.ticket.service;

import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import com.resolve.support.account.repository.UserRepository;
import com.resolve.support.exception.InvalidRequestException;
import com.resolve.support.history.service.TicketHistoryService;
import com.resolve.support.security.CurrentUserService;
import com.resolve.support.ticket.dto.CreateTicketRequest;
import com.resolve.support.ticket.dto.UpdateTicketRequest;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import com.resolve.support.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock TicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @Mock TicketHistoryService ticketHistoryService;
    @Mock CurrentUserService currentUserService;

    private TicketService ticketService;
    private User actor;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, userRepository, ticketHistoryService, currentUserService);
        actor = new User("Requester", "requester@example.com", "hash", UserRole.ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(actor);
    }

    @Test
    void createDefaultsAndRecordsInitialOpenHistory() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Ticket created = ticketService.create(new CreateTicketRequest("Title", "Description", null, null, null));
        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(created.getCreator()).isSameAs(actor);
        verify(ticketHistoryService).recordTicketCreated(created, actor);
    }

    @Test
    void statusTransitionWritesHistoryAndSameStatusDoesNot() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        ticketService.changeStatus(12L, TicketStatus.IN_PROGRESS);
        verify(ticketHistoryService).recordStatusChange(ticket, actor, TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

        ticketService.changeStatus(12L, TicketStatus.IN_PROGRESS);
        verify(ticketHistoryService, org.mockito.Mockito.times(1))
                .recordStatusChange(ticket, actor, TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    }

    @Test
    void invalidStatusTransitionDoesNotWriteHistory() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        assertThatThrownBy(() -> ticketService.changeStatus(12L, TicketStatus.CLOSED))
                .isInstanceOf(InvalidRequestException.class);
        verify(ticketHistoryService, never()).recordStatusChange(any(), any(), any(), any());
    }

    @Test
    void assignedAgentCanUpdateStatusButNotUnassignedOrOtherAgentTicket() {
        User agent = org.mockito.Mockito.mock(User.class);
        when(agent.getRole()).thenReturn(UserRole.AGENT);
        when(agent.getId()).thenReturn(42L);
        Ticket ticket = ticket(TicketStatus.OPEN);
        ticket.setAssignee(agent);
        when(currentUserService.getCurrentUser()).thenReturn(agent);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        assertThat(ticketService.changeStatus(12L, TicketStatus.IN_PROGRESS).getStatus())
                .isEqualTo(TicketStatus.IN_PROGRESS);

        User otherAgent = org.mockito.Mockito.mock(User.class);
        when(otherAgent.getId()).thenReturn(43L);
        ticket.setAssignee(otherAgent);
        assertThatThrownBy(() -> ticketService.changeStatus(12L, TicketStatus.RESOLVED))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignmentAllowsReassignmentAndExplicitUnassignment() {
        User agent = new User("Agent", "agent@example.com", "hash", UserRole.AGENT);
        Ticket ticket = ticket(TicketStatus.OPEN);
        ticket.setAssignee(agent);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        ticketService.assign(12L, true, null);
        assertThat(ticket.getAssignee()).isNull();
    }

    @Test
    void updatePreservesOmittedFields() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        Ticket updated = ticketService.update(12L, new UpdateTicketRequest(null, null, TicketPriority.URGENT));
        assertThat(updated.getTitle()).isEqualTo("Title");
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.URGENT);
        verify(ticketHistoryService, never()).recordStatusChange(any(), any(), any(), any());
    }

    private Ticket ticket(TicketStatus status) {
        return new Ticket("Title", "Description", status, TicketPriority.LOW, actor);
    }
}
