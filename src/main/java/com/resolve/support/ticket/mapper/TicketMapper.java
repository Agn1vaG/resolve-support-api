package com.resolve.support.ticket.mapper;

import com.resolve.support.account.mapper.UserMapper;
import com.resolve.support.ticket.dto.TicketResponse;
import com.resolve.support.ticket.dto.TicketSummaryResponse;
import com.resolve.support.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    private final UserMapper userMapper;

    public TicketMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getStatus(),
                ticket.getPriority(), userMapper.toSummary(ticket.getCreator()),
                userMapper.toSummary(ticket.getAssignee()), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return new TicketSummaryResponse(ticket.getId(), ticket.getTitle(), ticket.getStatus(), ticket.getPriority(),
                userMapper.toSummary(ticket.getCreator()), userMapper.toSummary(ticket.getAssignee()),
                ticket.getCreatedAt(), ticket.getUpdatedAt());
    }
}
