package com.themis.engine.infrastructure;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(name = "characters")
public class CharacterEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int level;

    // Base attributes
    @Column(name = "base_str", nullable = false)
    private int baseStr;

    @Column(name = "base_dex", nullable = false)
    private int baseDex;

    @Column(name = "base_con", nullable = false)
    private int baseCon;

    @Column(name = "base_int", nullable = false)
    private int baseInt;

    @Column(name = "base_wis", nullable = false)
    private int baseWis;

    @Column(name = "base_cha", nullable = false)
    private int baseCha;

    // Base derived statistics
    @Column(name = "base_hit_points", nullable = false)
    private int baseHitPoints;

    @Column(name = "base_attack_bonus", nullable = false)
    private int baseAttackBonus;

    @Column(name = "base_fortitude", nullable = false)
    private int baseFortitude;

    @Column(name = "base_reflex", nullable = false)
    private int baseReflex;

    @Column(name = "base_will", nullable = false)
    private int baseWill;

    @Column(name = "current_damage", nullable = false)
    private int currentDamage;

    // Spellcasting Feature
    @Column(name = "spellcasting_caster_level")
    private Integer spellcastingCasterLevel;

    @Column(name = "spellcasting_attribute")
    private String spellcastingAttribute;

    @Column(name = "spellcasting_max_slots")
    private String spellcastingMaxSlots;

    @Column(name = "spellcasting_remaining_slots")
    private String spellcastingRemainingSlots;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "character_id")
    private Set<CharacterEquippedItemEntity> equippedItems = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "character_id")
    private Set<CharacterActiveConditionEntity> activeConditions = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "character_id")
    private Set<CharacterEquippedWeaponEntity> equippedWeapons = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "character_id")
    private Set<CharacterEquippedArmorEntity> equippedArmors = new LinkedHashSet<>();

    @Column(name = "standard_used", nullable = false)
    private boolean standardUsed;

    @Column(name = "move_used", nullable = false)
    private boolean moveUsed;

    @Column(name = "swift_used", nullable = false)
    private boolean swiftUsed;

    public CharacterEntity() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getBaseStr() {
        return baseStr;
    }

    public void setBaseStr(int baseStr) {
        this.baseStr = baseStr;
    }

    public int getBaseDex() {
        return baseDex;
    }

    public void setBaseDex(int baseDex) {
        this.baseDex = baseDex;
    }

    public int getBaseCon() {
        return baseCon;
    }

    public void setBaseCon(int baseCon) {
        this.baseCon = baseCon;
    }

    public int getBaseInt() {
        return baseInt;
    }

    public void setBaseInt(int baseInt) {
        this.baseInt = baseInt;
    }

    public int getBaseWis() {
        return baseWis;
    }

    public void setBaseWis(int baseWis) {
        this.baseWis = baseWis;
    }

    public int getBaseCha() {
        return baseCha;
    }

    public void setBaseCha(int baseCha) {
        this.baseCha = baseCha;
    }

    public int getBaseHitPoints() {
        return baseHitPoints;
    }

    public void setBaseHitPoints(int baseHitPoints) {
        this.baseHitPoints = baseHitPoints;
    }

    public int getBaseAttackBonus() {
        return baseAttackBonus;
    }

    public void setBaseAttackBonus(int baseAttackBonus) {
        this.baseAttackBonus = baseAttackBonus;
    }

    public int getBaseFortitude() {
        return baseFortitude;
    }

    public void setBaseFortitude(int baseFortitude) {
        this.baseFortitude = baseFortitude;
    }

    public int getBaseReflex() {
        return baseReflex;
    }

    public void setBaseReflex(int baseReflex) {
        this.baseReflex = baseReflex;
    }

    public int getBaseWill() {
        return baseWill;
    }

    public void setBaseWill(int baseWill) {
        this.baseWill = baseWill;
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public void setCurrentDamage(int currentDamage) {
        this.currentDamage = currentDamage;
    }

    public Integer getSpellcastingCasterLevel() {
        return spellcastingCasterLevel;
    }

    public void setSpellcastingCasterLevel(Integer spellcastingCasterLevel) {
        this.spellcastingCasterLevel = spellcastingCasterLevel;
    }

    public String getSpellcastingAttribute() {
        return spellcastingAttribute;
    }

    public void setSpellcastingAttribute(String spellcastingAttribute) {
        this.spellcastingAttribute = spellcastingAttribute;
    }

    public String getSpellcastingMaxSlots() {
        return spellcastingMaxSlots;
    }

    public void setSpellcastingMaxSlots(String spellcastingMaxSlots) {
        this.spellcastingMaxSlots = spellcastingMaxSlots;
    }

    public String getSpellcastingRemainingSlots() {
        return spellcastingRemainingSlots;
    }

    public void setSpellcastingRemainingSlots(String spellcastingRemainingSlots) {
        this.spellcastingRemainingSlots = spellcastingRemainingSlots;
    }

    public Set<CharacterEquippedItemEntity> getEquippedItems() {
        return equippedItems;
    }

    public void setEquippedItems(Set<CharacterEquippedItemEntity> equippedItems) {
        this.equippedItems = equippedItems;
    }

    public Set<CharacterActiveConditionEntity> getActiveConditions() {
        return activeConditions;
    }

    public void setActiveConditions(Set<CharacterActiveConditionEntity> activeConditions) {
        this.activeConditions = activeConditions;
    }

    public Set<CharacterEquippedWeaponEntity> getEquippedWeapons() {
        return equippedWeapons;
    }

    public void setEquippedWeapons(Set<CharacterEquippedWeaponEntity> equippedWeapons) {
        this.equippedWeapons = equippedWeapons;
    }

    public Set<CharacterEquippedArmorEntity> getEquippedArmors() {
        return equippedArmors;
    }

    public void setEquippedArmors(Set<CharacterEquippedArmorEntity> equippedArmors) {
        this.equippedArmors = equippedArmors;
    }

    public boolean isStandardUsed() {
        return standardUsed;
    }

    public void setStandardUsed(boolean standardUsed) {
        this.standardUsed = standardUsed;
    }

    public boolean isMoveUsed() {
        return moveUsed;
    }

    public void setMoveUsed(boolean moveUsed) {
        this.moveUsed = moveUsed;
    }

    public boolean isSwiftUsed() {
        return swiftUsed;
    }

    public void setSwiftUsed(boolean swiftUsed) {
        this.swiftUsed = swiftUsed;
    }
}
