package com.polyglotmesh.sandbox;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.exception.TooManyExecutionsException;

/** Caps how many scripts run at the same time so one user cannot starve the server. */
@Component
public class ConcurrencyLimiter {

    private final Semaphore slots;
    private final int max;

    public ConcurrencyLimiter(PolyglotMeshProperties properties) {
        this.max = Math.max(1, properties.sandbox().maxConcurrentExecutions());
        this.slots = new Semaphore(max);
    }

    /** Use in try-with-resources: {@code try (var permit = limiter.acquire()) { ... }} */
    public Permit acquire() {
        if (!slots.tryAcquire()) {
            throw new TooManyExecutionsException(max);
        }
        return new Permit(slots);
    }

    public int activeExecutions() {
        return max - slots.availablePermits();
    }

    public int maxExecutions() {
        return max;
    }

    public static final class Permit implements AutoCloseable {
        private final Semaphore slots;
        private boolean released;

        private Permit(Semaphore slots) {
            this.slots = slots;
        }

        @Override
        public synchronized void close() {
            if (!released) {
                released = true;
                slots.release();
            }
        }
    }
}
