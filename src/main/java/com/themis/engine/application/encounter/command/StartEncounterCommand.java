package com.themis.engine.application.encounter.command;

import java.util.Map;

/**
 * Groups the inputs required to start an encounter.
 */
public record StartEncounterCommand(
    String encounterId,
    Map<String, Integer> manualRolls
) {
    public StartEncounterCommand {
        manualRolls = manualRolls == null ? Map.of() : Map.copyOf(manualRolls);
    }
}
