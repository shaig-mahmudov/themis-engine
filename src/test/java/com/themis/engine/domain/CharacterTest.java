package com.themis.engine.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CharacterTest {

    private Character fighter;

    @BeforeEach
    void setUp() {
        // Fighter Level 1: STR 15 (+2), DEX 12 (+1), CON 14 (+2), INT 8 (-1), WIS 10 (0), CHA 13 (+1)
        // Base HP: 10, BAB: 1, Base Saves: Fort +2, Ref 0, Will 0
        fighter = new Character(
            "char-123",
            "Valeros",
            1,   // Level
            15,  // STR
            12,  // DEX
            14,  // CON
            8,   // INT
            10,  // WIS
            13,  // CHA
            10,  // Base HP
            1,   // Base Attack Bonus
            2,   // Base Fort
            0,   // Base Ref
            0    // Base Will
        );
    }

    @Test
    void testBaseAttributeScoresAndModifiers() {
        assertEquals(15, fighter.getAttributeScore(StatType.STRENGTH));
        assertEquals(2, fighter.getAttributeModifier(StatType.STRENGTH));

        assertEquals(12, fighter.getAttributeScore(StatType.DEXTERITY));
        assertEquals(1, fighter.getAttributeModifier(StatType.DEXTERITY));

        assertEquals(8, fighter.getAttributeScore(StatType.INTELLIGENCE));
        assertEquals(-1, fighter.getAttributeModifier(StatType.INTELLIGENCE));
    }

    @Test
    void testDerivedStatsCalculateCorrectly() {
        // Fortitude = Base 2 + Con Mod 2 = 4
        assertEquals(4, fighter.getSave(StatType.FORTITUDE));

        // Reflex = Base 0 + Dex Mod 1 = 1
        assertEquals(1, fighter.getSave(StatType.REFLEX));

        // Will = Base 0 + Wis Mod 0 = 0
        assertEquals(0, fighter.getSave(StatType.WILL));

        // AC = 10 + Dex Mod 1 = 11
        assertEquals(11, fighter.getArmorClass());

        // Max HP = Base 10 + (Level 1 * Con Mod 2) = 12
        assertEquals(12, fighter.getMaxHitPoints());
    }

    @Test
    void testEquipBeltOfGiantStrengthUpdatesStrengthAndDerivedStats() {
        EquippableItem belt = new EquippableItem(
            "belt-1",
            "Belt of Giant Strength +2",
            Map.of(StatType.STRENGTH, List.of(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("belt-1", "Belt of Giant Strength +2", SourceType.ITEM))))
        );

        fighter.equip(belt);

        // Strength score should become 17, and modifier +3
        assertEquals(17, fighter.getAttributeScore(StatType.STRENGTH));
        assertEquals(3, fighter.getAttributeModifier(StatType.STRENGTH));

        // Unequipping should restore it
        fighter.unequip(belt);
        assertEquals(15, fighter.getAttributeScore(StatType.STRENGTH));
        assertEquals(2, fighter.getAttributeModifier(StatType.STRENGTH));
    }

    @Test
    void testEquipRingOfProtectionAndCloakOfResistance() {
        EquippableItem ring = new EquippableItem(
            "ring-1",
            "Ring of Protection +1",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(1, ModifierType.DEFLECTION, new ModifierSource("ring-1", "Ring of Protection +1", SourceType.ITEM))))
        );

        EquippableItem cloak = new EquippableItem(
            "cloak-1",
            "Cloak of Resistance +1",
            Map.of(
                StatType.FORTITUDE, List.of(new Modifier(1, ModifierType.RESISTANCE, new ModifierSource("cloak-1", "Cloak of Resistance +1", SourceType.ITEM))),
                StatType.REFLEX, List.of(new Modifier(1, ModifierType.RESISTANCE, new ModifierSource("cloak-1", "Cloak of Resistance +1", SourceType.ITEM))),
                StatType.WILL, List.of(new Modifier(1, ModifierType.RESISTANCE, new ModifierSource("cloak-1", "Cloak of Resistance +1", SourceType.ITEM)))
            )
        );

        fighter.equip(ring);
        fighter.equip(cloak);

        // AC = 11 + 1 Deflection = 12
        assertEquals(12, fighter.getArmorClass());

        // Fortitude = 4 + 1 Resistance = 5
        assertEquals(5, fighter.getSave(StatType.FORTITUDE));
        assertEquals(1, fighter.getSave(StatType.WILL));

        // Stacking check: equip another Ring of Protection +1 (deflection does not stack)
        EquippableItem ring2 = new EquippableItem(
            "ring-2",
            "Ring of Protection +1 (Duplicate)",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(1, ModifierType.DEFLECTION, new ModifierSource("ring-2", "Ring of Protection +1 (Duplicate)", SourceType.ITEM))))
        );
        fighter.equip(ring2);
        // AC should remain 12
        assertEquals(12, fighter.getArmorClass());
    }

    @Test
    void testApplyConditionSickened() {
        // Sickened applies a -2 Untyped penalty to saving throws
        Condition sickened = new Condition(
            "cond-sickened",
            "Sickened",
            Map.of(
                StatType.FORTITUDE, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("cond-sickened", "Sickened", SourceType.CONDITION))),
                StatType.REFLEX, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("cond-sickened", "Sickened", SourceType.CONDITION))),
                StatType.WILL, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("cond-sickened", "Sickened", SourceType.CONDITION)))
            )
        );

        fighter.applyCondition(sickened);

        // Fortitude = 4 - 2 = 2
        assertEquals(2, fighter.getSave(StatType.FORTITUDE));
        // Reflex = 1 - 2 = -1
        assertEquals(-1, fighter.getSave(StatType.REFLEX));

        fighter.removeCondition(sickened);
        assertEquals(4, fighter.getSave(StatType.FORTITUDE));
    }

    @Test
    void testDamageHealingConsciousnessAndDeath() {
        // Max HP = 12. Con score = 14.
        assertEquals(12, fighter.getMaxHitPoints());
        assertTrue(fighter.isConscious());
        assertFalse(fighter.isDead());

        // Take 5 damage -> 7 HP
        fighter.damage(5);
        assertEquals(7, fighter.getCurrentHitPoints());
        assertTrue(fighter.isConscious());

        // Take 7 damage -> 0 HP (staggered but conscious in PF1e)
        fighter.damage(7);
        assertEquals(0, fighter.getCurrentHitPoints());
        assertTrue(fighter.isConscious());
        assertFalse(fighter.isDead());

        // Take 1 more damage -> -1 HP (unconscious)
        fighter.damage(1);
        assertEquals(-1, fighter.getCurrentHitPoints());
        assertFalse(fighter.isConscious());
        assertFalse(fighter.isDead());

        // Take damage up to -14 (Con score is 14 -> dead at <= -14)
        fighter.damage(13); // total damage 26 -> current HP = -14
        assertEquals(-14, fighter.getCurrentHitPoints());
        assertFalse(fighter.isConscious());
        assertTrue(fighter.isDead());

        // Healing 5 points should restore consciousness (if above 0, but -9 is still unconscious)
        fighter.heal(5); // current HP = -9
        assertEquals(-9, fighter.getCurrentHitPoints());
        assertFalse(fighter.isConscious());
        assertFalse(fighter.isDead()); // not dead anymore

        fighter.heal(10); // current HP = 1
        assertEquals(1, fighter.getCurrentHitPoints());
        assertTrue(fighter.isConscious());
    }

    @Test
    void testConModifierIncreaseDynamicallyUpdatesMaxHPAndCurrentHP() {
        // Max HP starts at 12 (Base 10 + Con Mod 2 * Level 1)
        assertEquals(12, fighter.getMaxHitPoints());

        // Deal 4 damage -> current HP = 8
        fighter.damage(4);
        assertEquals(8, fighter.getCurrentHitPoints());

        // Equip a Belt of Constitution +2 (increases Con to 16 -> mod +3)
        EquippableItem conBelt = new EquippableItem(
            "belt-con",
            "Belt of Constitution +2",
            Map.of(StatType.CONSTITUTION, List.of(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("belt-con", "Belt of Constitution +2", SourceType.ITEM))))
        );
        fighter.equip(conBelt);

        // Con score = 16, Mod = +3
        assertEquals(16, fighter.getAttributeScore(StatType.CONSTITUTION));
        assertEquals(3, fighter.getAttributeModifier(StatType.CONSTITUTION));

        // Max HP should dynamically update to 13 (Base 10 + Con Mod 3 * Level 1)
        assertEquals(13, fighter.getMaxHitPoints());

        // Current HP should dynamically update to 9 (Max HP 13 - damage 4)
        assertEquals(9, fighter.getCurrentHitPoints());

        // Death threshold should also update to -16
        fighter.damage(25); // total damage = 29 -> current HP = -16
        assertTrue(fighter.isDead());
    }

    @Test
    void testConditionDurationAndExpiration() {
        Condition sickened = new Condition(
            "cond-sickened",
            "Sickened",
            Map.of(StatType.FORTITUDE, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("cond-sickened", "Sickened", SourceType.CONDITION)))),
            2, // 2 rounds duration
            null
        );

        fighter.applyCondition(sickened);
        assertEquals(2, fighter.getSave(StatType.FORTITUDE)); // Penalty applied
        assertEquals(2, fighter.getActiveConditions().get(0).durationRounds());

        // Round 1 start -> decrements to 1
        fighter.startTurn();
        assertEquals(2, fighter.getSave(StatType.FORTITUDE)); // Penalty still applied
        assertEquals(1, fighter.getActiveConditions().get(0).durationRounds());

        // Round 2 start -> decrements to 0, gets removed
        fighter.startTurn();
        assertEquals(4, fighter.getSave(StatType.FORTITUDE)); // Penalty removed!
        assertTrue(fighter.getActiveConditions().isEmpty());
    }

    @Test
    void testConditionStackingGroup() {
        // Shaken: -1 morale penalty to saving throws
        Condition shaken = new Condition(
            "cond-shaken",
            "Shaken",
            Map.of(StatType.FORTITUDE, List.of(new Modifier(-1, ModifierType.UNTYPED, new ModifierSource("cond-shaken", "Shaken", SourceType.CONDITION)))),
            null,
            "Fear"
        );

        // Frightened: -2 morale penalty to saving throws
        Condition frightened = new Condition(
            "cond-frightened",
            "Frightened",
            Map.of(StatType.FORTITUDE, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("cond-frightened", "Frightened", SourceType.CONDITION)))),
            null,
            "Fear"
        );

        fighter.applyCondition(shaken);
        fighter.applyCondition(frightened);

        // Since they are in the same stacking group "Fear", they both apply modifiers with source="Fear".
        // Therefore, they shouldn't stack, even though they are UNTYPED!
        // Worst penalty is -2. Fortitude = Base 4 - 2 = 2
        assertEquals(2, fighter.getSave(StatType.FORTITUDE));

        // If we remove the worse one, the lesser one (-1) is still active
        fighter.removeCondition(frightened);
        assertEquals(3, fighter.getSave(StatType.FORTITUDE));

        // Remove both -> back to normal
        fighter.removeCondition(shaken);
        assertEquals(4, fighter.getSave(StatType.FORTITUDE));
    }

    @Test
    void testEquipArmorCapsMaxDexterityBonus() {
        // Fighter starts with DEX 12 (+1), so getArmorClass is 11 (10 base + 1 dex)
        assertEquals(11, fighter.getArmorClass());

        // 1. Equip Leather Armor (Max Dex 6, Armor bonus +2)
        Armor leather = new Armor(
            "leather-1",
            "Leather Armor",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(2, ModifierType.ARMOR, new ModifierSource("leather-1", "Leather Armor", SourceType.ITEM)))),
            6
        );
        fighter.equipArmor(leather);
        // Dex mod is 1. Min Max Dex is 6. Mod is not capped because 1 <= 6.
        // AC = 10 base + 1 dex + 2 armor = 13
        assertEquals(13, fighter.getArmorClass());

        // 2. Equip heavy plate (Max Dex 1, Armor bonus +8)
        Armor fullPlate = new Armor(
            "plate-1",
            "Full Plate",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(8, ModifierType.ARMOR, new ModifierSource("plate-1", "Full Plate", SourceType.ITEM)))),
            1
        );
        fighter.equipArmor(fullPlate);
        // Equipped: leather (max dex 6), full plate (max dex 1). Min Max Dex is 1.
        // Dex mod is 1. Mod is not capped because 1 <= 1.
        // Armor modifiers do not stack, so only the +8 from Full Plate is applied.
        // AC = 10 base + 1 dex + 8 armor = 19
        assertEquals(19, fighter.getArmorClass());

        // 3. Equip dynamic Dexterity belt to boost DEX to 18 (+4)
        EquippableItem belt = new EquippableItem(
            "belt-dex",
            "Belt of Incredible Dexterity +6",
            Map.of(StatType.DEXTERITY, List.of(new Modifier(6, ModifierType.ENHANCEMENT, new ModifierSource("belt-dex", "Belt of Incredible Dexterity +6", SourceType.ITEM))))
        );
        fighter.equip(belt);
        // Dex mod is now +4.
        // With full plate, Min Max Dex is 1. Mod (+4) is capped to 1.
        // Armor modifiers do not stack, so only the +8 from Full Plate is applied.
        // AC = 10 base + 1 dex (capped) + 8 armor = 19
        assertEquals(19, fighter.getArmorClass());

        // 4. Unequip full plate
        fighter.unequipArmor(fullPlate);
        // Only Leather remains. Max Dex is 6. Dex mod +4 is not capped.
        // AC = 10 base + 4 dex + 2 armor = 16
        assertEquals(16, fighter.getArmorClass());

        // 5. Unequip leather
        fighter.unequipArmor(leather);
        // No armor remains. Dex mod +4 is not capped.
        // AC = 10 base + 4 dex = 14
        assertEquals(14, fighter.getArmorClass());
    }
}
