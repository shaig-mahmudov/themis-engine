package com.themis.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themis.engine.domain.*;
import com.themis.engine.domain.Character;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CombatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CharacterStore characterStore;

    @Test
    void testResolveAttack_Success() throws Exception {
        // 1. Create attacker (Valeros, Str 14 (+2), BAB +1)
        Character attacker = new Character(
            "attacker-id", "Valeros", 1,
            14, 10, 10, 10, 10, 10,
            10, 1, 0, 0, 0
        );
        // Equip weapon (Longsword 1d8, threat 19, mult 2)
        Weapon longsword = new Weapon(
            "lswd", "Longsword", WeaponType.MELEE, Map.of(), DiceRoll.parse("1d8"), 19, 2
        );
        attacker.equipWeapon(longsword);
        characterStore.save(attacker);

        // 2. Create target (Goblin, AC 15, Max HP 10)
        Character target = new Character(
            "target-id", "Goblin", 1,
            10, 10, 10, 10, 10, 10,
            10, 0, 0, 0, 0
        );
        target.equip(new EquippableItem("goblin-shield", "Shield", 
            Map.of(StatType.ARMOR_CLASS, java.util.List.of(new Modifier(5, ModifierType.SHIELD, "Shield")))));
        characterStore.save(target);

        // 3. Resolve attack: d20Roll = 12. Total attack = 12 + 1 (BAB) + 2 (Str) = 15. AC is 15. -> Hit!
        AttackRequestDto attackReq = new AttackRequestDto(
            "attacker-id",
            "target-id",
            "lswd",
            12
        );

        mockMvc.perform(post("/api/combat/attack")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHit").value(true))
                .andExpect(jsonPath("$.isCritical").value(false))
                .andExpect(jsonPath("$.attackRoll").value(15))
                .andExpect(jsonPath("$.damageDealt").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void testResolveAttack_ConsumesStandardAction_AndFailsOnConsecutiveAttacks() throws Exception {
        // 1. Create attacker (Valeros, Str 14 (+2), BAB +1)
        Character attacker = new Character(
            "attacker-id-2", "Valeros", 1,
            14, 10, 10, 10, 10, 10,
            10, 1, 0, 0, 0
        );
        Weapon longsword = new Weapon(
            "lswd2", "Longsword", WeaponType.MELEE, Map.of(), DiceRoll.parse("1d8"), 19, 2
        );
        attacker.equipWeapon(longsword);
        characterStore.save(attacker);

        // 2. Create target (Goblin, AC 10, Max HP 10)
        Character target = new Character(
            "target-id-2", "Goblin", 1,
            10, 10, 10, 10, 10, 10,
            10, 0, 0, 0, 0
        );
        characterStore.save(target);

        AttackRequestDto attackReq = new AttackRequestDto(
            "attacker-id-2",
            "target-id-2",
            "lswd2",
            12
        );

        // First attack succeeds
        mockMvc.perform(post("/api/combat/attack")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isOk());

        // Second attack fails because standard action is already used
        mockMvc.perform(post("/api/combat/attack")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot attack: standard action already consumed in current turn"));

        // Reset turn state via start-turn endpoint
        mockMvc.perform(post("/api/characters/attacker-id-2/start-turn")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnState.standardUsed").value(false));

        // Third attack now succeeds again
        mockMvc.perform(post("/api/combat/attack")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isOk());
    }
}
