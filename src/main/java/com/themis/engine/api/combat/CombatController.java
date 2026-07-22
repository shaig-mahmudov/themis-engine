package com.themis.engine.api.combat;

import com.themis.engine.api.combat.request.AttackRequest;
import com.themis.engine.domain.AttackResult;
import com.themis.engine.application.combat.CombatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing combat actions.
 */
@RestController
@RequestMapping("/api/combat")
public class CombatController {

    private final CombatService combatService;

    public CombatController(CombatService combatService) {
        this.combatService = combatService;
    }

    @PostMapping("/attack")
    public ResponseEntity<AttackResult> resolveAttack(@Valid @RequestBody AttackRequest request) {
        AttackResult result = combatService.resolveAttack(
            request.attackerId(),
            request.targetId(),
            request.weaponId(),
            request.d20Roll()
        );
        return ResponseEntity.ok(result);
    }
}
