package com.themis.engine.domain;

/**
 * Represents a numerical bonus or penalty of a specific type from a specific source.
 */
public record Modifier(int value, ModifierType type, String source) {
    public Modifier {
        if (type == null) {
            throw new IllegalArgumentException("Modifier type cannot be null");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Modifier source cannot be null or blank");
        }
    }
}
