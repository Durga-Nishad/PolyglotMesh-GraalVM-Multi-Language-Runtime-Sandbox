package com.polyglotmesh.util;

import com.polyglotmesh.exception.ValidationException;

/** Small null / empty / range checks used by every service so the rules are identical everywhere. */
public final class Guard {

    private Guard() {
    }

    /** Value must not be null or blank and must not exceed maxLength. Returns the value unchanged. */
    public static String requireText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, field + " must not be empty");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(field,
                    field + " is too long (" + value.length() + " characters, maximum " + maxLength + ")");
        }
        return value;
    }

    /** Same as {@link #requireText} but also trims surrounding spaces. */
    public static String requireTrimmed(String field, String value, int maxLength) {
        return requireText(field, value, maxLength).trim();
    }

    public static long requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("id", "id must be a positive number");
        }
        return id;
    }

    /** Returns "" for null/blank so JPQL queries can use the (:param = '') "no filter" pattern. */
    public static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "… [truncated]";
    }
}
