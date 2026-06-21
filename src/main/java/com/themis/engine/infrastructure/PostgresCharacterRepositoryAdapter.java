package com.themis.engine.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.*;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * Adapter that connects the domain CharacterStore port to the database via CharacterJpaRepository.
 */
@Component
public class PostgresCharacterRepositoryAdapter implements CharacterStore {

    private final CharacterJpaRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresCharacterRepositoryAdapter(CharacterJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    @CachePut(value = "characters", key = "#character.id")
    public Character save(Character character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }
        CharacterEntity entity = toEntity(character);
        CharacterEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "characters", key = "#id")
    public Optional<Character> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or blank");
        }
        return repository.findById(id).map(this::toDomain);
    }

    private CharacterEntity toEntity(Character domain) {
        CharacterEntity entity = new CharacterEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setLevel(domain.getLevel());
        
        entity.setBaseStr(domain.getBaseAttribute(StatType.STRENGTH));
        entity.setBaseDex(domain.getBaseAttribute(StatType.DEXTERITY));
        entity.setBaseCon(domain.getBaseAttribute(StatType.CONSTITUTION));
        entity.setBaseInt(domain.getBaseAttribute(StatType.INTELLIGENCE));
        entity.setBaseWis(domain.getBaseAttribute(StatType.WISDOM));
        entity.setBaseCha(domain.getBaseAttribute(StatType.CHARISMA));
        
        entity.setBaseHitPoints(domain.getBaseHitPoints());
        entity.setBaseAttackBonus(domain.getBaseAttackBonusRaw());
        entity.setBaseFortitude(domain.getBaseSave(StatType.FORTITUDE));
        entity.setBaseReflex(domain.getBaseSave(StatType.REFLEX));
        entity.setBaseWill(domain.getBaseSave(StatType.WILL));
        
        entity.setCurrentDamage(domain.getCurrentDamage());

        // Map spellcasting if exists
        if (domain.getSpellcastingFeature() != null) {
            SpellcastingFeature sf = domain.getSpellcastingFeature();
            entity.setSpellcastingCasterLevel(sf.getCasterLevel());
            entity.setSpellcastingAttribute(sf.getCastingAttribute().name());
            
            List<String> maxSlots = new ArrayList<>();
            List<String> remainingSlots = new ArrayList<>();
            for (int i = 0; i <= 9; i++) {
                maxSlots.add(String.valueOf(sf.getMaxSlots(i)));
                remainingSlots.add(String.valueOf(sf.getRemainingSlots(i)));
            }
            entity.setSpellcastingMaxSlots(String.join(",", maxSlots));
            entity.setSpellcastingRemainingSlots(String.join(",", remainingSlots));
        }

        // Map equipped items
        List<CharacterEquippedItemEntity> equippedList = new ArrayList<>();
        for (EquippableItem item : domain.getEquippedItems()) {
            try {
                String json = objectMapper.writeValueAsString(item.modifiers());
                equippedList.add(new CharacterEquippedItemEntity(domain.getId(), item.id(), item.name(), json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize item modifiers for: " + item.id(), e);
            }
        }
        entity.getEquippedItems().addAll(equippedList);

        // Map conditions
        List<CharacterActiveConditionEntity> conditionList = new ArrayList<>();
        for (Condition cond : domain.getActiveConditions()) {
            try {
                String json = objectMapper.writeValueAsString(cond.modifiers());
                conditionList.add(new CharacterActiveConditionEntity(domain.getId(), cond.id(), cond.name(), json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize condition modifiers for: " + cond.id(), e);
            }
        }
        entity.getActiveConditions().addAll(conditionList);

        // Map equipped weapons
        List<CharacterEquippedWeaponEntity> equippedWeaponsList = new ArrayList<>();
        for (Weapon weapon : domain.getEquippedWeapons()) {
            try {
                String json = objectMapper.writeValueAsString(weapon.modifiers());
                equippedWeaponsList.add(new CharacterEquippedWeaponEntity(
                    domain.getId(),
                    weapon.id(),
                    weapon.name(),
                    weapon.type().name(),
                    json,
                    weapon.damageRoll().toString(),
                    weapon.criticalThreatMin(),
                    weapon.criticalMultiplier()
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize weapon modifiers for: " + weapon.id(), e);
            }
        }
        entity.getEquippedWeapons().addAll(equippedWeaponsList);

        return entity;
    }

    private Character toDomain(CharacterEntity entity) {
        Character domain = new Character(
            entity.getId(),
            entity.getName(),
            entity.getLevel(),
            entity.getBaseStr(),
            entity.getBaseDex(),
            entity.getBaseCon(),
            entity.getBaseInt(),
            entity.getBaseWis(),
            entity.getBaseCha(),
            entity.getBaseHitPoints(),
            entity.getBaseAttackBonus(),
            entity.getBaseFortitude(),
            entity.getBaseReflex(),
            entity.getBaseWill()
        );

        JavaType modifiersMapType = objectMapper.getTypeFactory().constructType(
            new com.fasterxml.jackson.core.type.TypeReference<Map<StatType, List<Modifier>>>() {}
        );

        // Equip items back
        for (CharacterEquippedItemEntity itemEntity : entity.getEquippedItems()) {
            try {
                Map<StatType, List<Modifier>> modifiers = objectMapper.readValue(itemEntity.getModifiersJson(), modifiersMapType);
                EquippableItem item = new EquippableItem(itemEntity.getItemId(), itemEntity.getName(), modifiers);
                domain.equip(item);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize item modifiers for: " + itemEntity.getItemId(), e);
            }
        }

        // Apply conditions back
        for (CharacterActiveConditionEntity condEntity : entity.getActiveConditions()) {
            try {
                Map<StatType, List<Modifier>> modifiers = objectMapper.readValue(condEntity.getModifiersJson(), modifiersMapType);
                Condition cond = new Condition(condEntity.getConditionId(), condEntity.getName(), modifiers);
                domain.applyCondition(cond);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize condition modifiers for: " + condEntity.getConditionId(), e);
            }
        }

        // Equip weapons back
        for (CharacterEquippedWeaponEntity weaponEntity : entity.getEquippedWeapons()) {
            try {
                Map<StatType, List<Modifier>> modifiers = objectMapper.readValue(weaponEntity.getModifiersJson(), modifiersMapType);
                DiceRoll damageRoll = DiceRoll.parse(weaponEntity.getDamageRoll());
                WeaponType weaponType = weaponEntity.getType() == null ? WeaponType.MELEE : WeaponType.valueOf(weaponEntity.getType());
                Weapon weapon = new Weapon(
                    weaponEntity.getWeaponId(),
                    weaponEntity.getName(),
                    weaponType,
                    modifiers,
                    damageRoll,
                    weaponEntity.getCriticalThreatMin(),
                    weaponEntity.getCriticalMultiplier()
                );
                domain.equipWeapon(weapon);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize weapon modifiers for: " + weaponEntity.getWeaponId(), e);
            }
        }

        // Restore spellcasting feature
        if (entity.getSpellcastingCasterLevel() != null) {
            StatType castingAttribute = StatType.valueOf(entity.getSpellcastingAttribute());
            SpellcastingFeature sf = new SpellcastingFeature(entity.getSpellcastingCasterLevel(), castingAttribute);
            
            String[] maxStr = entity.getSpellcastingMaxSlots().split(",");
            String[] remStr = entity.getSpellcastingRemainingSlots().split(",");
            for (int i = 0; i <= 9; i++) {
                sf.setMaxSlots(i, Integer.parseInt(maxStr[i]));
                sf.setRemainingSlots(i, Integer.parseInt(remStr[i]));
            }
            domain.setSpellcastingFeature(sf);
        }

        // Apply damage after modifiers are set up to avoid incorrect clamping
        if (entity.getCurrentDamage() > 0) {
            domain.damage(entity.getCurrentDamage());
        }

        return domain;
    }
}
