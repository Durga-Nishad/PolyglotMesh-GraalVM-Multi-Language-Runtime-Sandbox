package com.polyglotmesh.engine;

/**
 * Contract between the engine and the rest of the app. To add a language (for example Ruby),
 * write one more implementation and expose it as a Spring bean; nothing else needs to change.
 */
public interface LanguageExecutor {

    /** Canonical API name, for example "python" or "javascript". */
    String apiLanguage();

    /** GraalVM language id, for example "python" or "js". */
    String graalLanguageId();

    ExecutionResult execute(ExecutionCommand command);
}
