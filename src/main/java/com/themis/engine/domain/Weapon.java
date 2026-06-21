package com.themis.engine.domain;

import java.util.List;
import java.util.Map;

/**
 * Represents a weapon that can be equipped by a character.
 * Tracks attack/damage modifiers, damage roll, and critical characteristics.
 */
public record Weapon(
    String id,
    String name,
    Map<StatType, List<Modifier>> modifiers,
    DiceRoll damageRoll,
    int criticalThreatMin,
    int criticalMultiplier
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Weapon {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Weapon id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Weapon name cannot be null or blank");
        }
        if (damageRoll == null) {
            throw new IllegalArgumentException("Damage roll cannot be null");
        }
        if (criticalThreatMin < 1 || criticalThreatMin > 20) {
            throw new IllegalArgumentException("Critical threat minimum must be between 1 and 20");
        }
        if (criticalMultiplier < 1) {
            throw new IllegalArgumentException("Critical multiplier must be at least 1");
        }
        modifiers = Map.copyOf(modifiers == null ? Map.of() : modifiers);
    }

    /**
     * Gets the modifiers this weapon provides for a specific stat.
     */
    public List<Modifier> getModifiersFor(StatType statType) {
        return modifiers.getOrDefault(statType, List.of());
    }

    /**
     * Helper to represent this weapon as a general EquippableItem.
     */
    public EquippableItem toEquippableItem() {
        return new EquippableItem(id, name, modifiers);
    }
}
