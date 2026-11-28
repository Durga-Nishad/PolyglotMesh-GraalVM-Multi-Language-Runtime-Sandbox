package com.polyglotmesh.sandbox;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.polyglotmesh.config.PolyglotMeshProperties;
import com.polyglotmesh.exception.ValidationException;

/**
 * Single place that knows which languages exist, which of them can be executed,
 * and how API names ("javascript") map to GraalVM language ids ("js").
 */
@Component
public class SupportedLanguages {

    private static final Map<String, String> ALIASES = Map.of(
            "python", "python",
            "py", "python",
            "python3", "python",
            "javascript", "javascript",
            "js", "javascript",
            "java", "java");

    private final Set<String> executable;

    public SupportedLanguages(PolyglotMeshProperties properties) {
        this.executable = properties.sandbox().allowedLanguages().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Returns the canonical name of a language that can be executed, or throws a 400-level error. */
    public String normalizeExecutable(String raw) {
        String canonical = canonical(raw);
        if (!executable.contains(canonical)) {
            throw new ValidationException("language",
                    "Language '" + canonical + "' cannot be executed. Supported: " + String.join(", ", executable));
        }
        return canonical;
    }

    /** Scripts may also be saved for Java (highlight-only tab), even though Java is not executed. */
    public String normalizeStorable(String raw) {
        return canonical(raw);
    }

    public Set<String> executableLanguages() {
        return executable;
    }

    /** "javascript" -> "js", "python" -> "python". */
    public String graalId(String canonicalName) {
        return "javascript".equals(canonicalName) ? "js" : canonicalName;
    }

    private String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("language", "language must not be empty");
        }
        String canonical = ALIASES.get(raw.trim().toLowerCase(Locale.ROOT));
        if (canonical == null) {
            throw new ValidationException("language",
                    "Unsupported language. Use one of: python, javascript, java (java is highlight-only).");
        }
        return canonical;
    }
}
