package com.polyglotmesh.dto;

import java.time.Instant;

import com.polyglotmesh.engine.ExecutionResult;

/**
 * Response of POST /api/execution/run (always HTTP 200 when the request itself was valid,
 * even if the script failed: look at "success" and "status").
 */
public record ExecutionResponse(
        boolean success,
        String status,
        String language,
        String output,
        String stderr,
        String result,
        String error,
        String errorType,
        double executionTimeMs,
        double setupTimeMs,
        double cpuTimeMs,
        long allocatedKb,
        boolean outputTruncated,
        Instant executedAt) {

    public static ExecutionResponse from(ExecutionResult result, String language, Instant executedAt) {
        return new ExecutionResponse(
                result.success(),
                result.status().name(),
                language,
                result.output(),
                result.stderr(),
                result.result(),
                result.error(),
                result.errorType(),
                result.executionTimeMs(),
                result.setupTimeMs(),
                result.cpuTimeMs(),
                result.allocatedKb(),
                result.outputTruncated(),
                executedAt);
    }
}
