package com.polyglotmesh.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Runs a tiny script in each language once after start-up, in a background thread.
 * The first start of GraalPy / GraalJS is slow on a normal JDK; doing it here means the first
 * script a user runs does not pay that cost. Turn it off with POLYGLOT_WARMUP=false.
 */
@Component
public class EngineWarmup {

    private static final Logger log = LoggerFactory.getLogger(EngineWarmup.class);

    private final PolyglotEngineService engine;
    private final boolean enabled;

    public EngineWarmup(PolyglotEngineService engine, @Value("${POLYGLOT_WARMUP:true}") boolean enabled) {
        this.engine = engine;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!enabled) {
            return;
        }
        Thread thread = new Thread(() -> {
            warm("javascript", "1 + 1");
            warm("python", "print(1)");
        }, "polyglotmesh-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    private void warm(String language, String code) {
        long start = System.nanoTime();
        try {
            ExecutionResult result = engine.execute(ExecutionCommand.of(language, code));
            log.info("Warm-up {}: {} in {} ms", language, result.status(), (System.nanoTime() - start) / 1_000_000);
        } catch (RuntimeException ex) {
            log.warn("Warm-up for {} failed: {}", language, ex.toString());
        }
    }
}
