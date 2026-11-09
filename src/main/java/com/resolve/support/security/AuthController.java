package com.resolve.support.security;

import com.resolve.support.account.dto.UserResponse;
import com.resolve.support.security.dto.AuthResponse;
import com.resolve.support.security.dto.LoginRequest;
import com.resolve.support.security.dto.RegisterRequest;
import com.resolve.support.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @Operation(summary = "Register a USER", description = "Public registration creates the USER role automatically. The request never selects a privileged role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account registered", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email is already registered", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT", description = "Authenticates credentials and returns a signed bearer access token. Invalid credentials receive the same generic response.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication succeeded", content = @Content(schema = @Schema(implementation = AuthResponse.class), examples = @ExampleObject(value = "{\"accessToken\":\"<jwt>\",\"tokenType\":\"Bearer\",\"expiresIn\":900,\"user\":{\"id\":1,\"name\":\"Ari Example\",\"email\":\"ari@example.com\",\"role\":\"USER\"}}"))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
