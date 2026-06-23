package com.themis.engine.domain;

/**
 * Represents a numerical bonus or penalty of a specific type from a specific source.
 */
public record Modifier(int value, ModifierType type, ModifierSource source) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Modifier {
        if (type == null) {
            throw new IllegalArgumentException("Modifier type cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Modifier source cannot be null");
        }
    }
}
