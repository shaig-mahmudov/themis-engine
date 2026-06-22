package com.themis.engine.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionEconomyAndSpellcastingTest {

    private Character wizard;

    @BeforeEach
    void setUp() {
        // Wizard Level 1: INT 15 (+2)
        wizard = new Character(
            "wizard-1",
            "Ezren",
            1,   // Level
            8,   // STR
            12,  // DEX
            12,  // CON
            15,  // INT
            13,  // WIS
            10,  // CHA
            6,   // Base HP
            0,   // Base Attack Bonus
            0,   // Base Fort
            0,   // Base Ref
            2    // Base Will
        );

        SpellcastingFeature feature = new SpellcastingFeature(1, StatType.INTELLIGENCE);
        feature.setMaxSlots(0, 3); // 3 Cantrips
        feature.setMaxSlots(1, 2); // 2 Level 1 spells
        feature.restAndRecover();  // Populate remaining slots
        wizard.setSpellcastingFeature(feature);
    }

    @Test
    void testActionEconomy_InitialState() {
        TurnState state = wizard.getTurnState();
        assertFalse(state.isStandardUsed());
        assertFalse(state.isMoveUsed());
        assertFalse(state.isSwiftUsed());

        assertTrue(state.canConsume(ActionType.STANDARD));
        assertTrue(state.canConsume(ActionType.MOVE));
        assertTrue(state.canConsume(ActionType.SWIFT));
        assertTrue(state.canConsume(ActionType.FREE));
        assertTrue(state.canConsume(ActionType.FULL_ROUND));
    }

    @Test
    void testActionEconomy_ConsumeFullRound() {
        TurnState state = wizard.getTurnState();
        state.consume(ActionType.FULL_ROUND);

        assertTrue(state.isStandardUsed());
        assertTrue(state.isMoveUsed());
        assertFalse(state.isSwiftUsed()); // Swift is separate

        assertFalse(state.canConsume(ActionType.STANDARD));
        assertFalse(state.canConsume(ActionType.MOVE));
        assertFalse(state.canConsume(ActionType.FULL_ROUND));
        assertTrue(state.canConsume(ActionType.SWIFT));
    }

    @Test
    void testActionEconomy_ConsumeStandardPreventsFullRound() {
        TurnState state = wizard.getTurnState();
        state.consume(ActionType.STANDARD);

        assertTrue(state.isStandardUsed());
        assertFalse(state.isMoveUsed());

        assertFalse(state.canConsume(ActionType.FULL_ROUND));
        assertFalse(state.canConsume(ActionType.STANDARD));
        assertTrue(state.canConsume(ActionType.MOVE));
    }

    @Test
    void testActionEconomy_DowngradeStandardToMove() {
        TurnState state = wizard.getTurnState();
        // Consume first move action
        state.consume(ActionType.MOVE);
        assertFalse(state.isStandardUsed());
        assertTrue(state.isMoveUsed());

        // Can still consume another move because standard is available
        assertTrue(state.canConsume(ActionType.MOVE));
        
        // Consume second move action (trades standard action)
        state.consume(ActionType.MOVE);
        assertTrue(state.isStandardUsed());
        assertTrue(state.isMoveUsed());

        // No standard or move remaining
        assertFalse(state.canConsume(ActionType.MOVE));
        assertFalse(state.canConsume(ActionType.STANDARD));
    }

    @Test
    void testActionEconomy_InvalidConsumesThrow() {
        TurnState state = wizard.getTurnState();
        state.consume(ActionType.STANDARD);
        assertThrows(IllegalStateException.class, () -> state.consume(ActionType.STANDARD));
        assertThrows(IllegalStateException.class, () -> state.consume(ActionType.FULL_ROUND));
    }

    @Test
    void testSpellcasting_CalculateSaveDC() {
        // Base INT modifier is +2. Save DC for level 1 spell = 10 + 1 + 2 = 13.
        assertEquals(13, wizard.getSpellSaveDC(1));

        // Buff intelligence with Headband of Vast Intelligence +2
        EquippableItem headband = new EquippableItem(
            "headband-1",
            "Headband of Vast Intelligence +2",
            Map.of(StatType.INTELLIGENCE, List.of(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("headband-1", "Headband of Vast Intelligence +2", SourceType.ITEM))))
        );
        wizard.equip(headband);

        // INT is now 17 (+3). Save DC for level 1 spell = 10 + 1 + 3 = 14.
        assertEquals(14, wizard.getSpellSaveDC(1));

        // Unequip restores DC
        wizard.unequip(headband);
        assertEquals(13, wizard.getSpellSaveDC(1));
    }

    @Test
    void testSpellcasting_SlotConsumption() {
        SpellcastingFeature feature = wizard.getSpellcastingFeature();
        
        assertEquals(2, feature.getRemainingSlots(1));
        feature.consumeSlot(1);
        assertEquals(1, feature.getRemainingSlots(1));
        feature.consumeSlot(1);
        assertEquals(0, feature.getRemainingSlots(1));

        assertThrows(IllegalStateException.class, () -> feature.consumeSlot(1));

        // Recovery
        feature.restAndRecover();
        assertEquals(2, feature.getRemainingSlots(1));
    }
}
