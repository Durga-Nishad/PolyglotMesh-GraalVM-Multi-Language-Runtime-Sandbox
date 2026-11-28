package com.polyglotmesh.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.exception.SandboxViolationException;
import com.polyglotmesh.exception.ValidationException;

/**
 * First line of defence: rejects obviously bad input BEFORE a GraalVM context is created.
 *
 * <p>IMPORTANT: this is a convenience pre-filter that gives users a friendly message. It is easy to bypass
 * (for example by building a string at runtime), so it is NOT a security boundary. The real restrictions are
 * the GraalVM Context settings in {@link SandboxContextFactory}, and for genuinely untrusted code you should
 * also isolate the whole process (container, seccomp, no network, cgroup limits).
 */
@Component
public class SandboxValidator {

    private record Rule(String id, Pattern pattern, String message) {
    }

    private static Rule rule(String id, String regex, String message) {
        return new Rule(id, Pattern.compile(regex, Pattern.MULTILINE), message);
    }

    private static final String PY_BLOCKED_MODULES =
            "os|sys|subprocess|socket|ctypes|shutil|pathlib|importlib|multiprocessing|threading|signal|resource|builtins";

    private static final Map<String, List<Rule>> RULES = Map.of(
            "python", List.of(
                    rule("py-import-system", "^[ \\t]*import[ \\t]+[\\w ,.]*\\b(" + PY_BLOCKED_MODULES + ")\\b",
                            "Importing system-level modules (os, sys, subprocess, socket, ...) is not allowed"),
                    rule("py-from-system", "^[ \\t]*from[ \\t]+(" + PY_BLOCKED_MODULES + ")\\b",
                            "Importing system-level modules (os, sys, subprocess, socket, ...) is not allowed"),
                    rule("py-dunder-import", "__import__\\s*\\(",
                            "Dynamic imports with __import__ are not allowed"),
                    rule("py-open", "(?<![\\w.])open\\s*\\(",
                            "File access with open() is not allowed"),
                    rule("py-host-bridge", "^[ \\t]*(import|from)[ \\t]+(java|polyglot)\\b",
                            "Accessing the Java host or other languages from Python is not allowed")),
            "javascript", List.of(
                    rule("js-require", "\\brequire\\s*\\(", "require() is not available in the sandbox"),
                    rule("js-process", "\\bprocess\\s*\\.", "The process object is not available in the sandbox"),
                    rule("js-java-bridge", "\\bJava\\s*\\.|\\bPackages\\b|\\bimportPackage\\b|\\bimportClass\\b",
                            "Accessing Java classes from JavaScript is not allowed"),
                    rule("js-java-package", "\\bjava\\s*\\.\\s*(lang|io|net|util|nio)\\b",
                            "Accessing Java packages from JavaScript is not allowed"),
                    rule("js-polyglot", "\\bPolyglot\\s*\\.", "Cross-language evaluation is not allowed"),
                    rule("js-load", "\\bload(WithNewGlobal)?\\s*\\(", "load() is not allowed"),
                    rule("js-dynamic-import", "\\bimport\\s*\\(", "Dynamic import() is not allowed")));

    private final int maxCodeLength;

    public SandboxValidator(PolyglotMeshProperties properties) {
        this.maxCodeLength = properties.sandbox().maxCodeLength();
    }

    /**
     * @param language canonical language name ("python" or "javascript")
     * @throws ValidationException       when the code is null, blank, too long or contains NUL characters (HTTP 400)
     * @throws SandboxViolationException when a deny rule matches (reported as status BLOCKED)
     */
    public void validate(String language, String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("code", "code must not be empty");
        }
        if (code.length() > maxCodeLength) {
            throw new ValidationException("code",
                    "code is too long (" + code.length() + " characters, maximum " + maxCodeLength + ")");
        }
        if (code.indexOf('\0') >= 0) {
            throw new ValidationException("code", "code must not contain NUL characters");
        }
        for (Rule rule : RULES.getOrDefault(language, List.of())) {
            if (rule.pattern().matcher(code).find()) {
                throw new SandboxViolationException(rule.id(), rule.message());
            }
        }
    }

    /** Human-readable list of the pre-filter rules, shown on the Security page. */
    public List<String> describeRules() {
        List<String> out = new ArrayList<>();
        RULES.forEach((language, rules) -> rules.stream()
                .map(Rule::message)
                .distinct()
                .forEach(message -> out.add(language + ": " + message)));
        return out;
    }
}
