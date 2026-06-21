package com.themis.engine.domain;

import java.util.List;
import java.util.Map;

/**
 * Represents an equippable item that can apply modifiers to a character's stats.
 */
public record EquippableItem(
    String id,
    String name,
    Map<StatType, List<Modifier>> modifiers
) implements java.io.Serializable {
    public EquippableItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Item id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or blank");
        }
        modifiers = Map.copyOf(modifiers == null ? Map.of() : modifiers);
    }

    /**
     * Gets the modifiers this item provides for a specific stat.
     * @param statType the target statistic.
     * @return a list of modifiers, or an empty list if none.
     */
    public List<Modifier> getModifiersFor(StatType statType) {
        return modifiers.getOrDefault(statType, List.of());
    }
}
