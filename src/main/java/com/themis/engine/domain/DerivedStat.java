package com.themis.engine.domain;

/**
 * Represents a derived statistic (e.g., Armor Class, Saving Throws).
 * Combines a base value, a key attribute modifier, and dynamic modifiers.
 */
public class DerivedStat implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final int baseValue;
    private final ModifierStack modifierStack = new ModifierStack();

    public DerivedStat(int baseValue) {
        this.baseValue = baseValue;
    }

    public int getBaseValue() {
        return baseValue;
    }

    public ModifierStack getModifierStack() {
        return modifierStack;
    }

    /**
     * Calculates the total value of this derived statistic.
     * @param attributeModifier the modifier from the relevant base attribute (e.g., Dexterity for AC).
     * @return the total calculated value.
     */
    public int getValue(int attributeModifier) {
        return baseValue + attributeModifier + modifierStack.getTotal();
    }
}
