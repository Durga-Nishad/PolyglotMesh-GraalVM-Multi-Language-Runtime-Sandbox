package com.polyglotmesh.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LimitedOutputStreamTest {

    @Test
    void keepsEverythingUnderTheLimit() {
        LimitedOutputStream stream = new LimitedOutputStream(100);
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        stream.write(bytes, 0, bytes.length);
        assertEquals("hello", stream.content());
        assertFalse(stream.isTruncated());
    }

    @Test
    void dropsOutputBeyondTheLimit() {
        LimitedOutputStream stream = new LimitedOutputStream(5);
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        stream.write(bytes, 0, bytes.length);
        stream.write('!');
        assertEquals("hello", stream.content());
        assertTrue(stream.isTruncated());
    }
}
