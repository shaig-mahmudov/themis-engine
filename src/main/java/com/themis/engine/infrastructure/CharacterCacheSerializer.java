package com.themis.engine.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themis.engine.domain.Armor;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.Condition;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.Modifier;
import com.themis.engine.domain.SpellcastingFeature;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Weapon;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic Redis serializer for the {@link Character} domain aggregate.
 * Serializes canonical aggregate state via an immutable snapshot record so that
 * domain classes remain free of persistence/Jackson annotations and computed
 * getters are never mistaken for mutable fields.
 */
public class CharacterCacheSerializer implements RedisSerializer<Character> {

    private final ObjectMapper objectMapper;

    public CharacterCacheSerializer() {
        this(new ObjectMapper());
    }

    public CharacterCacheSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

    @Override
    public byte[] serialize(Character character) throws SerializationException {
        if (character == null) {
            return new byte[0];
        }
        try {
            CharacterCacheSnapshot snapshot = toSnapshot(character);
            return objectMapper.writeValueAsBytes(snapshot);
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize Character for cache: " + character.getId(), e);
        }
    }

    @Override
    public Character deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            CharacterCacheSnapshot snapshot = objectMapper.readValue(bytes, CharacterCacheSnapshot.class);
            return toDomain(snapshot);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize Character from cache", e);
        }
    }

    private static CharacterCacheSnapshot toSnapshot(Character domain) {
        SpellcastingSnapshot spellcastingSnapshot = null;
        if (domain.getSpellcastingFeature() != null) {
            SpellcastingFeature sf = domain.getSpellcastingFeature();
            List<Integer> maxSlots = new ArrayList<>(10);
            List<Integer> remainingSlots = new ArrayList<>(10);
            for (int i = 0; i <= 9; i++) {
                maxSlots.add(sf.getMaxSlots(i));
                remainingSlots.add(sf.getRemainingSlots(i));
            }
            spellcastingSnapshot = new SpellcastingSnapshot(
                sf.getCasterLevel(),
                sf.getCastingAttribute(),
                maxSlots,
                remainingSlots
            );
        }

        List<ConditionSnapshot> conditionSnapshots = domain.getActiveConditions().stream()
            .map(c -> new ConditionSnapshot(
                c.id(),
                c.name(),
                c.modifiers(),
                c.durationRounds(),
                c.stackingGroup()
            ))
            .toList();

        return new CharacterCacheSnapshot(
            domain.getId(),
            domain.getVersion(),
            domain.getName(),
            domain.getLevel(),
            domain.getBaseAttribute(StatType.STRENGTH),
            domain.getBaseAttribute(StatType.DEXTERITY),
            domain.getBaseAttribute(StatType.CONSTITUTION),
            domain.getBaseAttribute(StatType.INTELLIGENCE),
            domain.getBaseAttribute(StatType.WISDOM),
            domain.getBaseAttribute(StatType.CHARISMA),
            domain.getBaseHitPoints(),
            domain.getBaseAttackBonusRaw(),
            domain.getBaseSave(StatType.FORTITUDE),
            domain.getBaseSave(StatType.REFLEX),
            domain.getBaseSave(StatType.WILL),
            domain.getCurrentDamage(),
            domain.getTurnState().isStandardUsed(),
            domain.getTurnState().isMoveUsed(),
            domain.getTurnState().isSwiftUsed(),
            spellcastingSnapshot,
            domain.getEquippedItems(),
            conditionSnapshots,
            domain.getEquippedWeapons(),
            domain.getEquippedArmors()
        );
    }

    private static Character toDomain(CharacterCacheSnapshot snapshot) {
        Character domain = new Character(
            snapshot.id(),
            snapshot.name(),
            snapshot.level(),
            snapshot.baseStr(),
            snapshot.baseDex(),
            snapshot.baseCon(),
            snapshot.baseInt(),
            snapshot.baseWis(),
            snapshot.baseCha(),
            snapshot.baseHitPoints(),
            snapshot.baseAttackBonus(),
            snapshot.baseFortitude(),
            snapshot.baseReflex(),
            snapshot.baseWill()
        );

        if (snapshot.equippedItems() != null) {
            for (EquippableItem item : snapshot.equippedItems()) {
                domain.equip(item);
            }
        }

        if (snapshot.activeConditions() != null) {
            for (ConditionSnapshot cond : snapshot.activeConditions()) {
                domain.applyCondition(new Condition(
                    cond.id(),
                    cond.name(),
                    cond.modifiers(),
                    cond.durationRounds(),
                    cond.stackingGroup()
                ));
            }
        }

        if (snapshot.equippedWeapons() != null) {
            for (Weapon weapon : snapshot.equippedWeapons()) {
                domain.equipWeapon(weapon);
            }
        }

        if (snapshot.equippedArmors() != null) {
            for (Armor armor : snapshot.equippedArmors()) {
                domain.equipArmor(armor);
            }
        }

        if (snapshot.spellcasting() != null) {
            SpellcastingSnapshot sc = snapshot.spellcasting();
            SpellcastingFeature sf = new SpellcastingFeature(sc.casterLevel(), sc.castingAttribute());
            for (int i = 0; i <= 9; i++) {
                sf.setMaxSlots(i, sc.maxSlots().get(i));
                sf.setRemainingSlots(i, sc.remainingSlots().get(i));
            }
            domain.setSpellcastingFeature(sf);
        }

        if (snapshot.currentDamage() > 0) {
            domain.damage(snapshot.currentDamage());
        }

        domain.restoreVersion(snapshot.version());
        domain.getTurnState().restore(snapshot.standardUsed(), snapshot.moveUsed(), snapshot.swiftUsed());
        return domain;
    }

    record SpellcastingSnapshot(
        int casterLevel,
        StatType castingAttribute,
        List<Integer> maxSlots,
        List<Integer> remainingSlots
    ) {}

    record ConditionSnapshot(
        String id,
        String name,
        Map<StatType, List<Modifier>> modifiers,
        Integer durationRounds,
        String stackingGroup
    ) {}

    record CharacterCacheSnapshot(
        String id,
        Long version,
        String name,
        int level,
        int baseStr,
        int baseDex,
        int baseCon,
        int baseInt,
        int baseWis,
        int baseCha,
        int baseHitPoints,
        int baseAttackBonus,
        int baseFortitude,
        int baseReflex,
        int baseWill,
        int currentDamage,
        boolean standardUsed,
        boolean moveUsed,
        boolean swiftUsed,
        SpellcastingSnapshot spellcasting,
        List<EquippableItem> equippedItems,
        List<ConditionSnapshot> activeConditions,
        List<Weapon> equippedWeapons,
        List<Armor> equippedArmors
    ) {}
}
