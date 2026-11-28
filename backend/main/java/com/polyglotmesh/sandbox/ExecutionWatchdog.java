package com.polyglotmesh.sandbox;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Enforces the execution timeout. When the time is up it cancels the running Context from a different thread
 * ({@code context.close(true)}), which makes the script stop even if it is stuck in {@code while True: pass}.
 */
@Component
public class ExecutionWatchdog {

    private static final Logger log = LoggerFactory.getLogger(ExecutionWatchdog.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "polyglotmesh-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    public Handle arm(Context context, long timeoutMs) {
        AtomicBoolean fired = new AtomicBoolean(false);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            fired.set(true);
            try {
                context.close(true);
            } catch (RuntimeException ex) {
                log.debug("Watchdog could not close context cleanly: {}", ex.toString());
            }
        }, Math.max(1, timeoutMs), TimeUnit.MILLISECONDS);
        return new Handle(future, fired);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /** Returned by {@link #arm}; call {@link #disarm()} when the script finishes in time. */
    public static final class Handle {
        private final ScheduledFuture<?> future;
        private final AtomicBoolean fired;

        Handle(ScheduledFuture<?> future, AtomicBoolean fired) {
            this.future = future;
            this.fired = fired;
        }

        public void disarm() {
            future.cancel(false);
        }

        public boolean timedOut() {
            return fired.get();
        }
    }
}
