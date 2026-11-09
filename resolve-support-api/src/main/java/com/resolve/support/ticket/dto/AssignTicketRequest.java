package com.resolve.support.ticket.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

/** agentId is required as a property; an explicit JSON null clears the current assignment. */
public class AssignTicketRequest {
    private Long agentId;
    private boolean agentIdProvided;

    public Long getAgentId() { return agentId; }
    public boolean isAgentIdProvided() { return agentIdProvided; }

    @JsonSetter("agentId")
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
        this.agentIdProvided = true;
    }
}
