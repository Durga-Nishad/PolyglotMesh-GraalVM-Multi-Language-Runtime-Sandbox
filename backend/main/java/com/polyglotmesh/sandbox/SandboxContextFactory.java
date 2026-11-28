package com.polyglotmesh.sandbox;

import java.io.OutputStream;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;

import com.polyglotmesh.config.PolyglotMeshProperties;

import jakarta.annotation.PreDestroy;

/**
 * Builds the restricted GraalVM {@link Context} every script runs in.
 *
 * <p>One shared {@link Engine} is used so compiled code and language runtimes are reused between runs
 * (this makes repeated executions faster); every execution still gets its OWN Context, so scripts cannot see
 * each other's variables.
 *
 * <p>What the Context forbids: host file system, sockets, process creation, native access, thread creation,
 * environment variables, Java class lookup and cross-language evaluation. A statement limit stops runaway loops.
 * What it can NOT do on GraalVM Community: cap heap memory per Context. Use JVM -Xmx and container limits.
 */
@Component
public class SandboxContextFactory {

    /** Scripts may read/write Map, List and iterable host objects and call methods marked @HostAccess.Export. */
    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder()
            .allowAccessAnnotatedBy(HostAccess.Export.class)
            .allowMapAccess(true)
            .allowListAccess(true)
            .allowIterableAccess(true)
            .allowIteratorAccess(true)
            .build();

    private final Engine engine;
    private final long statementLimit;

    public SandboxContextFactory(PolyglotMeshProperties properties) {
        this.statementLimit = properties.sandbox().statementLimit();
        // WarnInterpreterOnly=false hides the "running without Graal compiler" warning on a normal JDK.
        this.engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    /** @param graalLanguageId "python" or "js" */
    public Context create(String graalLanguageId, OutputStream out, OutputStream err) {
        ResourceLimits limits = ResourceLimits.newBuilder()
                .statementLimit(statementLimit, null)
                .build();

        return Context.newBuilder(graalLanguageId)
                .engine(engine)
                .out(out)
                .err(err)
                .allowHostAccess(HOST_ACCESS)
                .allowHostClassLookup(className -> false)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowNativeAccess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowExperimentalOptions(false)
                .resourceLimits(limits)
                .build();
    }

    public String implementationName() {
        return engine.getImplementationName();
    }

    public String engineVersion() {
        return engine.getVersion();
    }

    public Set<String> installedLanguages() {
        return engine.getLanguages().keySet();
    }

    @PreDestroy
    public void shutdown() {
        engine.close();
    }
}
