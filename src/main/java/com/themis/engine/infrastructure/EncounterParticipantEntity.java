package com.themis.engine.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "encounter_participants")
@IdClass(EncounterParticipantId.class)
public class EncounterParticipantEntity {

    @Id
    @Column(name = "encounter_id")
    private String encounterId;

    @Id
    @Column(name = "combatant_id")
    private String combatantId;

    @Column(name = "combatant_type", nullable = false)
    private String combatantType;

    @Column(nullable = false)
    private String name;

    @Column(name = "initiative_roll")
    private Integer initiativeRoll;

    @Column(name = "initiative_total")
    private Integer initiativeTotal;

    @Column(name = "dexterity_modifier", nullable = false)
    private int dexterityModifier;

    public EncounterParticipantEntity() {}

    public EncounterParticipantEntity(
        String encounterId,
        String combatantId,
        String combatantType,
        String name,
        Integer initiativeRoll,
        Integer initiativeTotal,
        int dexterityModifier
    ) {
        this.encounterId = encounterId;
        this.combatantId = combatantId;
        this.combatantType = combatantType;
        this.name = name;
        this.initiativeRoll = initiativeRoll;
        this.initiativeTotal = initiativeTotal;
        this.dexterityModifier = dexterityModifier;
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

    public String getCombatantType() {
        return combatantType;
    }

    public void setCombatantType(String combatantType) {
        this.combatantType = combatantType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getInitiativeRoll() {
        return initiativeRoll;
    }

    public void setInitiativeRoll(Integer initiativeRoll) {
        this.initiativeRoll = initiativeRoll;
    }

    public Integer getInitiativeTotal() {
        return initiativeTotal;
    }

    public void setInitiativeTotal(Integer initiativeTotal) {
        this.initiativeTotal = initiativeTotal;
    }

    public int getDexterityModifier() {
        return dexterityModifier;
    }

    public void setDexterityModifier(int dexterityModifier) {
        this.dexterityModifier = dexterityModifier;
    }
}
