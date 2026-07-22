package com.themis.engine.api.encounter;

import com.themis.engine.api.encounter.response.EncounterResponse;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component responsible for mapping between Encounter API contracts and Domain objects.
 */
@Component
public class EncounterApiMapper {

    public EncounterResponse toResponse(Encounter domain) {
        if (domain == null) {
            return null;
        }

        List<EncounterResponse.ParticipantResponseDto> participantsList = domain.getParticipants().stream()
            .map(this::toParticipantResponse)
            .toList();

        EncounterResponse.ParticipantResponseDto active = domain.getActiveParticipant()
            .map(this::toParticipantResponse)
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

    public EncounterResponse.ParticipantResponseDto toParticipantResponse(EncounterParticipant p) {
        if (p == null) {
            return null;
        }
        return new EncounterResponse.ParticipantResponseDto(
            p.combatantId(),
            p.combatantType().name(),
            p.name(),
            p.initiativeRoll(),
            p.initiativeTotal(),
            p.dexterityModifier()
        );
    }
}
