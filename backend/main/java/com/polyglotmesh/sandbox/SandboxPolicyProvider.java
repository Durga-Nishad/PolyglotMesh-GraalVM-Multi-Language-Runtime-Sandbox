package com.polyglotmesh.sandbox;

import java.util.List;

import org.springframework.stereotype.Component;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.dto.SandboxPolicyResponse;

/** Describes the active sandbox rules (and their honest limits) for the Security page. */
@Component
public class SandboxPolicyProvider {

    private final PolyglotMeshProperties properties;
    private final SandboxValidator validator;
    private final SupportedLanguages languages;

    public SandboxPolicyProvider(PolyglotMeshProperties properties, SandboxValidator validator,
                                 SupportedLanguages languages) {
        this.properties = properties;
        this.validator = validator;
        this.languages = languages;
    }

    public SandboxPolicyResponse describe() {
        PolyglotMeshProperties.Sandbox s = properties.sandbox();
        return new SandboxPolicyResponse(
                s.timeoutMs(),
                s.statementLimit(),
                s.maxCodeLength(),
                s.maxOutputBytes(),
                s.maxConcurrentExecutions(),
                List.copyOf(languages.executableLanguages()),
                List.of(
                        "Host file system: denied (IOAccess.NONE)",
                        "Network sockets: denied",
                        "Process creation: denied",
                        "Native code access: denied",
                        "Thread creation by scripts: denied",
                        "Environment variables: denied",
                        "Java class lookup: denied (only Map/List access and exported mock services)",
                        "Cross-language evaluation: denied"),
                validator.describeRules(),
                List.of(
                        "Graal Community cannot cap heap memory per Context. Use JVM -Xmx and container memory limits.",
                        "Context restrictions are not an operating-system security boundary. For untrusted code in production, "
                                + "run the engine in a separate locked-down container or process (no network, read-only file system, "
                                + "cgroup CPU/memory limits, seccomp).",
                        "The pattern pre-filter is a convenience and can be bypassed; the Context settings are the real control.",
                        "CPU use is bounded by the timeout and statement limit, not by OS-level CPU quotas."));
    }
}
