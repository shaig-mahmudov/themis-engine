package com.themis.engine.infrastructure;

import java.io.Serializable;
import java.util.Objects;

public class CharacterEquippedArmorId implements Serializable {
    private String characterId;
    private String armorId;

    public CharacterEquippedArmorId() {}

    public CharacterEquippedArmorId(String characterId, String armorId) {
        this.characterId = characterId;
        this.armorId = armorId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getArmorId() {
        return armorId;
    }

    public void setArmorId(String armorId) {
        this.armorId = armorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterEquippedArmorId that = (CharacterEquippedArmorId) o;
        return Objects.equals(characterId, that.characterId) &&
               Objects.equals(armorId, that.armorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, armorId);
    }
}
