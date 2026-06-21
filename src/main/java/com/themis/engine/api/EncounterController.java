package com.themis.engine.api;

import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterService;
import com.themis.engine.domain.EncounterStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterService encounterService;
    private final EncounterStore encounterStore;

    public EncounterController(EncounterService encounterService, EncounterStore encounterStore) {
        this.encounterService = encounterService;
        this.encounterStore = encounterStore;
    }

    @PostMapping
    public ResponseEntity<EncounterResponseDto> createEncounter(@Valid @RequestBody CreateEncounterRequest request) {
        Encounter encounter = encounterService.createEncounter(request.name());
        return ResponseEntity.status(201).body(EncounterResponseDto.fromDomain(encounter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EncounterResponseDto> getEncounter(@PathVariable String id) {
        return encounterStore.findById(id)
            .map(EncounterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<EncounterResponseDto> addParticipant(
        @PathVariable String id,
        @Valid @RequestBody AddParticipantRequest request
    ) {
        Encounter encounter = encounterService.addParticipant(
            id,
            request.combatantId(),
            request.combatantType(),
            request.name(),
            request.dexterityModifier()
        );
        return ResponseEntity.ok(EncounterResponseDto.fromDomain(encounter));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<EncounterResponseDto> startEncounter(
        @PathVariable String id,
        @RequestBody(required = false) StartEncounterRequest request
    ) {
        java.util.Map<String, Integer> manualRolls = request != null ? request.manualRolls() : null;
        Encounter encounter = encounterService.startEncounter(id, manualRolls);
        return ResponseEntity.ok(EncounterResponseDto.fromDomain(encounter));
    }

    @PostMapping("/{id}/next-turn")
    public ResponseEntity<EncounterResponseDto> nextTurn(@PathVariable String id) {
        Encounter encounter = encounterService.nextTurn(id);
        return ResponseEntity.ok(EncounterResponseDto.fromDomain(encounter));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<EncounterResponseDto> endEncounter(@PathVariable String id) {
        Encounter encounter = encounterService.endEncounter(id);
        return ResponseEntity.ok(EncounterResponseDto.fromDomain(encounter));
    }
}
