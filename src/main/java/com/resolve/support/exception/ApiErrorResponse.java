package com.resolve.support.exception;

import java.time.Instant;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Consistent API error response. Internal stack traces and database details are not included.", example = "{\"timestamp\":\"2026-11-09T10:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Request validation failed.\",\"path\":\"/api/v1/tickets\",\"validationErrors\":{\"title\":\"must not be blank\"}}")
public record ApiErrorResponse(Instant timestamp, int status, String error, String message,
                              String path, Map<String, String> validationErrors) {
}
