package com.themis.engine.infrastructure;

import com.themis.engine.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PostgresEncounterRepositoryAdapterTest {

    @Autowired
    private EncounterStore encounterStore;

    @Test
    void testSaveAndLoadEncounter_PreservesAllState() {
        // 1. Create encounter
        Encounter encounter = new Encounter("enc-test-123", "Boss Fight");
        
        EncounterParticipant p1 = new EncounterParticipant(
            "hero-1",
            CombatantType.CHARACTER,
            "Valeros",
            12,
            14,
            2
        );
        EncounterParticipant p2 = new EncounterParticipant(
            "lair-1",
            CombatantType.LAIR_ACTION,
            "Lair Spike",
            null,
            20,
            0
        );

        encounter.addParticipant(p1);
        encounter.addParticipant(p2);

        // 2. Persist
        encounterStore.save(encounter);

        // 3. Load
        Optional<Encounter> loadedOpt = encounterStore.findById("enc-test-123");
        assertTrue(loadedOpt.isPresent());
        Encounter loaded = loadedOpt.get();

        // 4. Assertions
        assertEquals(encounter.getId(), loaded.getId());
        assertEquals(encounter.getName(), loaded.getName());
        assertEquals(EncounterStatus.PENDING, loaded.getStatus());
        assertEquals(0, loaded.getCurrentRound());
        assertEquals(0, loaded.getActiveParticipantIndex());
        assertEquals(2, loaded.getParticipants().size());

        // Validate participants (retaining insertion order since it is PENDING)
        EncounterParticipant lp1 = loaded.getParticipants().get(0);
        assertEquals("hero-1", lp1.combatantId());
        assertEquals(CombatantType.CHARACTER, lp1.combatantType());
        assertEquals("Valeros", lp1.name());
        assertEquals(12, lp1.initiativeRoll());
        assertEquals(14, lp1.initiativeTotal());
        assertEquals(2, lp1.dexterityModifier());

        EncounterParticipant lp2 = loaded.getParticipants().get(1);
        assertEquals("lair-1", lp2.combatantId());
        assertEquals(CombatantType.LAIR_ACTION, lp2.combatantType());
        assertEquals("Lair Spike", lp2.name());
        assertNull(lp2.initiativeRoll());
        assertEquals(20, lp2.initiativeTotal());
        assertEquals(0, lp2.dexterityModifier());

        // 5. Update state and save again (simulate active fight)
        Encounter loadedForFight = new Encounter(
            loaded.getId(),
            loaded.getName(),
            EncounterStatus.ACTIVE,
            2,
            1,
            List.of(lp2, lp1) // Sorted: Lair Spike (20), Valeros (14)
        );

        encounterStore.save(loadedForFight);

        // Load again
        Optional<Encounter> loadedFightOpt = encounterStore.findById("enc-test-123");
        assertTrue(loadedFightOpt.isPresent());
        Encounter loadedFight = loadedFightOpt.get();

        assertEquals(EncounterStatus.ACTIVE, loadedFight.getStatus());
        assertEquals(2, loadedFight.getCurrentRound());
        assertEquals(1, loadedFight.getActiveParticipantIndex());
        assertEquals(2, loadedFight.getParticipants().size());
        assertEquals("lair-1", loadedFight.getParticipants().get(0).combatantId());
        assertEquals("hero-1", loadedFight.getParticipants().get(1).combatantId());
    }
}
