package com.themis.engine.api.character.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.themis.engine.domain.Character;

/**
 * Data Transfer Object representing the request body to create a new Character.
 */
public record CharacterRequestDto(
    @NotBlank(message = "Character ID cannot be blank")
    String id,

    @NotBlank(message = "Character Name cannot be blank")
    String name,

    @Min(value = 1, message = "Character Level must be at least 1")
    int level,

    @Min(value = 1, message = "Base Strength must be at least 1")
    int baseStr,

    @Min(value = 1, message = "Base Dexterity must be at least 1")
    int baseDex,

    @Min(value = 1, message = "Base Constitution must be at least 1")
    int baseCon,

    @Min(value = 1, message = "Base Intelligence must be at least 1")
    int baseInt,

    @Min(value = 1, message = "Base Wisdom must be at least 1")
    int baseWis,

    @Min(value = 1, message = "Base Charisma must be at least 1")
    int baseCha,

    @Min(value = 1, message = "Base Hit Points must be at least 1")
    int baseHitPoints,

    @Min(value = 0, message = "Base Attack Bonus cannot be negative")
    int baseAttackBonus,

    @Min(value = 0, message = "Base Fortitude Save cannot be negative")
    int baseFortitude,

    @Min(value = 0, message = "Base Reflex Save cannot be negative")
    int baseReflex,

    @Min(value = 0, message = "Base Will Save cannot be negative")
    int baseWill
) {
    public Character toDomain() {
        return new Character(
            id,
            name,
            level,
            baseStr,
            baseDex,
            baseCon,
            baseInt,
            baseWis,
            baseCha,
            baseHitPoints,
            baseAttackBonus,
            baseFortitude,
            baseReflex,
            baseWill
        );
    }
}
