package com.themis.engine.infrastructure;

import java.io.Serializable;
import java.util.Objects;

public class CharacterEquippedItemId implements Serializable {
    private String characterId;
    private String itemId;

    public CharacterEquippedItemId() {}

    public CharacterEquippedItemId(String characterId, String itemId) {
        this.characterId = characterId;
        this.itemId = itemId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterEquippedItemId that = (CharacterEquippedItemId) o;
        return Objects.equals(characterId, that.characterId) &&
               Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, itemId);
    }
}
