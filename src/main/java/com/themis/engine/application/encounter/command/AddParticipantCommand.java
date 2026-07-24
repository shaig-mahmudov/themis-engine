package com.themis.engine.application.encounter.command;

import com.themis.engine.domain.CombatantType;

/**
 * Groups the inputs required to add a participant to an encounter.
 */
public record AddParticipantCommand(
    String encounterId,
    String combatantId,
    CombatantType combatantType,
    String name,
    Integer dexterityModifier
) {
}
