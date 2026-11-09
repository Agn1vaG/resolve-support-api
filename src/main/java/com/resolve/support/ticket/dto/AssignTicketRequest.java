package com.resolve.support.ticket.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;

/** agentId is required as a property; an explicit JSON null clears the current assignment. */
@Schema(description = "Assignment request. Supply an AGENT id, or explicitly supply null to unassign.", example = "{\"agentId\":42}")
public class AssignTicketRequest {
    @Schema(description = "AGENT account id; explicit null clears the assignment.", nullable = true, example = "42")
    private Long agentId;
    private boolean agentIdProvided;

    public Long getAgentId() { return agentId; }
    @Schema(hidden = true)
    public boolean isAgentIdProvided() { return agentIdProvided; }

    @JsonSetter("agentId")
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
        this.agentIdProvided = true;
    }
}
