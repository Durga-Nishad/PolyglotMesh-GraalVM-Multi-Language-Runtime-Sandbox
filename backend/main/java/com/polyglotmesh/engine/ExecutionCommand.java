package com.polyglotmesh.engine;

import java.util.Map;

/**
 * Everything the engine needs to run one script.
 *
 * @param language           canonical language name ("python" or "javascript")
 * @param code               source code (already validated)
 * @param bindings           Java objects exposed to the script as global variables (for example "data", "mongo")
 * @param timeoutMsOverride  optional shorter timeout (used by the sandbox self-test); null = configured default
 */
public record ExecutionCommand(String language, String code, Map<String, Object> bindings, Long timeoutMsOverride) {

    public ExecutionCommand {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language must not be empty");
        }
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        bindings = bindings == null ? Map.of() : bindings;
    }

    public static ExecutionCommand of(String language, String code) {
        return new ExecutionCommand(language, code, Map.of(), null);
    }
}
