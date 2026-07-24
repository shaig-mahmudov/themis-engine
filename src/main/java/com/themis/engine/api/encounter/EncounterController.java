package com.themis.engine.api.encounter;

import com.themis.engine.api.encounter.request.AddParticipantRequest;
import com.themis.engine.api.encounter.request.CreateEncounterRequest;
import com.themis.engine.api.encounter.request.StartEncounterRequest;
import com.themis.engine.api.encounter.response.EncounterResponse;
import com.themis.engine.application.encounter.EncounterCommandService;
import com.themis.engine.application.encounter.EncounterQueryService;
import com.themis.engine.application.encounter.command.AddParticipantCommand;
import com.themis.engine.application.encounter.command.StartEncounterCommand;
import com.themis.engine.domain.Encounter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterCommandService encounterCommandService;
    private final EncounterQueryService encounterQueryService;
    private final EncounterApiMapper mapper;

    public EncounterController(
        EncounterCommandService encounterCommandService,
        EncounterQueryService encounterQueryService,
        EncounterApiMapper mapper
    ) {
        this.encounterCommandService = encounterCommandService;
        this.encounterQueryService = encounterQueryService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<EncounterResponse> createEncounter(@Valid @RequestBody CreateEncounterRequest request) {
        Encounter encounter = encounterCommandService.createEncounter(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(encounter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EncounterResponse> getEncounter(@PathVariable String id) {
        return encounterQueryService.getEncounter(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<EncounterResponse> addParticipant(
        @PathVariable String id,
        @Valid @RequestBody AddParticipantRequest request
    ) {
        AddParticipantCommand command = new AddParticipantCommand(
            id,
            request.combatantId(),
            request.combatantType(),
            request.name(),
            request.dexterityModifier()
        );
        Encounter encounter = encounterCommandService.addParticipant(command);
        return ResponseEntity.ok(mapper.toResponse(encounter));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<EncounterResponse> startEncounter(
        @PathVariable String id,
        @RequestBody(required = false) StartEncounterRequest request
    ) {
        StartEncounterCommand command = new StartEncounterCommand(
            id,
            request != null ? request.manualRolls() : null
        );
        Encounter encounter = encounterCommandService.startEncounter(command);
        return ResponseEntity.ok(mapper.toResponse(encounter));
    }

    @PostMapping("/{id}/next-turn")
    public ResponseEntity<EncounterResponse> nextTurn(@PathVariable String id) {
        Encounter encounter = encounterCommandService.nextTurn(id);
        return ResponseEntity.ok(mapper.toResponse(encounter));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<EncounterResponse> endEncounter(@PathVariable String id) {
        Encounter encounter = encounterCommandService.endEncounter(id);
        return ResponseEntity.ok(mapper.toResponse(encounter));
    }
}
