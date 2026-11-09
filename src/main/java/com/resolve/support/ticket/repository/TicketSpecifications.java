package com.resolve.support.ticket.repository;

import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.entity.TicketPriority;
import com.resolve.support.ticket.entity.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class TicketSpecifications {
    private TicketSpecifications() { }

    public static Specification<Ticket> statusIs(TicketStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> priorityIs(TicketPriority priority) {
        return priority == null ? null : (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> assigneeIs(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("assignee").get("id"), id);
    }

    public static Specification<Ticket> creatorIs(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("creator").get("id"), id);
    }

    public static Specification<Ticket> createdAtFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from);
    }

    public static Specification<Ticket> createdAtTo(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), to);
    }
}
