package com.themis.engine.domain;

/**
 * Represents modify-able stats on a character in Pathfinder 1e.
 */
public enum StatType {
    // Core Attributes
    STRENGTH,
    DEXTERITY,
    CONSTITUTION,
    INTELLIGENCE,
    WISDOM,
    CHARISMA,

    // Saving Throws
    FORTITUDE,
    REFLEX,
    WILL,

    // Defensive & Combat Stats
    ARMOR_CLASS,
    BASE_ATTACK_BONUS,
    ATTACK_ROLL,
    DAMAGE_ROLL,
    HIT_POINTS;

    public boolean isAbilityScore() {
        return switch (this) {
            case STRENGTH, DEXTERITY, CONSTITUTION, INTELLIGENCE, WISDOM, CHARISMA -> true;
            default -> false;
        };
    }
}
