package com.themis.engine.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "character_equipped_weapons")
@IdClass(CharacterEquippedWeaponId.class)
public class CharacterEquippedWeaponEntity {
    @Id
    @Column(name = "character_id")
    private String characterId;

    @Id
    @Column(name = "weapon_id")
    private String weaponId;

    private String name;

    @Column(name = "modifiers_json", columnDefinition = "TEXT")
    private String modifiersJson;

    @Column(name = "damage_roll")
    private String damageRoll;

    @Column(name = "critical_threat_min")
    private int criticalThreatMin;

    @Column(name = "critical_multiplier")
    private int criticalMultiplier;

    private String type;

    public CharacterEquippedWeaponEntity() {}

    public CharacterEquippedWeaponEntity(
        String characterId,
        String weaponId,
        String name,
        String type,
        String modifiersJson,
        String damageRoll,
        int criticalThreatMin,
        int criticalMultiplier
    ) {
        this.characterId = characterId;
        this.weaponId = weaponId;
        this.name = name;
        this.type = type;
        this.modifiersJson = modifiersJson;
        this.damageRoll = damageRoll;
        this.criticalThreatMin = criticalThreatMin;
        this.criticalMultiplier = criticalMultiplier;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(String weaponId) {
        this.weaponId = weaponId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModifiersJson() {
        return modifiersJson;
    }

    public void setModifiersJson(String modifiersJson) {
        this.modifiersJson = modifiersJson;
    }

    public String getDamageRoll() {
        return damageRoll;
    }

    public void setDamageRoll(String damageRoll) {
        this.damageRoll = damageRoll;
    }

    public int getCriticalThreatMin() {
        return criticalThreatMin;
    }

    public void setCriticalThreatMin(int criticalThreatMin) {
        this.criticalThreatMin = criticalThreatMin;
    }

    public int getCriticalMultiplier() {
        return criticalMultiplier;
    }

    public void setCriticalMultiplier(int criticalMultiplier) {
        this.criticalMultiplier = criticalMultiplier;
    }
}
