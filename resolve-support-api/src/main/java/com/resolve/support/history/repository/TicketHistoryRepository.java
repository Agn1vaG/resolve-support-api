package com.resolve.support.history.repository;

import com.resolve.support.history.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    @EntityGraph(attributePaths = {"ticket", "actor", "oldAssignee", "newAssignee"})
    List<TicketHistory> findByTicket_IdOrderByChangedAtAscIdAsc(Long ticketId);
}
