package com.themis.engine.domain;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a status condition (e.g., Sickened, Shaken) that applies modifiers to character stats.
 */
public record Condition(
    String id,
    String name,
    Map<StatType, List<Modifier>> modifiers
) implements java.io.Serializable {
    public Condition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Condition id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Condition name cannot be null or blank");
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
     * Gets the modifiers this condition provides for a specific stat.
     * @param statType the target statistic.
     * @return a list of modifiers, or an empty list if none.
     */
    public List<Modifier> getModifiersFor(StatType statType) {
        return modifiers.getOrDefault(statType, List.of());
    }
}
