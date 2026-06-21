package com.themis.engine.domain;

/**
 * Represents the detailed outcome of a combat attack resolution.
 */
public record AttackResult(
    boolean isHit,
    boolean isCritical,
    int attackRoll,
    int confirmationRoll,
    int damageDealt,
    String description
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
}
