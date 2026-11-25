package com.polyglotmesh.exception;

/**
 * Thrown by the sandbox pre-filter when submitted code uses something that is not allowed.
 * It is NOT an HTTP error: the execution API reports it as a normal response with status BLOCKED.
 */
public class SandboxViolationException extends RuntimeException {

    private final String ruleId;

    public SandboxViolationException(String ruleId, String message) {
        super(message);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
