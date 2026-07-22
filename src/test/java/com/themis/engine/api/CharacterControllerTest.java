package com.themis.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themis.engine.api.character.request.CharacterRequestDto;
import com.themis.engine.api.character.request.EquipArmorRequestDto;
import com.themis.engine.api.character.request.EquipWeaponRequestDto;
import com.themis.engine.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateCharacterAndGet() throws Exception {
        CharacterRequestDto request = new CharacterRequestDto(
            "test-char-1",
            "Valeros",
            3,
            16, 12, 14, 10, 10, 10,
            24, 3, 3, 1, 1
        );

        mockMvc.perform(post("/api/characters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test-char-1"))
                .andExpect(jsonPath("$.name").value("Valeros"))
                .andExpect(jsonPath("$.level").value(3))
                .andExpect(jsonPath("$.maxHitPoints").value(30)) // 24 + (CON mod 2 * lvl 3) = 30
                .andExpect(jsonPath("$.currentHitPoints").value(30));

        mockMvc.perform(get("/api/characters/test-char-1")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Valeros"))
                .andExpect(jsonPath("$.attributes.STRENGTH.score").value(16))
                .andExpect(jsonPath("$.attributes.STRENGTH.modifier").value(3));
    }

    @Test
    void testEquipItemAndCondition() throws Exception {
        // 1. Create character
        CharacterRequestDto request = new CharacterRequestDto(
            "test-char-2",
            "Kyra",
            2,
            12, 10, 12, 10, 16, 14,
            16, 1, 3, 0, 3
        );
        mockMvc.perform(post("/api/characters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Equip item
        EquippableItem ring = new EquippableItem(
            "ring-protection-1",
            "Ring of Protection +1",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(1, ModifierType.DEFLECTION, new ModifierSource("ring", "Ring", SourceType.ITEM))))
        );
        mockMvc.perform(post("/api/characters/test-char-2/equip-item")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ring)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.armorClass").value(11)); // Base 10 + Dex 0 + Ring 1 = 11

        // 3. Apply condition
        Condition shaken = new Condition(
            "shaken-cond",
            "Shaken",
            Map.of(StatType.WILL, List.of(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("fear", "Fear", SourceType.CONDITION))))
        );
        mockMvc.perform(post("/api/characters/test-char-2/apply-condition")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shaken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.willSave").value(4)); // Base 3 + Wis 3 - Shaken 2 = 4
    }

    @Test
    void testCreateCharacter_ValidationFailed() throws Exception {
        // Blank name and invalid level
        CharacterRequestDto request = new CharacterRequestDto(
            "test-char-3",
            "",
            0,
            16, 12, 14, 10, 10, 10,
            24, 3, 3, 1, 1
        );

        mockMvc.perform(post("/api/characters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Input validation failed for request body"));
    }

    @Test
    void testEquipWeapon_ValidationFailed() throws Exception {
        // Invalid critical multiplier (<2) and invalid threat min (<15)
        EquipWeaponRequestDto request = new EquipWeaponRequestDto(
            "invalid-weap",
            "Invalid Weapon",
            WeaponType.MELEE,
            Map.of(),
            "1d8",
            14,
            1
        );

        mockMvc.perform(post("/api/characters/test-char-2/equip-weapon")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testDamageCharacter_ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/characters/test-char-2/damage?amount=-5")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Damage amount cannot be negative"));
    }

    @Test
    void testEquipArmor_Success() throws Exception {
        // 1. Create character with high dex (18 -> +4 mod)
        CharacterRequestDto request = new CharacterRequestDto(
            "test-char-dex",
            "Merisiel",
            1,
            10, 18, 12, 10, 10, 10,
            8, 0, 0, 2, 0
        );
        mockMvc.perform(post("/api/characters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.armorClass").value(14)); // Base 10 + Dex 4 = 14

        // 2. Equip Full Plate (Max Dex 1, Armor bonus +8)
        EquipArmorRequestDto fullPlate = new EquipArmorRequestDto(
            "full-plate-1",
            "Full Plate",
            Map.of(StatType.ARMOR_CLASS, List.of(new Modifier(8, ModifierType.ARMOR, new ModifierSource("plate-1", "Full Plate", SourceType.ITEM)))),
            1
        );
        mockMvc.perform(post("/api/characters/test-char-dex/equip-armor")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fullPlate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.armorClass").value(19)) // Base 10 + Dex 1 (capped) + 8 armor = 19
                .andExpect(jsonPath("$.equippedArmors[0].id").value("full-plate-1"));

        // 3. Unequip Full Plate
        mockMvc.perform(post("/api/characters/test-char-dex/unequip-armor?armorId=full-plate-1")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.armorClass").value(14)) // Base 10 + Dex 4 = 14
                .andExpect(jsonPath("$.equippedArmors").isEmpty());
    }

    @Test
    void testEquipArmor_ValidationFailed() throws Exception {
        EquipArmorRequestDto request = new EquipArmorRequestDto(
            "invalid-armor",
            "Invalid Armor",
            Map.of(),
            -1 // negative max dexterity bonus
        );

        mockMvc.perform(post("/api/characters/test-char-2/equip-armor")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testSpellcastingFlow() throws Exception {
        // 1. Create Character
        CharacterRequestDto createRequest = new CharacterRequestDto(
            "test-wizard",
            "Ezren",
            1,
            8, 12, 12, 15, 13, 10,
            6, 0, 0, 0, 2
        );
        mockMvc.perform(post("/api/characters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // 2. Configure Spellcasting (3 Cantrips, 2 Level 1 spells)
        java.util.List<Integer> maxSlots = java.util.Arrays.asList(3, 2, 0, 0, 0, 0, 0, 0, 0, 0);
        ConfigureSpellcastingRequestDto configRequest = new ConfigureSpellcastingRequestDto(
            1, "INTELLIGENCE", maxSlots
        );

        mockMvc.perform(post("/api/characters/test-wizard/spellcasting")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spellcasting.casterLevel").value(1))
                .andExpect(jsonPath("$.spellcasting.castingAttribute").value("INTELLIGENCE"))
                .andExpect(jsonPath("$.spellcasting.maxSlots[0]").value(3))
                .andExpect(jsonPath("$.spellcasting.remainingSlots[0]").value(3))
                .andExpect(jsonPath("$.spellcasting.maxSlots[1]").value(2))
                .andExpect(jsonPath("$.spellcasting.remainingSlots[1]").value(2))
                .andExpect(jsonPath("$.spellcasting.spellSaveDcs[0]").value(12)) // 10 + 0 + 2 = 12
                .andExpect(jsonPath("$.spellcasting.spellSaveDcs[1]").value(13)); // 10 + 1 + 2 = 13

        // 3. Consume Spell Slot (Level 1)
        mockMvc.perform(post("/api/characters/test-wizard/spellcasting/consume-slot?spellLevel=1")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spellcasting.remainingSlots[1]").value(1));

        // 4. Consume second slot (Level 1)
        mockMvc.perform(post("/api/characters/test-wizard/spellcasting/consume-slot?spellLevel=1")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spellcasting.remainingSlots[1]").value(0));

        // 5. Attempt to consume when remaining slots are 0 -> should throw and get 400 Bad Request
        mockMvc.perform(post("/api/characters/test-wizard/spellcasting/consume-slot?spellLevel=1")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("No spell slots remaining at level 1"));

        // 6. Rest the character and verify slots are restored
        mockMvc.perform(post("/api/characters/test-wizard/rest")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spellcasting.remainingSlots[1]").value(2));
    }
}
