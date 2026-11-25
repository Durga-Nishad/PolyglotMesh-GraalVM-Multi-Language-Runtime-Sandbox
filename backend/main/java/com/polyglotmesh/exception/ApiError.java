package com.polyglotmesh.exception;

import java.time.Instant;
import java.util.Map;

/** JSON body returned for every HTTP error, so the frontend can show one consistent message. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
