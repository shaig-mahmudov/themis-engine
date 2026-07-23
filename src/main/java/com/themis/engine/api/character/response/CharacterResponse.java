package com.themis.engine.api.character.response;

import com.themis.engine.domain.StatType;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing the fully computed state of a Character.
 */
public record CharacterResponse(
    String id,
    String name,
    int level,
    Long version,
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
