package com.themis.engine.domain;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a weapon that can be equipped by a character.
 * Tracks attack/damage modifiers, damage roll, and critical characteristics.
 */
public record Weapon(
    String id,
    String name,
    WeaponType type,
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
        if (type == null) {
            throw new IllegalArgumentException("Weapon type cannot be null");
        }
        if (damageRoll == null) {
            throw new IllegalArgumentException("Damage roll cannot be null");
        }
        if (criticalThreatMin < 15 || criticalThreatMin > 20) {
            throw new IllegalArgumentException("Critical threat minimum must be between 15 and 20");
        }
        if (criticalMultiplier < 2) {
            throw new IllegalArgumentException("Critical multiplier must be at least 2");
        }
        
        // Deep copy the map structure to ensure lists are also immutable
        Map<StatType, List<Modifier>> tempModifiers = new HashMap<>();
        if (modifiers != null) {
            for (Map.Entry<StatType, List<Modifier>> entry : modifiers.entrySet()) {
                if (entry.getValue() != null) {
                    tempModifiers.put(entry.getKey(), List.copyOf(entry.getValue()));
                }
            }
        }
        modifiers = Map.copyOf(tempModifiers);
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
