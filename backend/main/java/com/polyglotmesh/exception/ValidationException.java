package com.polyglotmesh.exception;

import java.util.Map;

/** Thrown when a request value is null, empty, too long or otherwise invalid. Mapped to HTTP 400. */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
