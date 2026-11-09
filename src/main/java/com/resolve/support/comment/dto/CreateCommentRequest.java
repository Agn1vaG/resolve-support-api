package com.resolve.support.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comment text. The author is the authenticated user; message must be nonblank and at most 5000 characters.", example = "{\"message\":\"I restarted the device and can sign in now.\"}")
public record CreateCommentRequest(@Schema(example = "I restarted the device and can sign in now.") @NotBlank @Size(max = 5000) String message) {
}
