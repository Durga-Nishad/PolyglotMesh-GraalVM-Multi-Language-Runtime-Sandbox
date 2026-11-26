package com.polyglotmesh.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.polyglotmesh.dto.ExecutionRequest;
import com.polyglotmesh.dto.ExecutionResponse;
import com.polyglotmesh.engine.ExecutionCommand;
import com.polyglotmesh.engine.ExecutionResult;
import com.polyglotmesh.engine.PolyglotEngineService;
import com.polyglotmesh.exception.SandboxViolationException;
import com.polyglotmesh.exception.ValidationException;
import com.polyglotmesh.sandbox.ConcurrencyLimiter;
import com.polyglotmesh.sandbox.SandboxValidator;
import com.polyglotmesh.sandbox.SupportedLanguages;

/**
 * Week 1-2 flow: validate -> claim an execution slot -> run in GraalVM -> build the response.
 * (Saving history in MySQL comes in Week 4.)
 *
 * <p>This service is blocking on purpose; controllers call it through {@code Reactive.blocking(...)}.
 */
@Service
public class ExecutionService {

    private final SupportedLanguages languages;
    private final SandboxValidator validator;
    private final PolyglotEngineService engine;
    private final ConcurrencyLimiter limiter;

    public ExecutionService(SupportedLanguages languages, SandboxValidator validator,
                            PolyglotEngineService engine, ConcurrencyLimiter limiter) {
        this.languages = languages;
        this.validator = validator;
        this.engine = engine;
        this.limiter = limiter;
    }

    /** POST /api/execution/run: runs the code and returns the result. */
    public ExecutionResponse run(ExecutionRequest request) {
        if (request == null) {
            throw new ValidationException("body", "Request body is required");
        }
        String language = languages.normalizeExecutable(request.language());
        Instant startedAt = Instant.now();
        ExecutionResult result = executeChecked(language, request.code(), Map.of());
        return ExecutionResponse.from(result, language, startedAt);
    }

    /**
     * Validate + claim a slot + run.
     *
     * @param language canonical language name (already normalised)
     * @return a normal result; sandbox pre-filter violations come back as status BLOCKED
     */
    public ExecutionResult executeChecked(String language, String code, Map<String, Object> bindings) {
        try {
            validator.validate(language, code);
        } catch (SandboxViolationException violation) {
            return ExecutionResult.blocked(violation.getMessage());
        }
        try (ConcurrencyLimiter.Permit ignored = limiter.acquire()) {
            return engine.execute(new ExecutionCommand(language, code, bindings, null));
        }
    }
}
