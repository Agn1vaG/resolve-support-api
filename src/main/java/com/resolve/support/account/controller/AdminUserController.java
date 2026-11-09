package com.resolve.support.account.controller;

import com.resolve.support.account.dto.UpdateUserRoleRequest;
import com.resolve.support.account.dto.UserResponse;
import com.resolve.support.account.mapper.UserMapper;
import com.resolve.support.account.service.UserService;
import com.resolve.support.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public AdminUserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @Operation(summary = "Admin: list users", description = "Paginated safe user profiles. Page size is capped at 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of user profiles"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<UserResponse> getAll(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return userService.getAll(pageable).map(userMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Admin: get a user", description = "Returns safe user information without password hashes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Safe user profile", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public UserResponse getById(@PathVariable Long id) {
        return userMapper.toResponse(userService.getById(id));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Admin: change a user's role", description = "Change a user between USER and AGENT. This endpoint cannot assign ADMIN; the sole administrator cannot demote their own account.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = UpdateUserRoleRequest.class), examples = @ExampleObject(value = "{\"role\":\"AGENT\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role or unsafe self-demotion", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return userMapper.toResponse(userService.updateRole(id, request));
    }
}
