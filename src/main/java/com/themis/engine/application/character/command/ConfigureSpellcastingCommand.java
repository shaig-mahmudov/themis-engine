package com.themis.engine.application.character.command;

import com.themis.engine.domain.StatType;

import java.util.List;

/**
 * Groups the inputs required to configure a character's spellcasting feature.
 */
public record ConfigureSpellcastingCommand(
    String characterId,
    int casterLevel,
    StatType castingAttribute,
    List<Integer> maxSlots
) {
    public ConfigureSpellcastingCommand {
        maxSlots = maxSlots == null ? null : List.copyOf(maxSlots);
    }
}
