package com.polyglotmesh.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * The one entry point the rest of the application uses to run code.
 * It picks the right {@link LanguageExecutor} for the language and delegates.
 */
@Service
public class PolyglotEngineService {

    private final Map<String, LanguageExecutor> executors = new HashMap<>();

    public PolyglotEngineService(List<LanguageExecutor> executorBeans) {
        for (LanguageExecutor executor : executorBeans) {
            executors.put(executor.apiLanguage(), executor);
        }
    }

    public ExecutionResult execute(ExecutionCommand command) {
        LanguageExecutor executor = executors.get(command.language());
        if (executor == null) {
            return ExecutionResult.failure(ExecutionStatus.ERROR, "ENGINE_ERROR",
                    "No executor is registered for language '" + command.language() + "'");
        }
        return executor.execute(command);
    }

    public boolean supports(String language) {
        return executors.containsKey(language);
    }
}
