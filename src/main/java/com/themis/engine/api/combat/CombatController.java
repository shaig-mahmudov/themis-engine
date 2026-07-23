package com.themis.engine.api.combat;

import com.themis.engine.api.combat.request.ResolveAttackRequest;
import com.themis.engine.api.combat.response.AttackResponse;
import com.themis.engine.application.combat.CombatService;
import com.themis.engine.domain.AttackResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing combat actions.
 */
@RestController
@RequestMapping("/api/combat")
public class CombatController {

    private final CombatService combatService;
    private final CombatApiMapper mapper;

    public CombatController(CombatService combatService, CombatApiMapper mapper) {
        this.combatService = combatService;
        this.mapper = mapper;
    }

    @PostMapping("/attack")
    public ResponseEntity<AttackResponse> resolveAttack(@Valid @RequestBody ResolveAttackRequest request) {
        AttackResult result = combatService.resolveAttack(
            request.attackerId(),
            request.targetId(),
            request.weaponId(),
            request.d20Roll()
        );
        return ResponseEntity.ok(mapper.toResponse(result));
    }
}
