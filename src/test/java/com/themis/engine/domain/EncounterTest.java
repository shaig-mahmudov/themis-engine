package com.themis.engine.domain;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;
import static org.junit.jupiter.api.Assertions.*;

class EncounterTest {

    @Test
    void testCreateEncounterAndAddParticipants() {
        Encounter encounter = new Encounter("enc-1", "Goblin Ambush");
        assertEquals("enc-1", encounter.getId());
        assertEquals("Goblin Ambush", encounter.getName());
        assertEquals(EncounterStatus.PENDING, encounter.getStatus());

        EncounterParticipant p1 = new EncounterParticipant("c1", CombatantType.CHARACTER, "Valeros", null, null, 2);
        EncounterParticipant p2 = new EncounterParticipant("c2", CombatantType.CHARACTER, "Seoni", null, null, 4);

        encounter.addParticipant(p1);
        encounter.addParticipant(p2);

        assertEquals(2, encounter.getParticipants().size());
        assertTrue(encounter.getParticipants().contains(p1));
        assertTrue(encounter.getParticipants().contains(p2));
    }

    @Test
    void testCannotAddDuplicateParticipant() {
        Encounter encounter = new Encounter("enc-1", "Goblin Ambush");
        EncounterParticipant p1 = new EncounterParticipant("c1", CombatantType.CHARACTER, "Valeros", null, null, 2);
        encounter.addParticipant(p1);

        assertThrows(IllegalArgumentException.class, () -> encounter.addParticipant(p1));
    }

    @Test
    void testStartEncounterHybridInitiativeAndSorting() {
        Encounter encounter = new Encounter("enc-1", "Goblin Ambush");
        EncounterParticipant p1 = new EncounterParticipant("c1", CombatantType.CHARACTER, "Valeros", null, null, 2); // Dex Mod 2
        EncounterParticipant p2 = new EncounterParticipant("c2", CombatantType.CHARACTER, "Seoni", null, null, 4);   // Dex Mod 4
        EncounterParticipant p3 = new EncounterParticipant("c3", CombatantType.CHARACTER, "Harsk", null, null, 3);   // Dex Mod 3

        encounter.addParticipant(p1);
        encounter.addParticipant(p2);
        encounter.addParticipant(p3);

        // Manual roll for Valeros = 10 -> Total = 10 + 2 = 12
        // Manual roll for Seoni = 8 -> Total = 8 + 4 = 12
        // Harsk will be rolled randomly. Let's make random return 15 -> Total = 15 + 3 = 18
        Map<String, Integer> manualRolls = new HashMap<>();
        manualRolls.put("c1", 10);
        manualRolls.put("c2", 8);

        RandomGenerator random = new RandomGenerator() {
            @Override
            public long nextLong() { return 0; }
            @Override
            public int nextInt(int origin, int bound) {
                return 15; // Returns 15 for any random roll
            }
        };

        // Dex mod lookup map
        Map<String, Integer> dexMods = Map.of("c1", 2, "c2", 4, "c3", 3);

        encounter.start(manualRolls, random, dexMods::get);

        assertEquals(EncounterStatus.ACTIVE, encounter.getStatus());
        assertEquals(1, encounter.getCurrentRound());
        assertEquals(0, encounter.getActiveParticipantIndex());

        // Order should be:
        // 1. Harsk (c3): Init Total = 18
        // 2. Seoni (c2): Init Total = 12 (Dex Mod 4 - tie breaker winner)
        // 3. Valeros (c1): Init Total = 12 (Dex Mod 2 - tie breaker loser)
        var sorted = encounter.getParticipants();
        assertEquals("c3", sorted.get(0).combatantId());
        assertEquals("c2", sorted.get(1).combatantId());
        assertEquals("c1", sorted.get(2).combatantId());

        assertEquals(15, sorted.get(0).initiativeRoll());
        assertEquals(18, sorted.get(0).initiativeTotal());

        assertEquals(8, sorted.get(1).initiativeRoll());
        assertEquals(12, sorted.get(1).initiativeTotal());

        assertEquals(10, sorted.get(2).initiativeRoll());
        assertEquals(12, sorted.get(2).initiativeTotal());
    }

    @Test
    void testNextTurnAndRoundProgression() {
        Encounter encounter = new Encounter("enc-1", "Goblin Ambush");
        EncounterParticipant p1 = new EncounterParticipant("c1", CombatantType.CHARACTER, "Valeros", null, null, 2);
        EncounterParticipant p2 = new EncounterParticipant("c2", CombatantType.CHARACTER, "Seoni", null, null, 4);
        encounter.addParticipant(p1);
        encounter.addParticipant(p2);

        Map<String, Integer> manualRolls = Map.of("c1", 10, "c2", 12);
        encounter.start(manualRolls, RandomGenerator.getDefault(), id -> 0);

        // Turn order should be c2 (total 12), then c1 (total 10)
        assertEquals(0, encounter.getActiveParticipantIndex());
        assertEquals("c2", encounter.getActiveParticipant().orElseThrow().combatantId());
        assertEquals(1, encounter.getCurrentRound());

        // Advance to next participant (c1)
        encounter.nextTurn();
        assertEquals(1, encounter.getActiveParticipantIndex());
        assertEquals("c1", encounter.getActiveParticipant().orElseThrow().combatantId());
        assertEquals(1, encounter.getCurrentRound());

        // Advance turn (wraps back to c2, new round)
        encounter.nextTurn();
        assertEquals(0, encounter.getActiveParticipantIndex());
        assertEquals("c2", encounter.getActiveParticipant().orElseThrow().combatantId());
        assertEquals(2, encounter.getCurrentRound());
    }

    @Test
    void testEndEncounter() {
        Encounter encounter = new Encounter("enc-1", "Goblin Ambush");
        encounter.end();
        assertEquals(EncounterStatus.COMPLETED, encounter.getStatus());
    }
}
