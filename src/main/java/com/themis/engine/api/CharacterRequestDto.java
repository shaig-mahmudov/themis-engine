package com.themis.engine.api;

import com.themis.engine.domain.Character;

/**
 * Data Transfer Object representing the request body to create a new Character.
 */
public record CharacterRequestDto(
    String id,
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
