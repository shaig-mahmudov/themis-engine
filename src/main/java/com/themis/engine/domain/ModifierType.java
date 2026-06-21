package com.themis.engine.domain;

/**
 * Represents the type of a modifier in Pathfinder 1e.
 * Enforces stacking rules:
 * - DODGE and UNTYPED modifiers always stack.
 * - Other modifier types only apply the highest value.
 */
public enum ModifierType {
    ARMOR(false),
    SHIELD(false),
    DEXTERITY(false),
    SIZE(false),
    NATURAL_ARMOR(false),
    DEFLECTION(false),
    DODGE(true),
    ENHANCEMENT(false),
    MORALE(false),
    LUCK(false),
    SACRED(false),
    PROFANE(false),
    INSIGHT(false),
    RESISTANCE(false),
    ALCHEMICAL(false),
    UNTYPED(true);

    private final boolean stackable;

    ModifierType(boolean stackable) {
        this.stackable = stackable;
    }

    public boolean isStackable() {
        return stackable;
    }
}
