package com.resolve.support.comment.controller;

import com.resolve.support.comment.dto.CommentResponse;
import com.resolve.support.comment.dto.CreateCommentRequest;
import com.resolve.support.comment.mapper.CommentMapper;
import com.resolve.support.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable Long ticketId,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentMapper.toResponse(commentService.create(ticketId, request));
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + ticketId + "/comments/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<CommentResponse> getForTicket(@PathVariable Long ticketId) {
        return commentService.getByTicket(ticketId).stream().map(commentMapper::toResponse).toList();
    }
}
