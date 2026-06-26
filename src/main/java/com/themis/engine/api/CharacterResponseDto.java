package com.themis.engine.api;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.StatType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data Transfer Object representing the fully computed state of a Character.
 */
public record CharacterResponseDto(
    String id,
    String name,
    int level,
    Map<String, AttributeDto> attributes,
    int armorClass,
    int baseAttackBonus,
    int maxHitPoints,
    int currentHitPoints,
    int currentDamage,
    int fortitudeSave,
    int reflexSave,
    int willSave,
    List<EquippedItemDto> equippedItems,
    List<EquippedWeaponDto> equippedWeapons,
    List<EquippedArmorDto> equippedArmors,
    List<ActiveConditionDto> activeConditions,
    SpellcastingDto spellcasting,
    TurnStateDto turnState,
    boolean isConscious,
    boolean isDead
) {
    public static CharacterResponseDto fromDomain(Character c) {
        Map<String, AttributeDto> attributesMap = Map.of(
            "STRENGTH", new AttributeDto(c.getAttributeScore(StatType.STRENGTH), c.getAttributeModifier(StatType.STRENGTH)),
            "DEXTERITY", new AttributeDto(c.getAttributeScore(StatType.DEXTERITY), c.getAttributeModifier(StatType.DEXTERITY)),
            "CONSTITUTION", new AttributeDto(c.getAttributeScore(StatType.CONSTITUTION), c.getAttributeModifier(StatType.CONSTITUTION)),
            "INTELLIGENCE", new AttributeDto(c.getAttributeScore(StatType.INTELLIGENCE), c.getAttributeModifier(StatType.INTELLIGENCE)),
            "WISDOM", new AttributeDto(c.getAttributeScore(StatType.WISDOM), c.getAttributeModifier(StatType.WISDOM)),
            "CHARISMA", new AttributeDto(c.getAttributeScore(StatType.CHARISMA), c.getAttributeModifier(StatType.CHARISMA))
        );

        List<EquippedItemDto> items = c.getEquippedItems().stream()
            .map(item -> new EquippedItemDto(item.id(), item.name(), item.modifiers()))
            .collect(Collectors.toList());

        List<EquippedWeaponDto> weapons = c.getEquippedWeapons().stream()
            .map(w -> new EquippedWeaponDto(
                w.id(),
                w.name(),
                w.modifiers(),
                w.damageRoll().toString(),
                w.criticalThreatMin(),
                w.criticalMultiplier()
            ))
            .collect(Collectors.toList());

        List<EquippedArmorDto> armors = c.getEquippedArmors().stream()
            .map(a -> new EquippedArmorDto(
                a.id(),
                a.name(),
                a.modifiers(),
                a.maxDexterityBonus()
            ))
            .collect(Collectors.toList());

        List<ActiveConditionDto> conditions = c.getActiveConditions().stream()
            .map(cond -> new ActiveConditionDto(
                cond.id(),
                cond.name(),
                cond.modifiers(),
                cond.durationRounds(),
                cond.stackingGroup()
            ))
            .collect(Collectors.toList());

        SpellcastingDto spellcastingDto = null;
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
            spellcastingDto = new SpellcastingDto(sf.getCasterLevel(), sf.getCastingAttribute().name(), maxSlots, remainingSlots, spellSaveDcs);
        }

        TurnStateDto turnStateDto = new TurnStateDto(
            c.getTurnState().isStandardUsed(),
            c.getTurnState().isMoveUsed(),
            c.getTurnState().isSwiftUsed()
        );

        return new CharacterResponseDto(
            c.getId(),
            c.getName(),
            c.getLevel(),
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

    public record AttributeDto(int score, int modifier) {}
    public record EquippedItemDto(String id, String name, Map<StatType, List<com.themis.engine.domain.Modifier>> modifiers) {}
    public record EquippedWeaponDto(
        String id,
        String name,
        Map<StatType, List<com.themis.engine.domain.Modifier>> modifiers,
        String damageRoll,
        int criticalThreatMin,
        int criticalMultiplier
    ) {}
    public record EquippedArmorDto(
        String id,
        String name,
        Map<StatType, List<com.themis.engine.domain.Modifier>> modifiers,
        Integer maxDexterityBonus
    ) {}
    public record ActiveConditionDto(
        String id,
        String name,
        Map<StatType, List<com.themis.engine.domain.Modifier>> modifiers,
        Integer durationRounds,
        String stackingGroup
    ) {}
    public record SpellcastingDto(int casterLevel, String castingAttribute, int[] maxSlots, int[] remainingSlots, int[] spellSaveDcs) {}
    public record TurnStateDto(boolean standardUsed, boolean moveUsed, boolean swiftUsed) {}
}
