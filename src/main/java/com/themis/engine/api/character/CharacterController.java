package com.themis.engine.api.character;

import com.themis.engine.api.character.request.ApplyConditionRequest;
import com.themis.engine.api.character.request.ConfigureSpellcastingRequest;
import com.themis.engine.api.character.request.CreateCharacterRequest;
import com.themis.engine.api.character.request.EquipArmorRequest;
import com.themis.engine.api.character.request.EquipItemRequest;
import com.themis.engine.api.character.request.EquipWeaponRequest;
import com.themis.engine.api.character.response.CharacterResponse;
import com.themis.engine.application.character.CharacterCommandService;
import com.themis.engine.application.character.CharacterQueryService;
import com.themis.engine.domain.ActionType;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.StatType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring REST Controller exposing character management operations.
 */
@RestController
@RequestMapping("/api/characters")
@Validated
public class CharacterController {

    private final CharacterCommandService characterCommandService;
    private final CharacterQueryService characterQueryService;
    private final CharacterApiMapper mapper;

    public CharacterController(
        CharacterCommandService characterCommandService,
        CharacterQueryService characterQueryService,
        CharacterApiMapper mapper
    ) {
        this.characterCommandService = characterCommandService;
        this.characterQueryService = characterQueryService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<CharacterResponse> createCharacter(@Valid @RequestBody CreateCharacterRequest request) {
        Character character = mapper.toDomain(request);
        Character saved = characterCommandService.createCharacter(character);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponse> getCharacter(@PathVariable String id) {
        return characterQueryService.getCharacter(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-item")
    public ResponseEntity<CharacterResponse> equipItem(
        @PathVariable String id,
        @Valid @RequestBody EquipItemRequest request
    ) {
        return characterCommandService.equipItem(id, mapper.toDomain(request))
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-weapon")
    public ResponseEntity<CharacterResponse> equipWeapon(
        @PathVariable String id,
        @Valid @RequestBody EquipWeaponRequest request
    ) {
        return characterCommandService.equipWeapon(id, mapper.toDomain(request))
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-armor")
    public ResponseEntity<CharacterResponse> equipArmor(
        @PathVariable String id,
        @Valid @RequestBody EquipArmorRequest request
    ) {
        return characterCommandService.equipArmor(id, mapper.toDomain(request))
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unequip-armor")
    public ResponseEntity<CharacterResponse> unequipArmor(
        @PathVariable String id,
        @RequestParam String armorId
    ) {
        return characterCommandService.unequipArmor(id, armorId)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/apply-condition")
    public ResponseEntity<CharacterResponse> applyCondition(
        @PathVariable String id,
        @Valid @RequestBody ApplyConditionRequest request
    ) {
        return characterCommandService.applyCondition(id, mapper.toDomain(request))
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rest")
    public ResponseEntity<CharacterResponse> restCharacter(@PathVariable String id) {
        return characterCommandService.restCharacter(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/damage")
    public ResponseEntity<CharacterResponse> damageCharacter(
        @PathVariable String id,
        @RequestParam @PositiveOrZero(message = "Damage amount cannot be negative") int amount
    ) {
        return characterCommandService.damageCharacter(id, amount)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/heal")
    public ResponseEntity<CharacterResponse> healCharacter(
        @PathVariable String id,
        @RequestParam @PositiveOrZero(message = "Healing amount cannot be negative") int amount
    ) {
        return characterCommandService.healCharacter(id, amount)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start-turn")
    public ResponseEntity<CharacterResponse> startTurn(@PathVariable String id) {
        return characterCommandService.startTurn(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/consume-action")
    public ResponseEntity<CharacterResponse> consumeAction(
        @PathVariable String id,
        @RequestParam ActionType type
    ) {
        return characterCommandService.consumeAction(id, type)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/spellcasting")
    public ResponseEntity<CharacterResponse> configureSpellcasting(
        @PathVariable String id,
        @Valid @RequestBody ConfigureSpellcastingRequest request
    ) {
        StatType attribute;
        try {
            attribute = StatType.valueOf(request.castingAttribute().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid casting attribute: " + request.castingAttribute());
        }

        return characterCommandService.configureSpellcasting(id, request.casterLevel(), attribute, request.maxSlots())
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/spellcasting/consume-slot")
    public ResponseEntity<CharacterResponse> consumeSpellSlot(
        @PathVariable String id,
        @RequestParam @Min(value = 0, message = "Spell level must be between 0 and 9") @Max(value = 9, message = "Spell level must be between 0 and 9") int spellLevel
    ) {
        return characterCommandService.consumeSpellSlot(id, spellLevel)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
