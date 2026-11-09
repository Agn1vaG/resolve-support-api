package com.resolve.support.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials used to obtain a signed JWT access token.")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "ari@example.com")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Account password", example = "SecurePass123", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(max = 72) String password) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=[REDACTED]]";
    }
}
