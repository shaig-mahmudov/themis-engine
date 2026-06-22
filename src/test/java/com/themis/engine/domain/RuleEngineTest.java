package com.themis.engine.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.random.RandomGenerator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;
    private Character attacker;
    private Character target;
    private Weapon longsword;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
        
        // Attacker: Level 1, Str 14 (+2), Dex 10 (+0), Base attack bonus +1. Total attack bonus = +3.
        attacker = new Character("attacker", "Attacker", 1, 14, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);

        // Target: Level 1, AC 15 (base 10 + dex + shield)
        target = new Character("target", "Target", 1, 10, 10, 10, 10, 10, 10, 15, 0, 0, 0, 0);
        target.equip(new EquippableItem("shield", "Shield", 
            Map.of(StatType.ARMOR_CLASS, java.util.List.of(new Modifier(5, ModifierType.SHIELD, new ModifierSource("shield", "Shield", SourceType.ITEM))))));

        // Longsword: 1d8 damage, Threat 19-20, x2 multiplier
        longsword = new Weapon("longsword", "Longsword", WeaponType.MELEE, Map.of(), DiceRoll.parse("1d8"), 19, 2);
    }

    @Test
    void testNormalMiss() {
        var mockRng = mock(RandomGenerator.class);
        // Roll d20 = 10. Total attack = 10 + 1 (BAB) + 2 (Str) = 13. Target AC is 15. -> Miss.
        var result = ruleEngine.resolveAttack(attacker, longsword, target, 10, mockRng);

        assertFalse(result.isHit());
        assertFalse(result.isCritical());
        assertEquals(13, result.attackRoll());
        assertEquals(0, result.damageDealt());
        assertEquals(15, target.getCurrentHitPoints()); // No damage dealt
    }

    @Test
    void testNormalHit() {
        var mockRng = mock(RandomGenerator.class);
        // Roll d20 = 12. Total attack = 12 + 3 = 15. Target AC = 15. -> Hit.
        // Damage roll: 1d8 rolls a 5. Strength mod = +2. Total damage = 7.
        when(mockRng.nextInt(1, 9)).thenReturn(5);

        var result = ruleEngine.resolveAttack(attacker, longsword, target, 12, mockRng);

        assertTrue(result.isHit());
        assertFalse(result.isCritical());
        assertEquals(15, result.attackRoll());
        assertEquals(7, result.damageDealt());
        assertEquals(8, target.getCurrentHitPoints()); // 15 - 7 = 8
    }

    @Test
    void testNatural1AutomaticMiss() {
        var mockRng = mock(RandomGenerator.class);
        // Target AC is 10. Natural 1 always misses even if BAB+Str = +3.
        target = new Character("target", "Target", 1, 10, 10, 10, 10, 10, 10, 10, 0, 0, 0, 0);

        var result = ruleEngine.resolveAttack(attacker, longsword, target, 1, mockRng);
        assertFalse(result.isHit());
        assertEquals(0, result.damageDealt());
    }

    @Test
    void testNatural20AutomaticHit() {
        var mockRng = mock(RandomGenerator.class);
        // Target AC is 100. Natural 20 always hits and threatens critical.
        target = new Character("target", "Target", 1, 10, 10, 10, 10, 10, 10, 10, 0, 0, 0, 0);
        var superAcItem = new EquippableItem("cloak", "Cloak of Invincibility", 
            Map.of(StatType.ARMOR_CLASS, java.util.List.of(new Modifier(90, ModifierType.UNTYPED, new ModifierSource("cloak", "Cloak of Invincibility", SourceType.ITEM)))));
        target.equip(superAcItem);
        assertEquals(100, target.getArmorClass());

        // Confirmation roll: rolls 5. Confirm attack = 5 + 3 = 8. Misses AC 100 -> Critical fails, normal hit!
        // Damage roll rolls 4. Str +2. Damage = 6.
        when(mockRng.nextInt(1, 21)).thenReturn(5); // confirm d20
        when(mockRng.nextInt(1, 9)).thenReturn(4); // damage roll

        var result = ruleEngine.resolveAttack(attacker, longsword, target, 20, mockRng);

        assertTrue(result.isHit());
        assertFalse(result.isCritical());
        assertEquals(6, result.damageDealt());
    }

    @Test
    void testCriticalThreatConfirmed() {
        var mockRng = mock(RandomGenerator.class);
        // Roll d20 = 19. Total attack = 19 + 3 = 22 >= 15 (Hit + Critical Threat!).
        // Confirmation roll d20 = 15. Total confirm = 15 + 3 = 18 >= 15 (Confirmed!).
        // Damage rolls: roll 1 (rolls 4), roll 2 (rolls 6). Str mod (+2) added to both.
        // Total damage = (4 + 2) + (6 + 2) = 14.
        when(mockRng.nextInt(1, 21)).thenReturn(15); // confirm d20
        when(mockRng.nextInt(1, 9)).thenReturn(4, 6); // damage dice rolls

        var result = ruleEngine.resolveAttack(attacker, longsword, target, 19, mockRng);

        assertTrue(result.isHit());
        assertTrue(result.isCritical());
        assertEquals(22, result.attackRoll());
        assertEquals(18, result.confirmationRoll());
        assertEquals(14, result.damageDealt());
        assertEquals(1, target.getCurrentHitPoints()); // 15 - 14 = 1
    }

    @Test
    void testCriticalThreatNotConfirmed() {
        var mockRng = mock(RandomGenerator.class);
        // Roll d20 = 19. Total attack = 22 >= 15 (Hit + Threat).
        // Confirmation roll d20 = 5. Total confirm = 5 + 3 = 8 < 15 (Not Confirmed!).
        // Damage roll: rolls 5. Str mod (+2) added once. Total damage = 7.
        when(mockRng.nextInt(1, 21)).thenReturn(5); // confirm d20
        when(mockRng.nextInt(1, 9)).thenReturn(5); // damage die roll

        var result = ruleEngine.resolveAttack(attacker, longsword, target, 19, mockRng);

        assertTrue(result.isHit());
        assertFalse(result.isCritical());
        assertEquals(7, result.damageDealt());
    }

    @Test
    void testRangedWeaponUsesDexterityForAttackAndNoStrForDamage() {
        // Attacker: Level 1, Str 14 (+2), Dex 16 (+3), BAB +1.
        attacker = new Character("attacker", "Attacker", 1, 14, 16, 10, 10, 10, 10, 10, 1, 0, 0, 0);

        // Shortbow: 1d6 damage, Threat 20, x3 multiplier, Ranged type
        Weapon shortbow = new Weapon("shortbow", "Shortbow", WeaponType.RANGED, Map.of(), DiceRoll.parse("1d6"), 20, 3);
        attacker.equipWeapon(shortbow);

        var mockRng = mock(RandomGenerator.class);
        when(mockRng.nextInt(1, 7)).thenReturn(4); // damage roll

        // Roll d20 = 11. Total attack = 11 + 1 (BAB) + 3 (Dex) = 15. Target AC = 15. -> Hit.
        // Damage: 1d6 rolls 4 + 0 (ranged doesn't add STR modifier to damage) = 4.
        var result = ruleEngine.resolveAttack(attacker, shortbow, target, 11, mockRng);

        assertTrue(result.isHit());
        assertEquals(15, result.attackRoll());
        assertEquals(4, result.damageDealt());
        assertEquals(11, target.getCurrentHitPoints()); // 15 - 4 = 11
    }

    @Test
    void testFinesseWeaponUsesDexterityForAttackAndStrForDamage() {
        // Attacker: Level 1, Str 14 (+2), Dex 16 (+3), BAB +1.
        attacker = new Character("attacker", "Attacker", 1, 14, 16, 10, 10, 10, 10, 10, 1, 0, 0, 0);

        // Rapier: 1d6 damage, Threat 18-20, x2 multiplier, Finesse type
        Weapon rapier = new Weapon("rapier", "Rapier", WeaponType.FINESSE, Map.of(), DiceRoll.parse("1d6"), 18, 2);
        attacker.equipWeapon(rapier);

        var mockRng = mock(RandomGenerator.class);
        when(mockRng.nextInt(1, 7)).thenReturn(3); // damage roll

        // Roll d20 = 11. Total attack = 11 + 1 (BAB) + 3 (Dex) = 15. Target AC = 15. -> Hit.
        // Damage: 1d6 rolls 3 + 2 (STR modifier) = 5.
        var result = ruleEngine.resolveAttack(attacker, rapier, target, 11, mockRng);

        assertTrue(result.isHit());
        assertEquals(15, result.attackRoll());
        assertEquals(5, result.damageDealt());
        assertEquals(10, target.getCurrentHitPoints()); // 15 - 5 = 10
    }
}
