package com.themis.engine.api.encounter;

import com.themis.engine.api.encounter.response.EncounterResponse;
import com.themis.engine.domain.CombatantType;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import com.themis.engine.domain.EncounterStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncounterApiMapperTest {

    private EncounterApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EncounterApiMapper();
    }

    @Test
    void testToResponse() {
        Encounter encounter = new Encounter("enc-1", "Goblin Fight");
        encounter.restoreVersion(3L);
        EncounterParticipant participant = new EncounterParticipant(
            "p1", CombatantType.CHARACTER, "Valeros", 15, 17, 2
        );
        encounter.addParticipant(participant);

        EncounterResponse response = mapper.toResponse(encounter);

        assertNotNull(response);
        assertEquals("enc-1", response.id());
        assertEquals(3L, response.version());
        assertEquals("Goblin Fight", response.name());
        assertEquals("PENDING", response.status());
        assertEquals(1, response.participants().size());

        EncounterResponse.ParticipantResponseDto pDto = response.participants().get(0);
        assertEquals("p1", pDto.combatantId());
        assertEquals("CHARACTER", pDto.combatantType());
        assertEquals("Valeros", pDto.name());
        assertEquals(15, pDto.initiativeRoll());
        assertEquals(17, pDto.initiativeTotal());
        assertEquals(2, pDto.dexterityModifier());
    }

    @Test
    void testToResponse_NullThrowsException() {
        assertThrows(NullPointerException.class, () -> mapper.toResponse(null));
    }
}
