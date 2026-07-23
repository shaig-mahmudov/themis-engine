package com.themis.engine.api.encounter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themis.engine.api.character.request.CreateCharacterRequest;
import com.themis.engine.api.encounter.request.AddParticipantRequest;
import com.themis.engine.api.encounter.request.CreateEncounterRequest;
import com.themis.engine.api.encounter.request.StartEncounterRequest;
import com.themis.engine.domain.CombatantType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EncounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullEncounterFlow() throws Exception {
        // 1. Create a character to act as a participant
        CreateCharacterRequest charRequest = new CreateCharacterRequest(
            "char-v",
            "Valeros",
            1,
            10, 14, 10, 10, 10, 10, // Dex Mod is +2
            10, 1, 0, 0, 0
        );

        mockMvc.perform(post("/api/characters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(charRequest)))
                .andExpect(status().isCreated());

        // Consume Standard Action on Valeros to check if nextTurn() resets it later
        mockMvc.perform(post("/api/characters/char-v/consume-action")
                .header("X-API-KEY", "default-dev-key")
                .param("type", "STANDARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnState.standardUsed").value(true));

        // 2. Create Encounter
        CreateEncounterRequest createReq = new CreateEncounterRequest("Goblin Skirmish");
        String encounterJson = mockMvc.perform(post("/api/encounters")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goblin Skirmish"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        String encounterId = objectMapper.readTree(encounterJson).get("id").asText();

        // 3. Add Character Participant
        AddParticipantRequest addCharReq = new AddParticipantRequest(
            "char-v",
            CombatantType.CHARACTER,
            null,
            null
        );
        mockMvc.perform(post("/api/encounters/" + encounterId + "/participants")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addCharReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].combatantId").value("char-v"))
                .andExpect(jsonPath("$.participants[0].name").value("Valeros"))
                .andExpect(jsonPath("$.participants[0].dexterityModifier").value(2));

        // 4. Add Lair Action Participant (non-character)
        AddParticipantRequest addLairReq = new AddParticipantRequest(
            "lair-1",
            CombatantType.LAIR_ACTION,
            "Lair Hazard",
            0
        );
        mockMvc.perform(post("/api/encounters/" + encounterId + "/participants")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addLairReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[1].combatantId").value("lair-1"))
                .andExpect(jsonPath("$.participants[1].name").value("Lair Hazard"));

        // 5. Start Encounter with manual rolls
        // Valeros: Roll 10 -> Total = 10 + 2 = 12
        // Lair Hazard: Roll 15 -> Total = 15 + 0 = 15
        StartEncounterRequest startReq = new StartEncounterRequest(Map.of(
            "char-v", 10,
            "lair-1", 15
        ));

        mockMvc.perform(post("/api/encounters/" + encounterId + "/start")
                .header("X-API-KEY", "default-dev-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(startReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRound").value(1))
                .andExpect(jsonPath("$.participants[0].combatantId").value("lair-1")) // Lair Hazard goes first (15 total)
                .andExpect(jsonPath("$.participants[1].combatantId").value("char-v")) // Valeros goes second (12 total)
                .andExpect(jsonPath("$.activeParticipant.combatantId").value("lair-1"));

        // 6. Get Encounter to verify GET endpoint
        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 7. Advance turn to Valeros. Since Valeros is a character, his turn state should be reset!
        mockMvc.perform(post("/api/encounters/" + encounterId + "/next-turn")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeParticipant.combatantId").value("char-v"));

        // Retrieve Valeros and check that standardUsed has been reset back to false!
        mockMvc.perform(get("/api/characters/char-v")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnState.standardUsed").value(false));

        // 8. End Encounter
        mockMvc.perform(post("/api/encounters/" + encounterId + "/end")
                .header("X-API-KEY", "default-dev-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
