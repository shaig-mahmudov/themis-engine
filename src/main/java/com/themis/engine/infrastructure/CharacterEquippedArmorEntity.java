package com.themis.engine.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "character_equipped_armors")
@IdClass(CharacterEquippedArmorId.class)
public class CharacterEquippedArmorEntity {
    @Id
    @Column(name = "character_id")
    private String characterId;

    @Id
    @Column(name = "armor_id")
    private String armorId;

    private String name;

    @Column(name = "modifiers_json", columnDefinition = "TEXT")
    private String modifiersJson;

    @Column(name = "max_dexterity_bonus")
    private Integer maxDexterityBonus;

    public CharacterEquippedArmorEntity() {}

    public CharacterEquippedArmorEntity(
        String characterId,
        String armorId,
        String name,
        String modifiersJson,
        Integer maxDexterityBonus
    ) {
        this.characterId = characterId;
        this.armorId = armorId;
        this.name = name;
        this.modifiersJson = modifiersJson;
        this.maxDexterityBonus = maxDexterityBonus;
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

    public Integer getMaxDexterityBonus() {
        return maxDexterityBonus;
    }

    public void setMaxDexterityBonus(Integer maxDexterityBonus) {
        this.maxDexterityBonus = maxDexterityBonus;
    }
}
