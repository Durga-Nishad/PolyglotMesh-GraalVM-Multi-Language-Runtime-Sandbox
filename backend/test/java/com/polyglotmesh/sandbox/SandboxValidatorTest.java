package com.polyglotmesh.sandbox;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.exception.SandboxViolationException;
import com.polyglotmesh.exception.ValidationException;

/** Pure unit tests: no GraalVM needed. */
class SandboxValidatorTest {

    private final SandboxValidator validator = new SandboxValidator(properties());

    static PolyglotMeshProperties properties() {
        return new PolyglotMeshProperties(
                new PolyglotMeshProperties.Sandbox(5000, 200, 65536, 20_000_000, 4, List.of("python", "javascript")),
                new PolyglotMeshProperties.Cors(List.of("http://localhost:5173")),
                new PolyglotMeshProperties.Compare(5, 25));
    }

    @Test
    void rejectsNullAndBlankCode() {
        assertThrows(ValidationException.class, () -> validator.validate("python", null));
        assertThrows(ValidationException.class, () -> validator.validate("python", ""));
        assertThrows(ValidationException.class, () -> validator.validate("javascript", "   \n  "));
    }

    @Test
    void rejectsTooLongCode() {
        assertThrows(ValidationException.class, () -> validator.validate("python", "x = 1\n".repeat(100)));
    }

    @Test
    void rejectsNulCharacters() {
        assertThrows(ValidationException.class, () -> validator.validate("python", "print(1)\0"));
    }

    @Test
    void acceptsNormalCode() {
        assertDoesNotThrow(() -> validator.validate("python", "print('Hello PolyglotMesh')"));
        assertDoesNotThrow(() -> validator.validate("javascript", "console.log(1 + 2)"));
        assertDoesNotThrow(() -> validator.validate("python", "import json\nprint(json.dumps({'a': 1}))"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "import os",
            "import json, os",
            "from subprocess import run",
            "x = __import__('socket')",
            "data = open('/etc/passwd').read()",
            "import java",
            "import polyglot"
    })
    void blocksDangerousPython(String code) {
        assertThrows(SandboxViolationException.class, () -> validator.validate("python", code));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "const fs = require('fs')",
            "process.exit(1)",
            "Java.type('java.lang.System')",
            "Polyglot.eval('python', '1')",
            "load('/etc/passwd')",
            "import('fs')"
    })
    void blocksDangerousJavaScript(String code) {
        assertThrows(SandboxViolationException.class, () -> validator.validate("javascript", code));
    }

    @Test
    void doesNotBlockWordsThatOnlyContainBlockedNames() {
        assertDoesNotThrow(() -> validator.validate("python", "reopen = 1\nposition = 2"));
        assertDoesNotThrow(() -> validator.validate("javascript", "const javascriptValue = 1; const loader = 2;"));
    }
}
