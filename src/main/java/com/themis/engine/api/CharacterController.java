package com.themis.engine.api;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Spring REST Controller exposing character management operations.
 */
@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @PostMapping
    public ResponseEntity<CharacterResponseDto> createCharacter(@Valid @RequestBody CharacterRequestDto request) {
        Character character = request.toDomain();
        Character saved = characterService.createCharacter(character);
        return ResponseEntity.status(201).body(CharacterResponseDto.fromDomain(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponseDto> getCharacter(@PathVariable String id) {
        return characterService.getCharacter(id)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-item")
    public ResponseEntity<CharacterResponseDto> equipItem(
        @PathVariable String id,
        @Valid @RequestBody EquipItemRequestDto request
    ) {
        return characterService.equipItem(id, request.toDomain())
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-weapon")
    public ResponseEntity<CharacterResponseDto> equipWeapon(
        @PathVariable String id,
        @Valid @RequestBody EquipWeaponRequestDto request
    ) {
        return characterService.equipWeapon(id, request.toDomain())
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-armor")
    public ResponseEntity<CharacterResponseDto> equipArmor(
        @PathVariable String id,
        @Valid @RequestBody EquipArmorRequestDto request
    ) {
        return characterService.equipArmor(id, request.toDomain())
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unequip-armor")
    public ResponseEntity<CharacterResponseDto> unequipArmor(
        @PathVariable String id,
        @RequestParam String armorId
    ) {
        return characterService.unequipArmor(id, armorId)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/apply-condition")
    public ResponseEntity<CharacterResponseDto> applyCondition(
        @PathVariable String id,
        @Valid @RequestBody ApplyConditionRequestDto request
    ) {
        return characterService.applyCondition(id, request.toDomain())
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rest")
    public ResponseEntity<CharacterResponseDto> restCharacter(@PathVariable String id) {
        return characterService.restCharacter(id)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/damage")
    public ResponseEntity<CharacterResponseDto> damageCharacter(
        @PathVariable String id,
        @RequestParam int amount
    ) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage amount cannot be negative");
        }
        return characterService.damageCharacter(id, amount)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/heal")
    public ResponseEntity<CharacterResponseDto> healCharacter(
        @PathVariable String id,
        @RequestParam int amount
    ) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing amount cannot be negative");
        }
        return characterService.healCharacter(id, amount)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start-turn")
    public ResponseEntity<CharacterResponseDto> startTurn(@PathVariable String id) {
        return characterService.startTurn(id)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/consume-action")
    public ResponseEntity<CharacterResponseDto> consumeAction(
        @PathVariable String id,
        @RequestParam com.themis.engine.domain.ActionType type
    ) {
        return characterService.consumeAction(id, type)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
