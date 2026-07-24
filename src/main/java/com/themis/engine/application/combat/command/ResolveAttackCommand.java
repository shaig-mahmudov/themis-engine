package com.themis.engine.application.combat.command;

/**
 * Groups the inputs required to resolve a combat attack.
 */
public record ResolveAttackCommand(
    String attackerId,
    String targetId,
    String weaponId,
    Integer d20Roll
) {
}
