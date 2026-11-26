package com.polyglotmesh.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.polyglotmesh.dto.SelfTestResponse;
import com.polyglotmesh.dto.SelfTestResponse.Case;
import com.polyglotmesh.engine.ExecutionCommand;
import com.polyglotmesh.engine.ExecutionResult;
import com.polyglotmesh.engine.ExecutionStatus;
import com.polyglotmesh.engine.PolyglotEngineService;
import com.polyglotmesh.exception.SandboxViolationException;
import com.polyglotmesh.exception.TooManyExecutionsException;
import com.polyglotmesh.sandbox.ConcurrencyLimiter;
import com.polyglotmesh.sandbox.SandboxValidator;

/**
 * Runs a built-in set of "malicious" scripts so the Security page can PROVE the sandbox works.
 *
 * <p>The graal-context cases skip the pattern pre-filter on purpose and call the engine directly, so they test
 * the real GraalVM restrictions. The pre-filter cases test the friendly early rejection separately.
 */
@Service
public class SandboxSelfTestService {

    private static final long SELF_TEST_TIMEOUT_MS = 1500;

    private record Spec(String name, String language, String expectation, String code,
                        Predicate<ExecutionResult> passes) {
    }

    private static final Predicate<ExecutionResult> STOPPED =
            r -> r.status() == ExecutionStatus.TIMEOUT || r.status() == ExecutionStatus.RESOURCE_LIMIT;
    private static final Predicate<ExecutionResult> DENIED = r -> r.status() == ExecutionStatus.ERROR;
    private static final Predicate<ExecutionResult> OUTPUT_CAPPED =
            r -> r.outputTruncated() || r.status() != ExecutionStatus.SUCCESS;

    private static final List<Spec> CONTEXT_CASES = List.of(
            new Spec("Infinite loop", "python", "Stopped by timeout or statement limit",
                    "while True:\n    pass", STOPPED),
            new Spec("Read a host file", "python", "Denied (error)",
                    "print(open('/etc/passwd').read()[:80])", DENIED),
            new Spec("Open a network socket", "python", "Denied (error)",
                    "import socket\ns = socket.socket()\ns.connect(('example.com', 80))", DENIED),
            new Spec("Start a process", "python", "Denied (error)",
                    "import subprocess\nprint(subprocess.run(['id'], capture_output=True))", DENIED),
            new Spec("Reach Java classes", "python", "Denied (error)",
                    "import java\nprint(java.type('java.lang.System').getProperty('user.home'))", DENIED),
            new Spec("Flood the output", "python", "Output capped or run stopped",
                    "for i in range(2000000):\n    print('x' * 200)", OUTPUT_CAPPED),
            new Spec("Infinite loop", "javascript", "Stopped by timeout or statement limit",
                    "while (true) {}", STOPPED),
            new Spec("Reach Java classes", "javascript", "Denied (error)",
                    "console.log(Java.type('java.lang.System').getProperty('user.home'))", DENIED),
            new Spec("Load a host file", "javascript", "Denied (error)",
                    "load('/etc/passwd')", DENIED),
            new Spec("Evaluate another language", "javascript", "Denied (error)",
                    "console.log(Polyglot.eval('python', '1 + 1'))", DENIED),
            new Spec("Flood the output", "javascript", "Output capped or run stopped",
                    "for (let i = 0; i < 2000000; i++) { console.log('x'.repeat(200)); }", OUTPUT_CAPPED));

    private record FilterSpec(String name, String language, String code) {
    }

    private static final List<FilterSpec> FILTER_CASES = List.of(
            new FilterSpec("Import os", "python", "import os\nprint(os.listdir('/'))"),
            new FilterSpec("Dynamic __import__", "python", "__import__('subprocess')"),
            new FilterSpec("require child_process", "javascript", "require('child_process').exec('id')"));

    private final PolyglotEngineService engine;
    private final SandboxValidator validator;
    private final ConcurrencyLimiter limiter;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SandboxSelfTestService(PolyglotEngineService engine, SandboxValidator validator,
                                  ConcurrencyLimiter limiter) {
        this.engine = engine;
        this.validator = validator;
        this.limiter = limiter;
    }

    public SelfTestResponse run() {
        if (!running.compareAndSet(false, true)) {
            throw new TooManyExecutionsException(1);
        }
        try {
            List<Case> cases = new ArrayList<>();
            for (FilterSpec spec : FILTER_CASES) {
                cases.add(runFilterCase(spec));
            }
            for (Spec spec : CONTEXT_CASES) {
                cases.add(runContextCase(spec));
            }
            int passed = (int) cases.stream().filter(Case::passed).count();
            return new SelfTestResponse(passed, cases.size(), cases);
        } finally {
            running.set(false);
        }
    }

    private Case runFilterCase(FilterSpec spec) {
        long start = System.nanoTime();
        try {
            validator.validate(spec.language(), spec.code());
            return new Case(spec.name(), spec.language(), "pre-filter", "Rejected before running",
                    "ALLOWED", false, "The pre-filter did not reject this code.", ms(start));
        } catch (SandboxViolationException violation) {
            return new Case(spec.name(), spec.language(), "pre-filter", "Rejected before running",
                    "BLOCKED", true, violation.getMessage(), ms(start));
        }
    }

    private Case runContextCase(Spec spec) {
        long start = System.nanoTime();
        ExecutionResult result;
        try (ConcurrencyLimiter.Permit ignored = limiter.acquire()) {
            result = engine.execute(new ExecutionCommand(spec.language(), spec.code(), Map.of(), SELF_TEST_TIMEOUT_MS));
        } catch (TooManyExecutionsException busy) {
            return new Case(spec.name(), spec.language(), "graal-context", spec.expectation(),
                    "SKIPPED", false, busy.getMessage(), ms(start));
        }
        String detail = result.error() != null ? firstLine(result.error())
                : result.outputTruncated() ? "Output was cut at the configured limit." : "Completed without error.";
        return new Case(spec.name(), spec.language(), "graal-context", spec.expectation(),
                result.status().name(), spec.passes().test(result), detail, ms(start));
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        String line = newline < 0 ? text : text.substring(0, newline);
        return line.length() > 220 ? line.substring(0, 220) + "…" : line;
    }

    private static double ms(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 1_000.0) / 1000.0;
    }
}
