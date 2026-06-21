package com.themis.engine.domain;

import java.util.Arrays;

/**
 * Tracks a character's spellcasting abilities, including spell slots per level
 * and calculating save DCs based on their casting attribute.
 */
public class SpellcastingFeature implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final int casterLevel;
    private final StatType castingAttribute;
    
    // Arrays indexed by spell level (0 to 9)
    private final int[] maxSlots = new int[10];
    private final int[] remainingSlots = new int[10];

    public SpellcastingFeature(int casterLevel, StatType castingAttribute) {
        if (casterLevel < 1) {
            throw new IllegalArgumentException("Caster level must be at least 1");
        }
        if (castingAttribute == null) {
            throw new IllegalArgumentException("Casting attribute cannot be null");
        }
        this.casterLevel = casterLevel;
        this.castingAttribute = castingAttribute;
    }

    public int getCasterLevel() {
        return casterLevel;
    }

    public StatType getCastingAttribute() {
        return castingAttribute;
    }

    public int getMaxSlots(int spellLevel) {
        validateSpellLevel(spellLevel);
        return maxSlots[spellLevel];
    }

    public int getRemainingSlots(int spellLevel) {
        validateSpellLevel(spellLevel);
        return remainingSlots[spellLevel];
    }

    public void setMaxSlots(int spellLevel, int count) {
        validateSpellLevel(spellLevel);
        if (count < 0) {
            throw new IllegalArgumentException("Slot count cannot be negative");
        }
        maxSlots[spellLevel] = count;
        // Adjust remaining slots if they exceed new max
        remainingSlots[spellLevel] = Math.min(remainingSlots[spellLevel], count);
    }

    public void setRemainingSlots(int spellLevel, int count) {
        validateSpellLevel(spellLevel);
        if (count < 0 || count > maxSlots[spellLevel]) {
            throw new IllegalArgumentException("Remaining slots must be between 0 and max slots (" + maxSlots[spellLevel] + ")");
        }
        remainingSlots[spellLevel] = count;
    }

    /**
     * Consumes one spell slot of the given level.
     */
    public void consumeSlot(int spellLevel) {
        validateSpellLevel(spellLevel);
        if (remainingSlots[spellLevel] <= 0) {
            throw new IllegalStateException("No spell slots remaining at level " + spellLevel);
        }
        remainingSlots[spellLevel]--;
    }

    /**
     * Resets all remaining slots to their maximum values (e.g. after resting).
     */
    public void restAndRecover() {
        System.arraycopy(maxSlots, 0, remainingSlots, 0, maxSlots.length);
    }

    /**
     * Calculates the Save DC for a spell of the given level using the casting attribute modifier.
     */
    public int calculateSaveDC(int spellLevel, int attributeModifier) {
        validateSpellLevel(spellLevel);
        return 10 + spellLevel + attributeModifier;
    }

    private void validateSpellLevel(int spellLevel) {
        if (spellLevel < 0 || spellLevel > 9) {
            throw new IllegalArgumentException("Spell level must be between 0 and 9");
        }
    }
}
