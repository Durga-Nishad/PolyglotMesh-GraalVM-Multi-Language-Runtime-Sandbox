package com.polyglotmesh.engine;

import org.springframework.stereotype.Service;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.sandbox.ExecutionWatchdog;
import com.polyglotmesh.sandbox.SandboxContextFactory;

/** Runs Python (GraalPy) inside the sandboxed Context. */
@Service
public class PythonExecutionService extends AbstractGraalExecutor {

    public PythonExecutionService(SandboxContextFactory factory, ExecutionWatchdog watchdog,
                                  PolyglotMeshProperties properties) {
        super(factory, watchdog, properties);
    }

    @Override
    public String apiLanguage() {
        return "python";
    }

    @Override
    public String graalLanguageId() {
        return "python";
    }
}
