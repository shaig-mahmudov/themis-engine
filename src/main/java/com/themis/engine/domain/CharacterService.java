package com.themis.engine.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Service that orchestrates use cases related to characters.
 * Enforces transaction boundaries for read-modify-write operations.
 */
@Service
@Transactional
public class CharacterService {

    private final CharacterStore characterStore;

    public CharacterService(CharacterStore characterStore) {
        this.characterStore = characterStore;
    }

    public Character createCharacter(Character character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }
        return characterStore.save(character);
    }

    @Transactional(readOnly = true)
    public Optional<Character> getCharacter(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Character ID cannot be null or blank");
        }
        return characterStore.findById(id);
    }

    public Optional<Character> equipItem(String id, EquippableItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        return characterStore.findById(id).map(c -> {
            c.equip(item);
            return characterStore.save(c);
        });
    }

    public Optional<Character> equipWeapon(String id, Weapon weapon) {
        if (weapon == null) {
            throw new IllegalArgumentException("Weapon cannot be null");
        }
        return characterStore.findById(id).map(c -> {
            c.equipWeapon(weapon);
            return characterStore.save(c);
        });
    }

    public Optional<Character> equipArmor(String id, Armor armor) {
        if (armor == null) {
            throw new IllegalArgumentException("Armor cannot be null");
        }
        return characterStore.findById(id).map(c -> {
            c.equipArmor(armor);
            return characterStore.save(c);
        });
    }

    public Optional<Character> unequipArmor(String id, String armorId) {
        if (armorId == null || armorId.isBlank()) {
            throw new IllegalArgumentException("Armor ID cannot be null or blank");
        }
        return characterStore.findById(id).map(c -> {
            c.unequipArmorById(armorId);
            return characterStore.save(c);
        });
    }

    public Optional<Character> applyCondition(String id, Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }
        return characterStore.findById(id).map(c -> {
            c.applyCondition(condition);
            return characterStore.save(c);
        });
    }

    public Optional<Character> restCharacter(String id) {
        return characterStore.findById(id).map(c -> {
            c.getTurnState().reset();
            if (c.getSpellcastingFeature() != null) {
                c.getSpellcastingFeature().restAndRecover();
            }
            c.heal(c.getMaxHitPoints()); // Rest heals characters to full HP
            return characterStore.save(c);
        });
    }

    public Optional<Character> damageCharacter(String id, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage amount cannot be negative");
        }
        return characterStore.findById(id).map(c -> {
            c.damage(amount);
            return characterStore.save(c);
        });
    }

    public Optional<Character> healCharacter(String id, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing amount cannot be negative");
        }
        return characterStore.findById(id).map(c -> {
            c.heal(amount);
            return characterStore.save(c);
        });
    }

    public Optional<Character> startTurn(String id) {
        return characterStore.findById(id).map(c -> {
            c.startTurn();
            return characterStore.save(c);
        });
    }

    public Optional<Character> consumeAction(String id, ActionType actionType) {
        if (actionType == null) {
            throw new IllegalArgumentException("Action type cannot be null");
        }
        return characterStore.findById(id).map(c -> {
            c.getTurnState().consume(actionType);
            return characterStore.save(c);
        });
    }
}
