package com.polyglotmesh.engine;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.sandbox.ExecutionWatchdog;
import com.polyglotmesh.sandbox.LimitedOutputStream;
import com.polyglotmesh.sandbox.SandboxContextFactory;

/**
 * Shared logic for running one script in a fresh sandboxed Context:
 * create context -> bind Java objects -> arm timeout -> evaluate -> collect output, errors and metrics.
 */
public abstract class AbstractGraalExecutor implements LanguageExecutor {

    private static final Logger log = LoggerFactory.getLogger(AbstractGraalExecutor.class);
    private static final int MAX_RESULT_CHARS = 2000;
    private static final int MAX_GUEST_FRAMES = 12;

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean SUN_THREADS =
            THREADS instanceof com.sun.management.ThreadMXBean sun ? sun : null;

    private final SandboxContextFactory contextFactory;
    private final ExecutionWatchdog watchdog;
    private final PolyglotMeshProperties properties;

    protected AbstractGraalExecutor(SandboxContextFactory contextFactory, ExecutionWatchdog watchdog,
                                    PolyglotMeshProperties properties) {
        this.contextFactory = contextFactory;
        this.watchdog = watchdog;
        this.properties = properties;
    }

    @Override
    public ExecutionResult execute(ExecutionCommand command) {
        int maxOutput = properties.sandbox().maxOutputBytes();
        LimitedOutputStream out = new LimitedOutputStream(maxOutput);
        LimitedOutputStream err = new LimitedOutputStream(maxOutput);
        long timeoutMs = command.timeoutMsOverride() != null && command.timeoutMsOverride() > 0
                ? command.timeoutMsOverride()
                : properties.sandbox().timeoutMs();

        Context context = null;
        ExecutionWatchdog.Handle guard = null;
        long setupStart = System.nanoTime();
        long evalStart = setupStart;
        long cpuStart = -1;
        long allocStart = -1;
        String resultText = null;
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String errorText = null;
        String errorType = null;
        double setupMs;
        double evalMs;
        double cpuMs = -1;
        long allocKb = -1;

        try {
            context = contextFactory.create(graalLanguageId(), out, err);
            // Start the language runtime BEFORE the timeout clock starts. On a normal JDK the first start of
            // GraalPy / GraalJS can take several seconds, and that is not the user's script running.
            context.initialize(graalLanguageId());
            guard = watchdog.arm(context, timeoutMs);

            Value bindings = context.getBindings(graalLanguageId());
            for (Map.Entry<String, Object> entry : command.bindings().entrySet()) {
                bindings.putMember(entry.getKey(), entry.getValue());
            }

            evalStart = System.nanoTime();
            if (THREADS.isCurrentThreadCpuTimeSupported()) {
                cpuStart = THREADS.getCurrentThreadCpuTime();
            }
            if (SUN_THREADS != null) {
                allocStart = SUN_THREADS.getCurrentThreadAllocatedBytes();
            }

            Value value = context.eval(Source.create(graalLanguageId(), command.code()));
            resultText = describe(value);
        } catch (PolyglotException ex) {
            boolean timedOut = guard != null && guard.timedOut();
            if (timedOut || ex.isCancelled()) {
                status = ExecutionStatus.TIMEOUT;
                errorType = "TIMEOUT";
                errorText = "Execution stopped after " + timeoutMs + " ms (timeout). "
                        + "Check for infinite loops or very long computations.";
            } else if (ex.isResourceExhausted()) {
                status = ExecutionStatus.RESOURCE_LIMIT;
                errorType = "RESOURCE_LIMIT";
                errorText = "Execution stopped: the statement limit was reached. " + safeMessage(ex);
            } else {
                status = ExecutionStatus.ERROR;
                errorType = classify(ex);
                errorText = formatError(ex);
            }
        } catch (IllegalArgumentException ex) {
            // Thrown by Context.newBuilder(...) when the language runtime is not installed.
            status = ExecutionStatus.ERROR;
            errorType = "ENGINE_ERROR";
            errorText = "The '" + apiLanguage() + "' runtime is not available on this JVM: " + safeMessage(ex);
        } catch (RuntimeException ex) {
            log.error("Unexpected engine failure for language {}", apiLanguage(), ex);
            status = ExecutionStatus.ERROR;
            errorType = "INTERNAL_ERROR";
            errorText = "Internal engine error: " + ex.getClass().getSimpleName();
        } finally {
            long end = System.nanoTime();
            if (cpuStart >= 0 && THREADS.isCurrentThreadCpuTimeSupported()) {
                cpuMs = (THREADS.getCurrentThreadCpuTime() - cpuStart) / 1_000_000.0;
            }
            if (allocStart >= 0 && SUN_THREADS != null) {
                allocKb = Math.max(0, (SUN_THREADS.getCurrentThreadAllocatedBytes() - allocStart) / 1024);
            }
            if (guard != null) {
                guard.disarm();
            }
            if (context != null) {
                try {
                    context.close(true);
                } catch (RuntimeException ignored) {
                    // already closed by the watchdog
                }
            }
            setupMs = (evalStart - setupStart) / 1_000_000.0;
            evalMs = (end - evalStart) / 1_000_000.0;
        }

        return new ExecutionResult(
                status,
                out.content(),
                err.content(),
                resultText,
                errorText,
                errorType,
                round(evalMs),
                round(setupMs),
                cpuMs < 0 ? -1 : round(cpuMs),
                allocKb,
                out.isTruncated() || err.isTruncated());
    }

    private static String describe(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.toString();
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.length() > MAX_RESULT_CHARS ? text.substring(0, MAX_RESULT_CHARS) + "…" : text;
    }

    private static String classify(PolyglotException ex) {
        if (ex.isSyntaxError()) {
            return "SYNTAX_ERROR";
        }
        if (ex.isHostException()) {
            return "HOST_ERROR";
        }
        if (ex.isInternalError()) {
            return "INTERNAL_ERROR";
        }
        return "RUNTIME_ERROR";
    }

    private static String formatError(PolyglotException ex) {
        StringBuilder text = new StringBuilder(safeMessage(ex));
        int frames = 0;
        for (PolyglotException.StackFrame frame : ex.getPolyglotStackTrace()) {
            if (frame.isGuestFrame() && frames++ < MAX_GUEST_FRAMES) {
                text.append("\n    at ").append(frame);
            }
        }
        return text.toString();
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
