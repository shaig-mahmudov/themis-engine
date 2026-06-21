package com.themis.engine.api;

import com.themis.engine.domain.CombatantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddParticipantRequest(
    @NotBlank(message = "Combatant ID cannot be blank")
    String combatantId,

    @NotNull(message = "Combatant Type cannot be null")
    CombatantType combatantType,

    String name,

    Integer dexterityModifier
) {}
