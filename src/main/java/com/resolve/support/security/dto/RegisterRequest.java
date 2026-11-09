package com.resolve.support.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Public registration data. The API always creates a USER role and ignores no authenticated identity because registration is public.")
public record RegisterRequest(
        @Schema(description = "Display name", example = "Ari Example")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "Unique email address", example = "ari@example.com")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Password, 8 to 72 characters", example = "SecurePass123", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 72) String password) {
    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", password=[REDACTED]]";
    }
}
