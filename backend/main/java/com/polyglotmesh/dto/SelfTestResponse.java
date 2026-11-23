package com.polyglotmesh.dto;

import java.util.List;

/** Result of POST /api/sandbox/self-test: built-in "malicious code" checks. */
public record SelfTestResponse(int passed, int total, List<Case> cases) {

    /**
     * @param layer        "graal-context" (real GraalVM restrictions) or "pre-filter" (pattern check)
     * @param expectation  what a safe sandbox should do
     * @param actualStatus what actually happened
     * @param passed       true when the sandbox behaved as expected
     */
    public record Case(
            String name,
            String language,
            String layer,
            String expectation,
            String actualStatus,
            boolean passed,
            String detail,
            double durationMs) {
    }
}
