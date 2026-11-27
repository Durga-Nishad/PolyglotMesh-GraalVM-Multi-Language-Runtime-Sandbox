package com.polyglotmesh.engine;

/**
 * What the engine returns after running a script.
 *
 * @param executionTimeMs time spent evaluating the script (milliseconds, 3 decimals)
 * @param setupTimeMs     time spent creating the GraalVM Context and binding variables
 * @param cpuTimeMs       CPU time of the calling thread during evaluation (-1 if the JVM does not support it)
 * @param allocatedKb     memory allocated by the calling thread during evaluation (-1 if not supported)
 */
public record ExecutionResult(
        ExecutionStatus status,
        String output,
        String stderr,
        String result,
        String error,
        String errorType,
        double executionTimeMs,
        double setupTimeMs,
        double cpuTimeMs,
        long allocatedKb,
        boolean outputTruncated) {

    public boolean success() {
        return status == ExecutionStatus.SUCCESS;
    }

    public static ExecutionResult blocked(String message) {
        return failure(ExecutionStatus.BLOCKED, "SANDBOX_BLOCKED", message);
    }

    public static ExecutionResult failure(ExecutionStatus status, String errorType, String message) {
        return new ExecutionResult(status, "", "", null, message, errorType, 0, 0, -1, -1, false);
    }
}
