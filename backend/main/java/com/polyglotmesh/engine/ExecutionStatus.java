package com.polyglotmesh.engine;

/** Outcome of one script execution. Stored as text in execution_history.status. */
public enum ExecutionStatus {
    /** Script finished normally. */
    SUCCESS,
    /** Script raised an error (syntax error, runtime error, denied operation...). */
    ERROR,
    /** Script ran longer than the timeout and was cancelled. */
    TIMEOUT,
    /** Script hit the statement limit. */
    RESOURCE_LIMIT,
    /** Rejected by the sandbox pre-filter before it started. */
    BLOCKED
}
