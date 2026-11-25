package com.polyglotmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/execution/run
 *
 * @param language "python" or "javascript" (aliases "py" and "js" are accepted)
 * @param code     the source code to run
 */
public record ExecutionRequest(
        @NotBlank(message = "language must not be empty")
        @Size(max = 20, message = "language is too long")
        String language,

        @NotBlank(message = "code must not be empty")
        @Size(max = 200000, message = "code is too long")
        String code) {
}
