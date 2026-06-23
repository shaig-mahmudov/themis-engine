package com.themis.engine.infrastructure;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PostgresCharacterRepositoryAdapterTest {

    @Autowired
    private CharacterStore characterStore;

    @Test
    void testSaveAndLoadCharacter_PreservesAllDomainState() {
        // 1. Create a character
        Character original = new Character(
            "hero-456",
            "Seoni",
            5,   // Level
            10,  // STR
            14,  // DEX
            12,  // CON
            18,  // INT
            12,  // WIS
            10,  // CHA
            30,  // Base HP
            2,   // Base Attack Bonus
            1,   // Base Fortitude
            1,   // Base Reflex
            4    // Base Will
        );

        // 2. Equip some items
        EquippableItem intelligenceHeadband = new EquippableItem(
            "headband-vast-int",
            "Headband of Vast Intelligence +2",
            Map.of(StatType.INTELLIGENCE, List.of(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("headband-vast-int", "Headband of Vast Intelligence +2", SourceType.ITEM))))
        );
        original.equip(intelligenceHeadband);

        EquippableItem cloak = new EquippableItem(
            "cloak-res-2",
            "Cloak of Resistance +2",
            Map.of(
                StatType.FORTITUDE, List.of(new Modifier(2, ModifierType.RESISTANCE, new ModifierSource("cloak-res-2", "Cloak of Resistance +2", SourceType.ITEM))),
                StatType.REFLEX, List.of(new Modifier(2, ModifierType.RESISTANCE, new ModifierSource("cloak-res-2", "Cloak of Resistance +2", SourceType.ITEM))),
                StatType.WILL, List.of(new Modifier(2, ModifierType.RESISTANCE, new ModifierSource("cloak-res-2", "Cloak of Resistance +2", SourceType.ITEM)))
            )
        );
        original.equip(cloak);

        // 3. Apply a condition
        Condition fatigued = new Condition(
            "fatigued-cond",
            "Fatigued",
            Map.of(
                StatType.STRENGTH, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("fatigued-cond", "Fatigued", SourceType.CONDITION))),
                StatType.DEXTERITY, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("fatigued-cond", "Fatigued", SourceType.CONDITION)))
            )
        );
        original.applyCondition(fatigued);

        // Equip a weapon (Masterwork Dagger: +1 Enhancement to BAB, 1d4 damage, Threat 19, Mult 2)
        Weapon masterworkDagger = new Weapon(
            "mwk-dagger",
            "Masterwork Dagger",
            WeaponType.MELEE,
            Map.of(StatType.BASE_ATTACK_BONUS, List.of(new Modifier(1, ModifierType.ENHANCEMENT, new ModifierSource("mwk-dagger", "Masterwork", SourceType.ITEM)))),
            DiceRoll.parse("1d4"),
            19,
            2
        );
        original.equipWeapon(masterworkDagger);

        // Equip an armor (Mithral Shirt: +4 Armor, max dex 6)
        Armor mithralShirt = new Armor(
            "mithral-shirt",
            "Mithral Shirt",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(4, ModifierType.ARMOR, new ModifierSource("mithral-shirt", "Mithral Shirt", SourceType.ITEM)))),
            6
        );
        original.equipArmor(mithralShirt);

        // 4. Configure spellcasting
        SpellcastingFeature sf = new SpellcastingFeature(5, StatType.INTELLIGENCE);
        sf.setMaxSlots(1, 4);
        sf.setMaxSlots(2, 3);
        sf.restAndRecover();
        sf.consumeSlot(2); // Expend one Level 2 spell slot
        original.setSpellcastingFeature(sf);

        // 5. Apply damage (CON is 12, Level is 5. Max HP = 30 + (1 * 5) = 35. Let's do 10 damage)
        original.damage(10);

        // Verify initial calculations
        // AC: 10 base + 1 dex (fatigued) + 4 armor = 15
        assertEquals(15, original.getArmorClass());
        // INT: Base 18 + Headband 2 = 20 (+5 mod)
        assertEquals(20, original.getAttributeScore(StatType.INTELLIGENCE));
        // Save DC for level 1 spell: 10 + 1 + 5 = 16
        assertEquals(16, original.getSpellSaveDC(1));
        // STR: Base 10 + Fatigued -2 = 8
        assertEquals(8, original.getAttributeScore(StatType.STRENGTH));
        // BAB: Base 2 + Weapon Enhancement 1 = 3
        assertEquals(3, original.getBaseAttackBonus());
        // Max HP: 30 + (Level 5 * CON Mod 1) = 35. Current HP: 35 - 10 = 25
        assertEquals(35, original.getMaxHitPoints());
        assertEquals(25, original.getCurrentHitPoints());
        assertEquals(2, original.getSpellcastingFeature().getRemainingSlots(2));

        // 6. Persist to DB via adapter
        characterStore.save(original);

        // 7. Load from DB
        Optional<Character> loadedOpt = characterStore.findById("hero-456");
        assertTrue(loadedOpt.isPresent());
        Character loaded = loadedOpt.get();

        // 8. Assert all fields and calculations match perfectly
        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getLevel(), loaded.getLevel());

        // Attributes (buffed and debuffed)
        assertEquals(20, loaded.getAttributeScore(StatType.INTELLIGENCE));
        assertEquals(8, loaded.getAttributeScore(StatType.STRENGTH));
        assertEquals(12, loaded.getAttributeScore(StatType.DEXTERITY)); // Dex is 14 - 2 from fatigue = 12

        // Saving throws: Fortitude = Base 1 + Con Mod 1 + Cloak 2 = 4
        assertEquals(4, loaded.getSave(StatType.FORTITUDE));
        // Reflex = Base 1 + Dex Mod 1 (12 Dex is +1) + Cloak 2 = 4
        assertEquals(4, loaded.getSave(StatType.REFLEX));

        // Damage and Hit points
        assertEquals(35, loaded.getMaxHitPoints());
        assertEquals(25, loaded.getCurrentHitPoints());
        assertEquals(10, loaded.getCurrentDamage());

        // Spellcasting Feature
        assertNotNull(loaded.getSpellcastingFeature());
        assertEquals(5, loaded.getSpellcastingFeature().getCasterLevel());
        assertEquals(StatType.INTELLIGENCE, loaded.getSpellcastingFeature().getCastingAttribute());
        assertEquals(16, loaded.getSpellSaveDC(1));
        assertEquals(4, loaded.getSpellcastingFeature().getRemainingSlots(1));
        assertEquals(2, loaded.getSpellcastingFeature().getRemainingSlots(2));

        // Active inventory / condition lists
        assertEquals(2, loaded.getEquippedItems().size());
        assertEquals(1, loaded.getActiveConditions().size());
        assertEquals(1, loaded.getEquippedWeapons().size());
        assertEquals(1, loaded.getEquippedArmors().size());

        Weapon loadedWeapon = loaded.getEquippedWeapons().get(0);
        assertEquals("mwk-dagger", loadedWeapon.id());
        assertEquals("Masterwork Dagger", loadedWeapon.name());
        assertEquals(19, loadedWeapon.criticalThreatMin());
        assertEquals(2, loadedWeapon.criticalMultiplier());
        assertEquals("1d4", loadedWeapon.damageRoll().toString());
        // Verify weapon enhancement modifier on loaded BAB
        assertEquals(3, loaded.getBaseAttackBonus());

        Armor loadedArmor = loaded.getEquippedArmors().get(0);
        assertEquals("mithral-shirt", loadedArmor.id());
        assertEquals("Mithral Shirt", loadedArmor.name());
        assertEquals(6, loadedArmor.maxDexterityBonus());
        // Verify armor + dex cap on loaded AC
        assertEquals(15, loaded.getArmorClass());
    }
}
