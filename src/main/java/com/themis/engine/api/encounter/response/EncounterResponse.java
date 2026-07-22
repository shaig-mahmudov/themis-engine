package com.themis.engine.api.encounter.response;

import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;

import java.util.List;
import java.util.stream.Collectors;

public record EncounterResponse(
    String id,
    String name,
    String status,
    int currentRound,
    int activeParticipantIndex,
    List<ParticipantResponseDto> participants,
    ParticipantResponseDto activeParticipant
) {
    public static EncounterResponse fromDomain(Encounter domain) {
        List<ParticipantResponseDto> participantsList = domain.getParticipants().stream()
            .map(ParticipantResponseDto::fromDomain)
            .collect(Collectors.toList());

        ParticipantResponseDto active = domain.getActiveParticipant()
            .map(ParticipantResponseDto::fromDomain)
            .orElse(null);

        return new EncounterResponse(
            domain.getId(),
            domain.getName(),
            domain.getStatus().name(),
            domain.getCurrentRound(),
            domain.getActiveParticipantIndex(),
            participantsList,
            active
        );
    }

    public record ParticipantResponseDto(
        String combatantId,
        String combatantType,
        String name,
        Integer initiativeRoll,
        Integer initiativeTotal,
        int dexterityModifier
    ) {
        public static ParticipantResponseDto fromDomain(EncounterParticipant p) {
            return new ParticipantResponseDto(
                p.combatantId(),
                p.combatantType().name(),
                p.name(),
                p.initiativeRoll(),
                p.initiativeTotal(),
                p.dexterityModifier()
            );
        }
    }
}
