package com.polyglotmesh.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/** Turns every exception into a clean JSON {@link ApiError}. Stack traces are logged, never returned. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange, ex.getFieldErrors());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleBind(WebExchangeBindException ex, ServerWebExchange exchange) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed. Please check the highlighted fields.", exchange, fields);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleBadInput(ServerWebInputException ex, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST,
                "The request body or a request parameter is missing or malformed.", exchange, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(TooManyExecutionsException.class)
    public ResponseEntity<ApiError> handleBusy(TooManyExecutionsException ex, ServerWebExchange exchange) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getReason() != null ? ex.getReason() : "Request failed", exchange, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error on {}", exchange.getRequest().getPath().value(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on the server. Please try again.", exchange, Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           ServerWebExchange exchange, Map<String, String> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, exchange.getRequest().getPath().value(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
