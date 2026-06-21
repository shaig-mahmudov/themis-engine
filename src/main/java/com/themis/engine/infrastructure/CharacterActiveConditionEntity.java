package com.themis.engine.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "character_active_conditions")
@IdClass(CharacterActiveConditionId.class)
public class CharacterActiveConditionEntity {
    @Id
    @Column(name = "character_id")
    private String characterId;

    @Id
    @Column(name = "condition_id")
    private String conditionId;

    private String name;

    @Column(name = "modifiers_json", columnDefinition = "TEXT")
    private String modifiersJson;

    public CharacterActiveConditionEntity() {}

    public CharacterActiveConditionEntity(String characterId, String conditionId, String name, String modifiersJson) {
        this.characterId = characterId;
        this.conditionId = conditionId;
        this.name = name;
        this.modifiersJson = modifiersJson;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getConditionId() {
        return conditionId;
    }

    public void setConditionId(String conditionId) {
        this.conditionId = conditionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModifiersJson() {
        return modifiersJson;
    }

    public void setModifiersJson(String modifiersJson) {
        this.modifiersJson = modifiersJson;
    }
}
