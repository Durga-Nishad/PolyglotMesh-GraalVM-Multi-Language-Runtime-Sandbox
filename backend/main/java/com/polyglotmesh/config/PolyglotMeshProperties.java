package com.polyglotmesh.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed view of the "polyglotmesh.*" section of application.yml.
 * Every value can be changed from backend/.env without touching code.
 */
@ConfigurationProperties(prefix = "polyglotmesh")
public record PolyglotMeshProperties(
        @DefaultValue Sandbox sandbox,
        @DefaultValue Cors cors,
        @DefaultValue Compare compare) {

    public record Sandbox(
            @DefaultValue("5000") long timeoutMs,
            @DefaultValue("20000") int maxCodeLength,
            @DefaultValue("65536") int maxOutputBytes,
            @DefaultValue("20000000") long statementLimit,
            @DefaultValue("4") int maxConcurrentExecutions,
            @DefaultValue("python,javascript") List<String> allowedLanguages) {
    }

    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }

    public record Compare(
            @DefaultValue("5") int defaultIterations,
            @DefaultValue("25") int maxIterations) {
    }
}
