package com.themis.engine.api.encounter.response;

import java.util.List;

public record EncounterResponse(
    String id,
    String name,
    String status,
    int currentRound,
    int activeParticipantIndex,
    List<ParticipantResponseDto> participants,
    ParticipantResponseDto activeParticipant
) {
    public record ParticipantResponseDto(
        String combatantId,
        String combatantType,
        String name,
        Integer initiativeRoll,
        Integer initiativeTotal,
        int dexterityModifier
    ) {}
}
