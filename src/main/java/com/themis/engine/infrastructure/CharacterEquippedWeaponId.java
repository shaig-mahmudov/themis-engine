package com.themis.engine.infrastructure;

import java.io.Serializable;
import java.util.Objects;

public class CharacterEquippedWeaponId implements Serializable {
    private String characterId;
    private String weaponId;

    public CharacterEquippedWeaponId() {}

    public CharacterEquippedWeaponId(String characterId, String weaponId) {
        this.characterId = characterId;
        this.weaponId = weaponId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterEquippedWeaponId that = (CharacterEquippedWeaponId) o;
        return Objects.equals(characterId, that.characterId) &&
               Objects.equals(weaponId, that.weaponId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, weaponId);
    }
}
