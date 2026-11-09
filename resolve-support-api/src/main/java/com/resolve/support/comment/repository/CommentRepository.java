package com.resolve.support.comment.repository;

import com.resolve.support.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"ticket", "author"})
    List<Comment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
