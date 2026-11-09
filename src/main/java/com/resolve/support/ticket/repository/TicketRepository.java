package com.resolve.support.ticket.repository;

import com.resolve.support.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Override
    @EntityGraph(attributePaths = {"creator", "assignee"})
    Optional<Ticket> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<Ticket> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<Ticket> findAll(Specification<Ticket> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<Ticket> findByCreator_Id(Long creatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<Ticket> findByAssignee_Id(Long assigneeId, Pageable pageable);

}
