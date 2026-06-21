package com.themis.engine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages a stack of active modifiers and resolves their total value
 * based on Pathfinder 1e stacking rules.
 */
public class ModifierStack implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Modifier> modifiers = new ArrayList<>();

    /**
     * Adds a modifier to the stack.
     * @param modifier the modifier to add, must not be null.
     */
    public void add(Modifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("Modifier cannot be null");
        }
        modifiers.add(modifier);
    }

    /**
     * Removes a modifier from the stack.
     * @param modifier the modifier to remove.
     */
    public void remove(Modifier modifier) {
        modifiers.remove(modifier);
    }

    /**
     * Calculates the total value of all active modifiers in the stack.
     * Enforces the following rules:
     * 1. Stackable modifier types (e.g., DODGE, UNTYPED) sum up.
     * 2. Non-stackable modifier types (e.g., ENHANCEMENT, MORALE):
     *    - The highest positive bonus applies.
     *    - The worst (most negative) penalty applies.
     *    - Both the highest bonus and worst penalty apply (they net out).
     */
    public int getTotal() {
        if (modifiers.isEmpty()) {
            return 0;
        }

        Map<ModifierType, List<Modifier>> grouped = modifiers.stream()
                .collect(Collectors.groupingBy(Modifier::type));

        int total = 0;

        for (Map.Entry<ModifierType, List<Modifier>> entry : grouped.entrySet()) {
            ModifierType type = entry.getKey();
            List<Modifier> mods = entry.getValue();

            if (type.isStackable()) {
                total += mods.stream().mapToInt(Modifier::value).sum();
            } else {
                int maxBonus = mods.stream()
                        .filter(m -> m.value() > 0)
                        .mapToInt(Modifier::value)
                        .max()
                        .orElse(0);

                int minPenalty = mods.stream()
                        .filter(m -> m.value() < 0)
                        .mapToInt(Modifier::value)
                        .min()
                        .orElse(0);

                total += maxBonus + minPenalty;
            }
        }

        return total;
    }
}
