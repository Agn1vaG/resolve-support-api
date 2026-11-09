package com.resolve.support.history.entity;

import com.resolve.support.account.entity.User;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ticket_history")
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_ticket_history_ticket"))
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_ticket_history_actor"))
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private TicketHistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private TicketStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private TicketStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_assignee_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_ticket_history_old_assignee"))
    private User oldAssignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_assignee_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_ticket_history_new_assignee"))
    private User newAssignee;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected TicketHistory() {
    }

    public TicketHistory(Ticket ticket, User actor, TicketHistoryEventType eventType,
                         TicketStatus oldStatus, TicketStatus newStatus, User oldAssignee, User newAssignee) {
        this.ticket = ticket;
        this.actor = actor;
        this.eventType = eventType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.oldAssignee = oldAssignee;
        this.newAssignee = newAssignee;
    }

    public TicketHistory(Ticket ticket, User actor, TicketStatus oldStatus, TicketStatus newStatus) {
        this(ticket, actor, TicketHistoryEventType.STATUS_CHANGED, oldStatus, newStatus, null, null);
    }

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public User getActor() { return actor; }
    public TicketHistoryEventType getEventType() { return eventType; }
    public TicketStatus getOldStatus() { return oldStatus; }
    public TicketStatus getNewStatus() { return newStatus; }
    public User getOldAssignee() { return oldAssignee; }
    public User getNewAssignee() { return newAssignee; }
    public Instant getChangedAt() { return changedAt; }
}
