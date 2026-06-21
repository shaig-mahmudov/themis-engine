package com.themis.engine.api;

import com.themis.engine.domain.AttackResult;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.RuleEngine;
import com.themis.engine.domain.Weapon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * REST Controller exposing combat actions.
 */
@RestController
@RequestMapping("/api/combat")
public class CombatController {

    private final CharacterStore characterStore;
    private final RuleEngine ruleEngine = new RuleEngine();
    private final RandomGenerator random = new SecureRandom();

    public CombatController(CharacterStore characterStore) {
        this.characterStore = characterStore;
    }

    @PostMapping("/attack")
    public ResponseEntity<AttackResult> resolveAttack(@RequestBody AttackRequestDto request) {
        if (request.attackerId() == null || request.targetId() == null || request.weaponId() == null) {
            return ResponseEntity.badRequest().build();
        }

        var attackerOpt = characterStore.findById(request.attackerId());
        var targetOpt = characterStore.findById(request.targetId());

        if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Character attacker = attackerOpt.get();
        Character target = targetOpt.get();

        Weapon weapon = attacker.getEquippedWeapons().stream()
            .filter(w -> w.id().equals(request.weaponId()))
            .findFirst()
            .orElse(null);

        if (weapon == null) {
            return ResponseEntity.badRequest().build();
        }

        int d20Roll = request.d20Roll() != null ? request.d20Roll() : random.nextInt(1, 21);

        AttackResult result = ruleEngine.resolveAttack(attacker, weapon, target, d20Roll, random);

        // Persist both characters (target's HP changed, and attacker might have active states updated)
        characterStore.save(attacker);
        characterStore.save(target);

        return ResponseEntity.ok(result);
    }
}
