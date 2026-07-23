package com.themis.engine.api.combat.response;

/**
 * Data Transfer Object representing the response of a combat attack resolution.
 */
public record AttackResponse(
    boolean isHit,
    boolean isCritical,
    int attackRoll,
    int confirmationRoll,
    int damageDealt,
    String description
) {}
