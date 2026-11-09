package com.resolve.support.comment.mapper;

import com.resolve.support.account.mapper.UserMapper;
import com.resolve.support.comment.dto.CommentResponse;
import com.resolve.support.comment.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getMessage(), comment.getTicket().getId(),
                userMapper.toSummary(comment.getAuthor()), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
