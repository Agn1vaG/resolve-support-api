package com.resolve.support.history.mapper;

import com.resolve.support.account.mapper.UserMapper;
import com.resolve.support.history.dto.TicketHistoryResponse;
import com.resolve.support.history.entity.TicketHistory;
import org.springframework.stereotype.Component;

@Component
public class TicketHistoryMapper {

    private final UserMapper userMapper;

    public TicketHistoryMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TicketHistoryResponse toResponse(TicketHistory history) {
        return new TicketHistoryResponse(history.getId(), history.getTicket().getId(),
                history.getEventType(), userMapper.toSummary(history.getActor()), history.getOldStatus(),
                history.getNewStatus(), userMapper.toSummary(history.getOldAssignee()),
                userMapper.toSummary(history.getNewAssignee()), history.getChangedAt());
    }
}
