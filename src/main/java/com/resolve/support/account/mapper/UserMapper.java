package com.resolve.support.account.mapper;

import com.resolve.support.account.dto.UserResponse;
import com.resolve.support.account.dto.UserSummaryResponse;
import com.resolve.support.account.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    public UserSummaryResponse toSummary(User user) {
        return user == null ? null : new UserSummaryResponse(user.getId(), user.getName(), user.getRole());
    }
}
