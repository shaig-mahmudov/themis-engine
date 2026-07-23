package com.themis.engine.api.character;

import com.themis.engine.api.character.request.ApplyConditionRequest;
import com.themis.engine.api.character.request.CreateCharacterRequest;
import com.themis.engine.api.character.request.EquipArmorRequest;
import com.themis.engine.api.character.request.EquipItemRequest;
import com.themis.engine.api.character.request.EquipWeaponRequest;
import com.themis.engine.api.character.response.CharacterResponse;
import com.themis.engine.domain.Armor;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.Condition;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.Modifier;
import com.themis.engine.domain.ModifierSource;
import com.themis.engine.domain.ModifierType;
import com.themis.engine.domain.SourceType;
import com.themis.engine.domain.SpellcastingFeature;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Weapon;
import com.themis.engine.domain.WeaponType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CharacterApiMapperTest {

    private CharacterApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CharacterApiMapper();
    }

    @Test
    void testToDomain_CreateCharacterRequest() {
        CreateCharacterRequest request = new CreateCharacterRequest(
            "c1", "Valeros", 3,
            16, 12, 14, 10, 10, 10,
            24, 3, 3, 1, 1
        );

        Character domain = mapper.toDomain(request);

        assertNotNull(domain);
        assertEquals("c1", domain.getId());
        assertEquals("Valeros", domain.getName());
        assertEquals(3, domain.getLevel());
        assertEquals(16, domain.getBaseAttribute(StatType.STRENGTH));
        assertEquals(24, domain.getBaseHitPoints());
    }

    @Test
    void testToDomain_EquipItemRequest() {
        EquipItemRequest request = new EquipItemRequest(
            "item-1", "Ring", Map.of()
        );

        EquippableItem item = mapper.toDomain(request);

        assertNotNull(item);
        assertEquals("item-1", item.id());
        assertEquals("Ring", item.name());
    }

    @Test
    void testToDomain_EquipWeaponRequest() {
        EquipWeaponRequest request = new EquipWeaponRequest(
            "w1", "Longsword", WeaponType.MELEE, Map.of(), "1d8", 19, 2
        );

        Weapon weapon = mapper.toDomain(request);

        assertNotNull(weapon);
        assertEquals("w1", weapon.id());
        assertEquals("Longsword", weapon.name());
        assertEquals(WeaponType.MELEE, weapon.type());
        assertEquals(19, weapon.criticalThreatMin());
        assertEquals(2, weapon.criticalMultiplier());
    }

    @Test
    void testToDomain_EquipArmorRequest() {
        EquipArmorRequest request = new EquipArmorRequest(
            "a1", "Plate", Map.of(), 1
        );

        Armor armor = mapper.toDomain(request);

        assertNotNull(armor);
        assertEquals("a1", armor.id());
        assertEquals("Plate", armor.name());
        assertEquals(1, armor.maxDexterityBonus());
    }

    @Test
    void testToDomain_ApplyConditionRequest() {
        ApplyConditionRequest request = new ApplyConditionRequest(
            "cond-1", "Haste", Map.of(), 5, "speed"
        );

        Condition condition = mapper.toDomain(request);

        assertNotNull(condition);
        assertEquals("cond-1", condition.id());
        assertEquals("Haste", condition.name());
        assertEquals(5, condition.durationRounds());
        assertEquals("speed", condition.stackingGroup());
    }

    @Test
    void testToResponse_Character() {
        Character c = new Character(
            "c1", "Ezren", 1,
            8, 12, 12, 16, 13, 10,
            6, 0, 0, 0, 2
        );

        SpellcastingFeature sf = new SpellcastingFeature(1, StatType.INTELLIGENCE);
        sf.setMaxSlots(1, 2);
        sf.setRemainingSlots(1, 2);
        c.setSpellcastingFeature(sf);

        CharacterResponse response = mapper.toResponse(c);

        assertNotNull(response);
        assertEquals("c1", response.id());
        assertEquals("Ezren", response.name());
        assertEquals(1, response.level());
        assertNull(response.version());
        assertEquals(16, response.attributes().get("INTELLIGENCE").score());
        assertEquals(3, response.attributes().get("INTELLIGENCE").modifier());
        assertNotNull(response.spellcasting());
        assertEquals("INTELLIGENCE", response.spellcasting().castingAttribute());
        assertEquals(2, response.spellcasting().maxSlots()[1]);
    }
}
