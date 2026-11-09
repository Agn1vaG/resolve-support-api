package com.resolve.support.security.dto;

import com.resolve.support.account.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Signed access token and safe account profile returned after login.", example = "{\"accessToken\":\"eyJ...\",\"tokenType\":\"Bearer\",\"expiresIn\":900,\"user\":{\"id\":1,\"name\":\"Ari Example\",\"email\":\"ari@example.com\",\"role\":\"USER\"}}")
public record AuthResponse(
        @Schema(description = "Signed JWT access token. Never log or share this value.", accessMode = Schema.AccessMode.READ_ONLY)
        String accessToken,
        String tokenType, long expiresIn, UserResponse user) {
    @Override
    public String toString() {
        return "AuthResponse[accessToken=[REDACTED], tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", user=" + user + "]";
    }
}
