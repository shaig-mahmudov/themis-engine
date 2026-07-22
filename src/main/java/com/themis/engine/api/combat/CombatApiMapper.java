package com.themis.engine.api.combat;

import com.themis.engine.api.combat.response.AttackResponse;
import com.themis.engine.domain.AttackResult;
import org.springframework.stereotype.Component;

/**
 * Component responsible for mapping between Combat API contracts and Domain objects.
 */
@Component
public class CombatApiMapper {

    public AttackResponse toResponse(AttackResult result) {
        if (result == null) {
            return null;
        }
        return new AttackResponse(
            result.isHit(),
            result.isCritical(),
            result.attackRoll(),
            result.confirmationRoll(),
            result.damageDealt(),
            result.description()
        );
    }
}
