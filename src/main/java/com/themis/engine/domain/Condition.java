package com.themis.engine.domain;

import java.util.List;
import java.util.Map;

/**
 * Represents a status condition (e.g., Sickened, Shaken) that applies modifiers to character stats.
 */
public record Condition(
    String id,
    String name,
    Map<StatType, List<Modifier>> modifiers
) {
    public Condition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Condition id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Condition name cannot be null or blank");
        }
        modifiers = Map.copyOf(modifiers == null ? Map.of() : modifiers);
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
