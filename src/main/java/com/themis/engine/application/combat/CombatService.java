package com.themis.engine.application.combat;

import com.themis.engine.domain.*;
import com.themis.engine.domain.Character;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.random.RandomGenerator;

/**
 * Service that orchestrates combat use cases.
 * Resolves attacks and updates characters within a single transaction boundary.
 */
@Service
@Transactional
public class CombatService {

    private final CharacterStore characterStore;
    private final RuleEngine ruleEngine;
    private final RandomGenerator random;

    public CombatService(
        CharacterStore characterStore,
        RuleEngine ruleEngine,
        RandomGenerator random
    ) {
        this.characterStore = characterStore;
        this.ruleEngine = ruleEngine;
        this.random = random;
    }

    public AttackResult resolveAttack(
        String attackerId,
        String targetId,
        String weaponId,
        Integer d20Roll
    ) {
        if (attackerId == null || attackerId.isBlank()) {
            throw new IllegalArgumentException("Attacker ID cannot be null or blank");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Target ID cannot be null or blank");
        }
        if (weaponId == null || weaponId.isBlank()) {
            throw new IllegalArgumentException("Weapon ID cannot be null or blank");
        }

        com.themis.engine.domain.Character attacker = characterStore.findById(attackerId)
            .orElseThrow(() -> new IllegalArgumentException("Attacker character not found: " + attackerId));
        Character target = characterStore.findById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("Target character not found: " + targetId));

        Weapon weapon = attacker.getEquippedWeapons().stream()
            .filter(w -> w.id().equals(weaponId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Weapon " + weaponId + " is not equipped on attacker " + attackerId));

        if (!attacker.getTurnState().canConsume(ActionType.STANDARD)) {
            throw new IllegalStateException("Cannot attack: standard action already consumed in current turn");
        }
        attacker.getTurnState().consume(ActionType.STANDARD);

        int roll = d20Roll != null ? d20Roll : random.nextInt(1, 21);

        AttackResult result = ruleEngine.resolveAttack(attacker, weapon, target, roll, random);

        // Save both within the same transaction to guarantee data consistency
        characterStore.save(attacker);
        characterStore.save(target);

        return result;
    }
}
