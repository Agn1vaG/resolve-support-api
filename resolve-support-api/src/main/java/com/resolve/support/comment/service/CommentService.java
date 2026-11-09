package com.resolve.support.comment.service;

import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import com.resolve.support.comment.dto.CreateCommentRequest;
import com.resolve.support.comment.entity.Comment;
import com.resolve.support.comment.repository.CommentRepository;
import com.resolve.support.exception.ResourceNotFoundException;
import com.resolve.support.security.CurrentUserService;
import com.resolve.support.ticket.entity.Ticket;
import com.resolve.support.ticket.repository.TicketRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository,
                          CurrentUserService currentUserService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Comment create(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        User author = currentUserService.getCurrentUser();
        requireCanAccess(ticket, author);
        return commentRepository.save(new Comment(request.message(), ticket, author));
    }

    public List<Comment> getByTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        requireCanAccess(ticket, currentUserService.getCurrentUser());
        return commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId);
    }

    private void requireCanAccess(Ticket ticket, User actor) {
        boolean allowed = switch (actor.getRole()) {
            case ADMIN -> true;
            case USER -> ticket.getCreator().getId().equals(actor.getId());
            case AGENT -> ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actor.getId());
        };
        if (!allowed) throw new AccessDeniedException("You do not have access to this ticket's comments.");
    }
}
