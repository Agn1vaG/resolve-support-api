package com.resolve.support.comment.controller;

import com.resolve.support.comment.dto.CommentResponse;
import com.resolve.support.comment.dto.CreateCommentRequest;
import com.resolve.support.comment.mapper.CommentMapper;
import com.resolve.support.comment.service.CommentService;
import com.resolve.support.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Comments")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    @PostMapping
    @Operation(summary = "Add a comment", description = "The ticket creator, assigned agent, or admin may comment. The author is derived from the JWT. Messages must be nonblank and at most 5000 characters.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateCommentRequest.class), examples = @ExampleObject(value = "{\"message\":\"I restarted the device and can sign in now.\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created", content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No access to this ticket", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CommentResponse> create(@PathVariable Long ticketId,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentMapper.toResponse(commentService.create(ticketId, request));
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + ticketId + "/comments/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List ticket comments", description = "Returns chronological comments for the ticket creator, assigned agent, or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chronological comments"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No access to this ticket", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public List<CommentResponse> getForTicket(@PathVariable Long ticketId) {
        return commentService.getByTicket(ticketId).stream().map(commentMapper::toResponse).toList();
    }
}
