package com.themis.engine.domain;

/**
 * Represents a core ability score (e.g., Strength, Dexterity).
 * Manages its base value and active modifiers.
 */
public class Attribute {
    private final int baseScore;
    private final ModifierStack modifierStack = new ModifierStack();

    public Attribute(int baseScore) {
        if (baseScore < 0) {
            throw new IllegalArgumentException("Base score cannot be negative");
        }
        this.baseScore = baseScore;
    }

    public int getBaseScore() {
        return baseScore;
    }

    public ModifierStack getModifierStack() {
        return modifierStack;
    }

    public int getScore() {
        return baseScore + modifierStack.getTotal();
    }

    public int getModifier() {
        return Math.floorDiv(getScore() - 10, 2);
    }
}
