package com.themis.engine.domain;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents armor or shield that can be equipped by a character.
 */
public record Armor(
    String id,
    String name,
    Map<StatType, List<Modifier>> modifiers,
    Integer maxDexterityBonus
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Armor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Armor id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Armor name cannot be null or blank");
        }
        if (maxDexterityBonus != null && maxDexterityBonus < 0) {
            throw new IllegalArgumentException("Max dexterity bonus cannot be negative");
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
     * Gets the modifiers this armor provides for a specific stat.
     */
    public List<Modifier> getModifiersFor(StatType statType) {
        return modifiers.getOrDefault(statType, List.of());
    }
}
