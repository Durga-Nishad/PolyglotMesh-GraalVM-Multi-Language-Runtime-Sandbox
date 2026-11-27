package com.polyglotmesh.engine;

import org.springframework.stereotype.Service;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.sandbox.ExecutionWatchdog;
import com.polyglotmesh.sandbox.SandboxContextFactory;

/** Runs JavaScript (GraalJS) inside the sandboxed Context. */
@Service
public class JavaScriptExecutionService extends AbstractGraalExecutor {

    public JavaScriptExecutionService(SandboxContextFactory factory, ExecutionWatchdog watchdog,
                                      PolyglotMeshProperties properties) {
        super(factory, watchdog, properties);
    }

    @Override
    public String apiLanguage() {
        return "javascript";
    }

    @Override
    public String graalLanguageId() {
        return "js";
    }
}
