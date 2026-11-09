package com.resolve.support.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.resolve.support.account.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Administrator-created account. Public registration uses RegisterRequest and always assigns USER.", example = "{\"name\":\"Sam Agent\",\"email\":\"sam.agent@example.com\",\"password\":\"SecurePass123\",\"role\":\"AGENT\"}")
public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Initial account password; stored as a one-way BCrypt hash.", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 72) String password,
        UserRole role) {

    @Override
    public String toString() {
        return "CreateUserRequest[name=" + name + ", email=" + email + ", password=[REDACTED], role=" + role + "]";
    }
}
