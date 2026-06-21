package com.themis.engine.infrastructure;

import java.io.Serializable;
import java.util.Objects;

public class CharacterActiveConditionId implements Serializable {
    private String characterId;
    private String conditionId;

    public CharacterActiveConditionId() {}

    public CharacterActiveConditionId(String characterId, String conditionId) {
        this.characterId = characterId;
        this.conditionId = conditionId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterActiveConditionId that = (CharacterActiveConditionId) o;
        return Objects.equals(characterId, that.characterId) &&
               Objects.equals(conditionId, that.conditionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, conditionId);
    }
}
