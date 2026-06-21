package com.themis.engine.infrastructure;

import java.io.Serializable;
import java.util.Objects;

public class EncounterParticipantId implements Serializable {
    private static final long serialVersionUID = 1L;

    private String encounterId;
    private String combatantId;

    public EncounterParticipantId() {}

    public EncounterParticipantId(String encounterId, String combatantId) {
        this.encounterId = encounterId;
        this.combatantId = combatantId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getCombatantId() {
        return combatantId;
    }

    public void setCombatantId(String combatantId) {
        this.combatantId = combatantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncounterParticipantId that = (EncounterParticipantId) o;
        return Objects.equals(encounterId, that.encounterId) &&
               Objects.equals(combatantId, that.combatantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encounterId, combatantId);
    }
}
