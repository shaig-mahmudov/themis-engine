package com.themis.engine.domain;

public record ModifierSource(String id, String name, SourceType type) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public ModifierSource {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ModifierSource ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ModifierSource Name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("ModifierSource Type cannot be null");
        }
    }
}
