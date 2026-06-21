package com.themis.engine.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "character_equipped_items")
@IdClass(CharacterEquippedItemId.class)
public class CharacterEquippedItemEntity {
    @Id
    @Column(name = "character_id")
    private String characterId;

    @Id
    @Column(name = "item_id")
    private String itemId;

    private String name;

    @Column(name = "modifiers_json", columnDefinition = "TEXT")
    private String modifiersJson;

    public CharacterEquippedItemEntity() {}

    public CharacterEquippedItemEntity(String characterId, String itemId, String name, String modifiersJson) {
        this.characterId = characterId;
        this.itemId = itemId;
        this.name = name;
        this.modifiersJson = modifiersJson;
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
