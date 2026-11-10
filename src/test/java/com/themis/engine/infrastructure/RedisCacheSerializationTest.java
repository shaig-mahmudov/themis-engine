package com.themis.engine.infrastructure;

import com.themis.engine.domain.ActionType;
import com.themis.engine.domain.Armor;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CombatantType;
import com.themis.engine.domain.Condition;
import com.themis.engine.domain.DiceRoll;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import com.themis.engine.domain.EncounterStatus;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.Modifier;
import com.themis.engine.domain.ModifierSource;
import com.themis.engine.domain.ModifierType;
import com.themis.engine.domain.SourceType;
import com.themis.engine.domain.SpellcastingFeature;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Weapon;
import com.themis.engine.domain.WeaponType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisCacheSerializationTest {

    private final CharacterCacheSerializer characterSerializer = new CharacterCacheSerializer();
    private final EncounterCacheSerializer encounterSerializer = new EncounterCacheSerializer();

    @Test
    void characterSerializerRoundTripsComplexCharacterState() {
        Character original = new Character(
            "char-redis-1",
            "Ezren",
            7,
            10,
            14,
            12,
            20,
            13,
            11,
            38,
            3,
            2,
            2,
            5
        );
        original.restoreVersion(4L);

        EquippableItem headband = new EquippableItem(
            "headband-int-2",
            "Headband of Vast Intelligence +2",
            Map.of(
                StatType.INTELLIGENCE,
                List.of(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("headband-int-2", "Headband", SourceType.ITEM)))
            )
        );
        original.equip(headband);

        Weapon quarterstaff = new Weapon(
            "mwk-quarterstaff",
            "Masterwork Quarterstaff",
            WeaponType.MELEE,
            Map.of(
                StatType.BASE_ATTACK_BONUS,
                List.of(new Modifier(1, ModifierType.ENHANCEMENT, new ModifierSource("mwk-quarterstaff", "Quarterstaff", SourceType.ITEM)))
            ),
            DiceRoll.parse("1d6"),
            20,
            2
        );
        original.equipWeapon(quarterstaff);

        Armor haramaki = new Armor(
            "haramaki-1",
            "+1 Haramaki",
            Map.of(
                StatType.ARMOR_CLASS,
                List.of(new Modifier(2, ModifierType.ARMOR, new ModifierSource("haramaki-1", "Haramaki", SourceType.ITEM)))
            ),
            null
        );
        original.equipArmor(haramaki);

        Condition shaken = new Condition(
            "shaken-1",
            "Shaken",
            Map.of(
                StatType.WILL,
                List.of(new Modifier(-2, ModifierType.MORALE, new ModifierSource("shaken-1", "Shaken", SourceType.CONDITION)))
            ),
            3,
            "fear"
        );
        original.applyCondition(shaken);

        SpellcastingFeature spellcasting = new SpellcastingFeature(7, StatType.INTELLIGENCE);
        spellcasting.setMaxSlots(0, 4);
        spellcasting.setMaxSlots(1, 5);
        spellcasting.setMaxSlots(2, 4);
        spellcasting.setMaxSlots(3, 3);
        spellcasting.setMaxSlots(4, 1);
        spellcasting.restAndRecover();
        spellcasting.consumeSlot(3);
        original.setSpellcastingFeature(spellcasting);

        original.damage(12);
        original.getTurnState().consume(ActionType.STANDARD);
        original.getTurnState().consume(ActionType.SWIFT);

        byte[] bytes = characterSerializer.serialize(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        Character restored = characterSerializer.deserialize(bytes);
        assertNotNull(restored);

        assertEquals(original.getId(), restored.getId());
        assertEquals(4L, restored.getVersion());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getLevel(), restored.getLevel());

        assertEquals(original.getAttributeScore(StatType.INTELLIGENCE), restored.getAttributeScore(StatType.INTELLIGENCE));
        assertEquals(original.getArmorClass(), restored.getArmorClass());
        assertEquals(original.getMaxHitPoints(), restored.getMaxHitPoints());
        assertEquals(original.getCurrentHitPoints(), restored.getCurrentHitPoints());
        assertEquals(original.getCurrentDamage(), restored.getCurrentDamage());
        assertEquals(original.getBaseAttackBonus(), restored.getBaseAttackBonus());
        assertEquals(original.getSave(StatType.WILL), restored.getSave(StatType.WILL));
        assertEquals(original.getSpellSaveDC(3), restored.getSpellSaveDC(3));

        assertTrue(restored.getTurnState().isStandardUsed());
        assertFalse(restored.getTurnState().isMoveUsed());
        assertTrue(restored.getTurnState().isSwiftUsed());

        assertNotNull(restored.getSpellcastingFeature());
        assertEquals(2, restored.getSpellcastingFeature().getRemainingSlots(3));
        assertEquals(3, restored.getSpellcastingFeature().getMaxSlots(3));

        assertEquals(1, restored.getEquippedItems().size());
        assertEquals(1, restored.getEquippedWeapons().size());
        assertEquals(1, restored.getEquippedArmors().size());
        assertEquals(1, restored.getActiveConditions().size());
        assertEquals(3, restored.getActiveConditions().get(0).durationRounds());
        assertEquals("fear", restored.getActiveConditions().get(0).stackingGroup());
    }

    @Test
    void encounterSerializerRoundTripsEncounterState() {
        Encounter original = new Encounter(
            "enc-redis-1",
            "Goblin Ambush",
            EncounterStatus.ACTIVE,
            3,
            1,
            List.of(
                new EncounterParticipant("hero-1", CombatantType.CHARACTER, "Valeros", 15, 17, 2),
                new EncounterParticipant("goblin-1", CombatantType.LAIR_ACTION, "Goblin Trap", 12, 15, 3)
            )
        );
        original.restoreVersion(2L);

        byte[] bytes = encounterSerializer.serialize(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        Encounter restored = encounterSerializer.deserialize(bytes);
        assertNotNull(restored);

        assertEquals("enc-redis-1", restored.getId());
        assertEquals(2L, restored.getVersion());
        assertEquals("Goblin Ambush", restored.getName());
        assertEquals(EncounterStatus.ACTIVE, restored.getStatus());
        assertEquals(3, restored.getCurrentRound());
        assertEquals(1, restored.getActiveParticipantIndex());
        assertEquals(2, restored.getParticipants().size());
        assertEquals("goblin-1", restored.getActiveParticipant().orElseThrow().combatantId());
    }

    @Test
    void serializersHandleNullAndEmptyBytesGracefully() {
        assertEquals(0, characterSerializer.serialize(null).length);
        assertNull(characterSerializer.deserialize(null));
        assertNull(characterSerializer.deserialize(new byte[0]));

        assertEquals(0, encounterSerializer.serialize(null).length);
        assertNull(encounterSerializer.deserialize(null));
        assertNull(encounterSerializer.deserialize(new byte[0]));
    }

    @Test
    void redisConfigurationCreatesTransactionAwareCacheManager() {
        RedisConfiguration configuration = new RedisConfiguration();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = configuration.cacheManager(connectionFactory);

        assertNotNull(cacheManager);
        assertTrue(cacheManager.isTransactionAware());
        assertTrue(cacheManager.getCacheConfigurations().containsKey("characters"));
        assertTrue(cacheManager.getCacheConfigurations().containsKey("encounters"));
    }
}
