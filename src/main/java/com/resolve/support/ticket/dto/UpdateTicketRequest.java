package com.resolve.support.ticket.dto;

import com.resolve.support.ticket.entity.TicketPriority;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partial ticket detail update. Omitted properties are preserved; status and assignment use dedicated endpoints.", example = "{\"title\":\"Updated sign-in issue\",\"priority\":\"URGENT\"}")
public record UpdateTicketRequest(
        @Size(max = 200) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String title,
        @Size(max = 10000) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String description,
        TicketPriority priority) {

    public boolean hasChanges() {
        return title != null || description != null || priority != null;
    }
}
