package com.themis.engine.domain;

/**
 * Represents a generic participant in an encounter.
 * Designed to hold basic combatant info without strictly coupling to the Character entity.
 */
public record EncounterParticipant(
    String combatantId,
    CombatantType combatantType,
    String name,
    Integer initiativeRoll,
    Integer initiativeTotal,
    int dexterityModifier
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public EncounterParticipant {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("Combatant ID cannot be null or blank");
        }
        if (combatantType == null) {
            throw new IllegalArgumentException("Combatant type cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }

    public EncounterParticipant withInitiative(Integer roll, Integer total) {
        return new EncounterParticipant(combatantId, combatantType, name, roll, total, dexterityModifier);
    }
}
