package com.resolve.support.history.controller;

import com.resolve.support.history.dto.TicketHistoryResponse;
import com.resolve.support.history.mapper.TicketHistoryMapper;
import com.resolve.support.history.service.TicketHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/history")
public class TicketHistoryController {

    private final TicketHistoryService historyService;
    private final TicketHistoryMapper historyMapper;

    public TicketHistoryController(TicketHistoryService historyService, TicketHistoryMapper historyMapper) {
        this.historyService = historyService;
        this.historyMapper = historyMapper;
    }

    @GetMapping
    public List<TicketHistoryResponse> getForTicket(@PathVariable Long ticketId) {
        return historyService.getByTicket(ticketId).stream().map(historyMapper::toResponse).toList();
    }
}
