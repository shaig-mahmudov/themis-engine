package com.themis.engine.api.encounter;

import com.themis.engine.api.encounter.response.EncounterResponse;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Component responsible for mapping between Encounter API contracts and Domain objects.
 */
@Component
public class EncounterApiMapper {

    public EncounterResponse toResponse(Encounter domain) {
        Objects.requireNonNull(domain, "Encounter cannot be null");

        List<EncounterResponse.ParticipantResponseDto> participantsList = domain.getParticipants().stream()
            .map(this::toParticipantResponse)
            .toList();

        EncounterResponse.ParticipantResponseDto active = domain.getActiveParticipant()
            .map(this::toParticipantResponse)
            .orElse(null);

        return new EncounterResponse(
            domain.getId(),
            domain.getVersion(),
            domain.getName(),
            domain.getStatus().name(),
            domain.getCurrentRound(),
            domain.getActiveParticipantIndex(),
            participantsList,
            active
        );
    }

    private EncounterResponse.ParticipantResponseDto toParticipantResponse(EncounterParticipant p) {
        Objects.requireNonNull(p, "Encounter participant cannot be null");
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
