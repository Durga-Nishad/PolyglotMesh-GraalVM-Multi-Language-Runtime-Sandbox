package com.polyglotmesh.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.sandbox.ExecutionWatchdog;
import com.polyglotmesh.sandbox.SandboxContextFactory;

/**
 * Engine tests. They need the GraalVM Python and JavaScript runtimes from the Maven dependencies
 * (they run on a normal JDK 17+, just slower than on a GraalVM JDK).
 */
class PolyglotEngineServiceTest {

    private static SandboxContextFactory factory;
    private static ExecutionWatchdog watchdog;
    private static PolyglotEngineService engine;

    @BeforeAll
    static void setUp() {
        PolyglotMeshProperties props = new PolyglotMeshProperties(
                new PolyglotMeshProperties.Sandbox(5000, 20000, 65536, 20_000_000, 4, List.of("python", "javascript")),
                new PolyglotMeshProperties.Cors(List.of()),
                new PolyglotMeshProperties.Compare(5, 25));
        factory = new SandboxContextFactory(props);
        watchdog = new ExecutionWatchdog();
        engine = new PolyglotEngineService(List.of(
                new PythonExecutionService(factory, watchdog, props),
                new JavaScriptExecutionService(factory, watchdog, props)));
        // Warm up: the first start of each language is slow on a normal JDK. Results are ignored.
        engine.execute(ExecutionCommand.of("python", "print(1)"));
        engine.execute(ExecutionCommand.of("javascript", "1 + 1"));
    }

    @AfterAll
    static void tearDown() {
        factory.shutdown();
        watchdog.shutdown();
    }

    @Test
    void runsPython() {
        ExecutionResult result = engine.execute(ExecutionCommand.of("python", "print('Hello PolyglotMesh')"));
        assertEquals(ExecutionStatus.SUCCESS, result.status(), result.error());
        assertTrue(result.output().contains("Hello PolyglotMesh"));
    }

    @Test
    void runsJavaScript() {
        ExecutionResult result = engine.execute(ExecutionCommand.of("javascript", "console.log(6 * 7)"));
        assertEquals(ExecutionStatus.SUCCESS, result.status(), result.error());
        assertTrue(result.output().contains("42"));
    }

    @Test
    void reportsSyntaxErrors() {
        ExecutionResult result = engine.execute(ExecutionCommand.of("python", "def broken(:"));
        assertEquals(ExecutionStatus.ERROR, result.status());
        assertNotNull(result.error());
    }

    @Test
    void stopsInfiniteLoops() {
        ExecutionResult result = engine.execute(
                new ExecutionCommand("python", "while True:\n    pass", Map.of(), 1000L));
        assertTrue(result.status() == ExecutionStatus.TIMEOUT || result.status() == ExecutionStatus.RESOURCE_LIMIT,
                "expected TIMEOUT or RESOURCE_LIMIT but was " + result.status());
    }

    @Test
    void deniesHostFileAccess() {
        ExecutionResult result = engine.execute(
                ExecutionCommand.of("python", "print(open('/etc/passwd').read())"));
        assertNotEquals(ExecutionStatus.SUCCESS, result.status());
    }

    @Test
    void deniesJavaClassLookupFromJavaScript() {
        ExecutionResult result = engine.execute(
                ExecutionCommand.of("javascript", "console.log(Java.type('java.lang.System').getProperty('user.home'))"));
        assertNotEquals(ExecutionStatus.SUCCESS, result.status());
    }

    @Test
    void pythonCanChangeAJavaHashMapInPlace() {
        Map<String, Object> data = new HashMap<>();
        data.put("price", 100.0);
        ExecutionResult result = engine.execute(new ExecutionCommand("python",
                "data['price'] = data['price'] * 2\ndata['seen'] = True", Map.of("data", data), null));
        assertEquals(ExecutionStatus.SUCCESS, result.status(), result.error());
        assertEquals(200.0, ((Number) data.get("price")).doubleValue());
        assertEquals(Boolean.TRUE, data.get("seen"));
    }

    @Test
    void javaScriptCanChangeAJavaHashMapInPlace() {
        Map<String, Object> data = new HashMap<>();
        data.put("price", 100.0);
        ExecutionResult result = engine.execute(new ExecutionCommand("javascript",
                "data.price = data.price * 2; data.seen = true;", Map.of("data", data), null));
        assertEquals(ExecutionStatus.SUCCESS, result.status(), result.error());
        assertEquals(200.0, ((Number) data.get("price")).doubleValue());
        assertEquals(Boolean.TRUE, data.get("seen"));
    }

    @Test
    void capsRunawayOutput() {
        ExecutionResult result = engine.execute(new ExecutionCommand("javascript",
                "for (let i = 0; i < 200000; i++) { console.log('x'.repeat(100)); }", Map.of(), 3000L));
        assertTrue(result.outputTruncated() || result.status() != ExecutionStatus.SUCCESS);
        assertTrue(result.output().length() <= 65536);
    }
}
