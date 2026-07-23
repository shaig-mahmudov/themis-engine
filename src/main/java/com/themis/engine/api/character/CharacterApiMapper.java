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
import com.themis.engine.domain.DiceRoll;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Weapon;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Component responsible for mapping between Character API contracts and Domain aggregates.
 */
@Component
public class CharacterApiMapper {

    public Character toDomain(CreateCharacterRequest request) {
        Objects.requireNonNull(request, "Create character request cannot be null");
        return new Character(
            request.id(),
            request.name(),
            request.level(),
            request.baseStr(),
            request.baseDex(),
            request.baseCon(),
            request.baseInt(),
            request.baseWis(),
            request.baseCha(),
            request.baseHitPoints(),
            request.baseAttackBonus(),
            request.baseFortitude(),
            request.baseReflex(),
            request.baseWill()
        );
    }

    public EquippableItem toDomain(EquipItemRequest request) {
        Objects.requireNonNull(request, "Equip item request cannot be null");
        return new EquippableItem(
            request.id(),
            request.name(),
            request.modifiers()
        );
    }

    public Weapon toDomain(EquipWeaponRequest request) {
        Objects.requireNonNull(request, "Equip weapon request cannot be null");
        return new Weapon(
            request.id(),
            request.name(),
            request.type(),
            request.modifiers(),
            DiceRoll.parse(request.damageRoll()),
            request.criticalThreatMin(),
            request.criticalMultiplier()
        );
    }

    public Armor toDomain(EquipArmorRequest request) {
        Objects.requireNonNull(request, "Equip armor request cannot be null");
        return new Armor(
            request.id(),
            request.name(),
            request.modifiers(),
            request.maxDexterityBonus()
        );
    }

    public Condition toDomain(ApplyConditionRequest request) {
        Objects.requireNonNull(request, "Apply condition request cannot be null");
        return new Condition(
            request.id(),
            request.name(),
            request.modifiers(),
            request.durationRounds(),
            request.stackingGroup()
        );
    }

    public CharacterResponse toResponse(Character c) {
        Objects.requireNonNull(c, "Character cannot be null");

        Map<String, CharacterResponse.AttributeDto> attributesMap = Map.of(
            "STRENGTH", new CharacterResponse.AttributeDto(c.getAttributeScore(StatType.STRENGTH), c.getAttributeModifier(StatType.STRENGTH)),
            "DEXTERITY", new CharacterResponse.AttributeDto(c.getAttributeScore(StatType.DEXTERITY), c.getAttributeModifier(StatType.DEXTERITY)),
            "CONSTITUTION", new CharacterResponse.AttributeDto(c.getAttributeScore(StatType.CONSTITUTION), c.getAttributeModifier(StatType.CONSTITUTION)),
            "INTELLIGENCE", new CharacterResponse.AttributeDto(c.getAttributeScore(StatType.INTELLIGENCE), c.getAttributeModifier(StatType.INTELLIGENCE)),
            "WISDOM", new CharacterResponse.AttributeDto(c.getAttributeScore(StatType.WISDOM), c.getAttributeModifier(StatType.WISDOM)),
            "CHARISMA", new CharacterResponse.AttributeDto(c.getAttributeScore(StatType.CHARISMA), c.getAttributeModifier(StatType.CHARISMA))
        );

        List<CharacterResponse.EquippedItemDto> items = c.getEquippedItems().stream()
            .map(item -> new CharacterResponse.EquippedItemDto(item.id(), item.name(), item.modifiers()))
            .toList();

        List<CharacterResponse.EquippedWeaponDto> weapons = c.getEquippedWeapons().stream()
            .map(w -> new CharacterResponse.EquippedWeaponDto(
                w.id(),
                w.name(),
                w.modifiers(),
                w.damageRoll().toString(),
                w.criticalThreatMin(),
                w.criticalMultiplier()
            ))
            .toList();

        List<CharacterResponse.EquippedArmorDto> armors = c.getEquippedArmors().stream()
            .map(a -> new CharacterResponse.EquippedArmorDto(
                a.id(),
                a.name(),
                a.modifiers(),
                a.maxDexterityBonus()
            ))
            .toList();

        List<CharacterResponse.ActiveConditionDto> conditions = c.getActiveConditions().stream()
            .map(cond -> new CharacterResponse.ActiveConditionDto(
                cond.id(),
                cond.name(),
                cond.modifiers(),
                cond.durationRounds(),
                cond.stackingGroup()
            ))
            .toList();

        CharacterResponse.SpellcastingDto spellcastingDto = null;
        if (c.getSpellcastingFeature() != null) {
            var sf = c.getSpellcastingFeature();
            int[] maxSlots = new int[10];
            int[] remainingSlots = new int[10];
            int[] spellSaveDcs = new int[10];
            for (int i = 0; i <= 9; i++) {
                maxSlots[i] = sf.getMaxSlots(i);
                remainingSlots[i] = sf.getRemainingSlots(i);
                spellSaveDcs[i] = c.getSpellSaveDC(i);
            }
            spellcastingDto = new CharacterResponse.SpellcastingDto(sf.getCasterLevel(), sf.getCastingAttribute().name(), maxSlots, remainingSlots, spellSaveDcs);
        }

        CharacterResponse.TurnStateDto turnStateDto = new CharacterResponse.TurnStateDto(
            c.getTurnState().isStandardUsed(),
            c.getTurnState().isMoveUsed(),
            c.getTurnState().isSwiftUsed()
        );

        return new CharacterResponse(
            c.getId(),
            c.getName(),
            c.getLevel(),
            c.getVersion(),
            attributesMap,
            c.getArmorClass(),
            c.getBaseAttackBonus(),
            c.getMaxHitPoints(),
            c.getCurrentHitPoints(),
            c.getCurrentDamage(),
            c.getSave(StatType.FORTITUDE),
            c.getSave(StatType.REFLEX),
            c.getSave(StatType.WILL),
            items,
            weapons,
            armors,
            conditions,
            spellcastingDto,
            turnStateDto,
            c.isConscious(),
            c.isDead()
        );
    }
}
