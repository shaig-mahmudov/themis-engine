package com.themis.engine.api.combat;

import com.themis.engine.api.combat.response.AttackResponse;
import com.themis.engine.domain.AttackResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatApiMapperTest {

    private CombatApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CombatApiMapper();
    }

    @Test
    void testToResponse() {
        AttackResult result = new AttackResult(true, false, 18, 0, 7, "Hit for 7 damage");

        AttackResponse response = mapper.toResponse(result);

        assertNotNull(response);
        assertTrue(response.isHit());
        assertFalse(response.isCritical());
        assertEquals(18, response.attackRoll());
        assertEquals(0, response.confirmationRoll());
        assertEquals(7, response.damageDealt());
        assertEquals("Hit for 7 damage", response.description());
    }

    @Test
    void testToResponse_Null() {
        assertNull(mapper.toResponse(null));
    }
}
