package com.themis.engine.domain;

/**
 * Categorizes weapons to determine their attack and damage scaling rules.
 */
public enum WeaponType {
    /** Melee weapons use Strength for both attack and damage rolls. */
    MELEE,
    
    /** Ranged weapons use Dexterity for attack rolls and receive no default attribute bonus to damage. */
    RANGED,
    
    /** Finesse weapons use Dexterity for attack rolls and Strength for damage rolls. */
    FINESSE
}
