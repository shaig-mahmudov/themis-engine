package com.themis.engine.application.character;

import com.themis.engine.application.character.command.ConfigureSpellcastingCommand;
import com.themis.engine.domain.ActionType;
import com.themis.engine.domain.Armor;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.Condition;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.SpellcastingFeature;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Weapon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service that orchestrates state-changing character use cases.
 */
@Service
@Transactional
public class CharacterCommandService {

    private final CharacterStore characterStore;

    public CharacterCommandService(CharacterStore characterStore) {
        this.characterStore = characterStore;
    }

    public Character createCharacter(Character character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }
        return characterStore.save(character);
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
            c.heal(c.getMaxHitPoints());
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

    public Optional<Character> configureSpellcasting(ConfigureSpellcastingCommand command) {
        Objects.requireNonNull(command, "Configure spellcasting command cannot be null");

        String id = command.characterId();
        int casterLevel = command.casterLevel();
        StatType castingAttribute = command.castingAttribute();
        List<Integer> maxSlots = command.maxSlots();

        if (castingAttribute == null) {
            throw new IllegalArgumentException("Casting attribute cannot be null");
        }
        if (maxSlots == null || maxSlots.size() != 10) {
            throw new IllegalArgumentException("Max slots must have exactly 10 elements");
        }
        return characterStore.findById(id).map(c -> {
            SpellcastingFeature spellcastingFeature = new SpellcastingFeature(casterLevel, castingAttribute);
            for (int i = 0; i <= 9; i++) {
                spellcastingFeature.setMaxSlots(i, maxSlots.get(i));
                spellcastingFeature.setRemainingSlots(i, maxSlots.get(i));
            }
            c.setSpellcastingFeature(spellcastingFeature);
            return characterStore.save(c);
        });
    }

    public Optional<Character> consumeSpellSlot(String id, int spellLevel) {
        return characterStore.findById(id).map(c -> {
            if (c.getSpellcastingFeature() == null) {
                throw new IllegalStateException("Character has no spellcasting feature");
            }
            c.getSpellcastingFeature().consumeSlot(spellLevel);
            return characterStore.save(c);
        });
    }
}
