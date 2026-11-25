package com.polyglotmesh.exception;

/** Thrown when all execution slots are busy. Mapped to HTTP 429. */
public class TooManyExecutionsException extends RuntimeException {

    public TooManyExecutionsException(int max) {
        super("All " + max + " execution slots are busy. Please try again in a moment.");
    }
}
