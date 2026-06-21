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
    Map<StatType, List<Modifier>> modifiers,
    Integer durationRounds,
    String stackingGroup
) implements java.io.Serializable {
    public Condition(String id, String name, Map<StatType, List<Modifier>> modifiers) {
        this(id, name, modifiers, null, null);
    }

    public Condition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Condition id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Condition name cannot be null or blank");
        }
        
        // Deep copy the map structure to ensure lists are also immutable
        // Also normalize modifier sources to match stackingGroup or name so stacking engine works correctly
        Map<StatType, List<Modifier>> tempModifiers = new java.util.HashMap<>();
        String effectiveSource = (stackingGroup != null && !stackingGroup.isBlank()) ? stackingGroup : name;
        
        if (modifiers != null) {
            for (Map.Entry<StatType, List<Modifier>> entry : modifiers.entrySet()) {
                if (entry.getValue() != null) {
                    List<Modifier> normalizedModifiers = entry.getValue().stream()
                        .map(m -> new Modifier(m.value(), m.type(), effectiveSource))
                        .toList();
                    tempModifiers.put(entry.getKey(), List.copyOf(normalizedModifiers));
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

    public Condition withDuration(Integer newDuration) {
        return new Condition(id, name, modifiers, newDuration, stackingGroup);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition = (Condition) o;
        return id.equals(condition.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
