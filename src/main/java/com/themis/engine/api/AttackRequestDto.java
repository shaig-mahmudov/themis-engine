package com.themis.engine.api;

/**
 * Data Transfer Object representing the request body to execute an attack.
 */
public record AttackRequestDto(
    String attackerId,
    String targetId,
    String weaponId,
    Integer d20Roll
) {}
