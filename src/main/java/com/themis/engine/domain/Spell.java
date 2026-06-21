package com.themis.engine.domain;

/**
 * A value object representing a spell in Pathfinder 1e.
 */
public record Spell(
    String id,
    String name,
    int level,
    ActionType actionCost
) {
    public Spell {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Spell ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Spell Name cannot be null or blank");
        }
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("Spell Level must be between 0 and 9");
        }
        if (actionCost == null) {
            throw new IllegalArgumentException("Spell ActionCost cannot be null");
        }
    }
}
