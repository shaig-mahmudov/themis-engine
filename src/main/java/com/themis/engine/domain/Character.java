package com.themis.engine.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The primary Aggregate Root representing a Pathfinder 1e character.
 * Automates statistics calculations by integrating the Modifier Stacking Engine.
 */
public class Character implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private final int level;

    // Attributes
    private final Map<StatType, Attribute> attributes = new HashMap<>();

    // Derived Statistics
    private final Map<StatType, DerivedStat> saves = new HashMap<>();
    private final DerivedStat armorClass;
    private final int baseHitPoints;
    private final int baseAttackBonus;
    
    private final ModifierStack hitPointsModifierStack = new ModifierStack();
    private final ModifierStack baseAttackBonusModifierStack = new ModifierStack();

    // State
    private int currentDamage = 0;
    private final List<EquippableItem> equippedItems = new ArrayList<>();
    private final List<Weapon> equippedWeapons = new ArrayList<>();
    private final List<Armor> equippedArmors = new ArrayList<>();
    private final List<Condition> activeConditions = new ArrayList<>();
    private final TurnState turnState = new TurnState();
    private SpellcastingFeature spellcastingFeature;

    public Character(
        String id,
        String name,
        int level,
        int baseStr,
        int baseDex,
        int baseCon,
        int baseInt,
        int baseWis,
        int baseCha,
        int baseHitPoints,
        int baseAttackBonus,
        int baseFortitude,
        int baseReflex,
        int baseWill
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Character ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Character Name cannot be null or blank");
        }
        if (level < 1) {
            throw new IllegalArgumentException("Character Level must be at least 1");
        }
        if (baseStr < 1 || baseDex < 1 || baseCon < 1 || baseInt < 1 || baseWis < 1 || baseCha < 1) {
            throw new IllegalArgumentException("Base ability scores must be at least 1");
        }
        if (baseHitPoints < 1) {
            throw new IllegalArgumentException("Base hit points must be at least 1");
        }
        if (baseAttackBonus < 0) {
            throw new IllegalArgumentException("Base attack bonus cannot be negative");
        }
        if (baseFortitude < 0 || baseReflex < 0 || baseWill < 0) {
            throw new IllegalArgumentException("Base saving throws cannot be negative");
        }

        this.id = id;
        this.name = name;
        this.level = level;

        // Initialize Attributes
        this.attributes.put(StatType.STRENGTH, new Attribute(baseStr));
        this.attributes.put(StatType.DEXTERITY, new Attribute(baseDex));
        this.attributes.put(StatType.CONSTITUTION, new Attribute(baseCon));
        this.attributes.put(StatType.INTELLIGENCE, new Attribute(baseInt));
        this.attributes.put(StatType.WISDOM, new Attribute(baseWis));
        this.attributes.put(StatType.CHARISMA, new Attribute(baseCha));

        // Initialize Saves
        this.saves.put(StatType.FORTITUDE, new DerivedStat(baseFortitude));
        this.saves.put(StatType.REFLEX, new DerivedStat(baseReflex));
        this.saves.put(StatType.WILL, new DerivedStat(baseWill));

        // AC starts with a base of 10
        this.armorClass = new DerivedStat(10);

        this.baseHitPoints = baseHitPoints;
        this.baseAttackBonus = baseAttackBonus;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getAttributeScore(StatType type) {
        Attribute attr = attributes.get(type);
        if (attr == null) {
            throw new IllegalArgumentException("Invalid attribute type: " + type);
        }
        return attr.getScore();
    }

    public int getAttributeModifier(StatType type) {
        Attribute attr = attributes.get(type);
        if (attr == null) {
            throw new IllegalArgumentException("Invalid attribute type: " + type);
        }
        return attr.getModifier();
    }

    /**
     * Calculates Max HP based on base HP + (Constitution Modifier * Level) + HP Modifiers.
     */
    public int getMaxHitPoints() {
        int conModifier = getAttributeModifier(StatType.CONSTITUTION);
        int rawHP = baseHitPoints + (conModifier * level) + hitPointsModifierStack.getTotal();
        // In Pathfinder, you always gain at least 1 HP per level, even with negative Constitution
        return Math.max(level, rawHP);
    }

    public int getCurrentHitPoints() {
        return getMaxHitPoints() - currentDamage;
    }

    public int getBaseAttackBonus() {
        return baseAttackBonus + baseAttackBonusModifierStack.getTotal();
    }

    public int getBaseAttackBonusRaw() {
        return baseAttackBonus;
    }

    public int getBaseHitPoints() {
        return baseHitPoints;
    }

    public int getBaseAttribute(StatType type) {
        Attribute attr = attributes.get(type);
        if (attr == null) {
            throw new IllegalArgumentException("Invalid attribute type: " + type);
        }
        return attr.getBaseScore();
    }

    public int getBaseSave(StatType type) {
        DerivedStat save = saves.get(type);
        if (save == null) {
            throw new IllegalArgumentException("Invalid saving throw type: " + type);
        }
        return save.getBaseValue();
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public int getArmorClass() {
        int dexModifier = getAttributeModifier(StatType.DEXTERITY);
        Integer minMaxDex = equippedArmors.stream()
            .map(Armor::maxDexterityBonus)
            .filter(java.util.Objects::nonNull)
            .reduce(Integer::min)
            .orElse(null);
        if (minMaxDex != null && dexModifier > minMaxDex) {
            dexModifier = minMaxDex;
        }
        return armorClass.getValue(dexModifier);
    }

    public int getSave(StatType saveType) {
        DerivedStat save = saves.get(saveType);
        if (save == null) {
            throw new IllegalArgumentException("Invalid saving throw type: " + saveType);
        }
        
        int attributeModifier = switch (saveType) {
            case FORTITUDE -> getAttributeModifier(StatType.CONSTITUTION);
            case REFLEX -> getAttributeModifier(StatType.DEXTERITY);
            case WILL -> getAttributeModifier(StatType.WISDOM);
            default -> throw new IllegalArgumentException("StatType is not a saving throw: " + saveType);
        };

        return save.getValue(attributeModifier);
    }

    public List<EquippableItem> getEquippedItems() {
        return List.copyOf(equippedItems);
    }

    public List<Weapon> getEquippedWeapons() {
        return List.copyOf(equippedWeapons);
    }

    public List<Armor> getEquippedArmors() {
        return List.copyOf(equippedArmors);
    }

    public List<Condition> getActiveConditions() {
        return List.copyOf(activeConditions);
    }

    public TurnState getTurnState() {
        return turnState;
    }

    public SpellcastingFeature getSpellcastingFeature() {
        return spellcastingFeature;
    }

    public void setSpellcastingFeature(SpellcastingFeature spellcastingFeature) {
        this.spellcastingFeature = spellcastingFeature;
    }

    /**
     * Dynamically calculates the spell save DC for a spell level based on the character's current attributes.
     */
    public int getSpellSaveDC(int spellLevel) {
        if (spellcastingFeature == null) {
            throw new IllegalStateException("Character has no spellcasting feature configured");
        }
        int modifier = getAttributeModifier(spellcastingFeature.getCastingAttribute());
        return spellcastingFeature.calculateSaveDC(spellLevel, modifier);
    }

    // --- Damage / Healing logic ---

    public void damage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage amount cannot be negative");
        }
        currentDamage = Math.clamp(currentDamage + amount, 0, getMaxHitPoints() + getAttributeScore(StatType.CONSTITUTION));
    }

    public void heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing amount cannot be negative");
        }
        currentDamage = Math.max(0, currentDamage - amount);
    }

    public boolean isConscious() {
        return getCurrentHitPoints() >= 0;
    }

    /**
     * In Pathfinder 1e, you die when your negative HP is equal to or greater than your Constitution SCORE.
     */
    public boolean isDead() {
        return getCurrentHitPoints() <= -getAttributeScore(StatType.CONSTITUTION);
    }

    // --- Equipment & Conditions logic ---

    public void equip(EquippableItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot equip null item");
        }
        if (equippedItems.contains(item)) {
            return;
        }
        equippedItems.add(item);
        applyModifiers(item.modifiers());
    }

    public void unequip(EquippableItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot unequip null item");
        }
        if (equippedItems.remove(item)) {
            removeModifiers(item.modifiers());
        }
    }

    public void equipWeapon(Weapon weapon) {
        if (weapon == null) {
            throw new IllegalArgumentException("Cannot equip null weapon");
        }
        if (equippedWeapons.contains(weapon)) {
            return;
        }
        equippedWeapons.add(weapon);
        applyModifiers(weapon.modifiers());
    }

    public void unequipWeapon(Weapon weapon) {
        if (weapon == null) {
            throw new IllegalArgumentException("Cannot unequip null weapon");
        }
        if (equippedWeapons.remove(weapon)) {
            removeModifiers(weapon.modifiers());
        }
    }

    public void equipArmor(Armor armor) {
        if (armor == null) {
            throw new IllegalArgumentException("Cannot equip null armor");
        }
        if (equippedArmors.contains(armor)) {
            return;
        }
        equippedArmors.add(armor);
        applyModifiers(armor.modifiers());
    }

    public void unequipArmor(Armor armor) {
        if (armor == null) {
            throw new IllegalArgumentException("Cannot unequip null armor");
        }
        if (equippedArmors.remove(armor)) {
            removeModifiers(armor.modifiers());
        }
    }

    public void unequipArmorById(String armorId) {
        if (armorId == null || armorId.isBlank()) {
            throw new IllegalArgumentException("Cannot unequip null or blank armor ID");
        }
        Armor found = equippedArmors.stream()
            .filter(a -> a.id().equals(armorId))
            .findFirst()
            .orElse(null);
        if (found != null) {
            unequipArmor(found);
        }
    }

    public void applyCondition(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Cannot apply null condition");
        }
        if (activeConditions.contains(condition)) {
            return;
        }
        activeConditions.add(condition);
        applyModifiers(condition.modifiers());
    }

    public void removeCondition(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Cannot remove null condition");
        }
        Condition stored = activeConditions.stream()
            .filter(c -> c.id().equals(condition.id()))
            .findFirst()
            .orElse(null);
            
        if (stored != null && activeConditions.remove(stored)) {
            removeModifiers(stored.modifiers());
        }
    }

    /**
     * Starts a new turn for the character. Resets the turn's action economy and
     * decrements the duration of active conditions by 1 round.
     */
    public void startTurn() {
        turnState.reset();
        
        List<Condition> toRemove = new ArrayList<>();
        List<Condition> toUpdate = new ArrayList<>();
        
        for (Condition cond : activeConditions) {
            if (cond.durationRounds() != null) {
                int remaining = cond.durationRounds() - 1;
                if (remaining <= 0) {
                    toRemove.add(cond);
                } else {
                    toUpdate.add(cond.withDuration(remaining));
                }
            }
        }
        
        for (Condition cond : toRemove) {
            removeCondition(cond);
        }
        
        for (Condition updated : toUpdate) {
            int index = -1;
            for (int i = 0; i < activeConditions.size(); i++) {
                if (activeConditions.get(i).id().equals(updated.id())) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                activeConditions.set(index, updated);
            }
        }
    }

    // --- Internal Modifier Helpers ---

    private void applyModifiers(Map<StatType, List<Modifier>> modifiersMap) {
        for (Map.Entry<StatType, List<Modifier>> entry : modifiersMap.entrySet()) {
            ModifierStack stack = getModifierStackFor(entry.getKey());
            if (stack != null) {
                entry.getValue().forEach(stack::add);
            }
        }
    }

    private void removeModifiers(Map<StatType, List<Modifier>> modifiersMap) {
        for (Map.Entry<StatType, List<Modifier>> entry : modifiersMap.entrySet()) {
            ModifierStack stack = getModifierStackFor(entry.getKey());
            if (stack != null) {
                entry.getValue().forEach(stack::remove);
            }
        }
    }

    private ModifierStack getModifierStackFor(StatType statType) {
        if (attributes.containsKey(statType)) {
            return attributes.get(statType).getModifierStack();
        } else if (saves.containsKey(statType)) {
            return saves.get(statType).getModifierStack();
        } else if (statType == StatType.ARMOR_CLASS) {
            return armorClass.getModifierStack();
        } else if (statType == StatType.HIT_POINTS) {
            return hitPointsModifierStack;
        } else if (statType == StatType.BASE_ATTACK_BONUS) {
            return baseAttackBonusModifierStack;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Character character = (Character) o;
        return id.equals(character.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
