package com.polyglotmesh.dto;

import java.util.List;

public record SandboxPolicyResponse(
        long timeoutMs,
        long statementLimit,
        int maxCodeLength,
        int maxOutputBytes,
        int maxConcurrentExecutions,
        List<String> executableLanguages,
        List<String> contextRestrictions,
        List<String> preFilterRules,
        List<String> limitations) {
}
