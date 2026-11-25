package com.polyglotmesh.util;

import java.util.concurrent.Callable;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * The API is reactive (WebFlux) but JPA and the GraalVM engine are blocking.
 * These helpers move blocking work onto Reactor's boundedElastic pool so the event-loop threads stay free.
 */
public final class Reactive {

    private Reactive() {
    }

    public static <T> Mono<T> blocking(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(Schedulers.boundedElastic());
    }

    public static Mono<Void> blockingRun(Runnable work) {
        return Mono.<Void>fromRunnable(work).subscribeOn(Schedulers.boundedElastic());
    }
}
