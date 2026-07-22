package com.themis.engine.api.character;

import com.themis.engine.api.character.request.*;
import com.themis.engine.api.character.response.CharacterResponse;
import com.themis.engine.domain.Character;
import com.themis.engine.application.character.CharacterService;
import com.themis.engine.domain.StatType;
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
    public ResponseEntity<CharacterResponse> createCharacter(@Valid @RequestBody CreateCharacterRequest request) {
        Character character = request.toDomain();
        Character saved = characterService.createCharacter(character);
        return ResponseEntity.status(201).body(CharacterResponse.fromDomain(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponse> getCharacter(@PathVariable String id) {
        return characterService.getCharacter(id)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-item")
    public ResponseEntity<CharacterResponse> equipItem(
        @PathVariable String id,
        @Valid @RequestBody EquipItemRequest request
    ) {
        return characterService.equipItem(id, request.toDomain())
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-weapon")
    public ResponseEntity<CharacterResponse> equipWeapon(
        @PathVariable String id,
        @Valid @RequestBody EquipWeaponRequest request
    ) {
        return characterService.equipWeapon(id, request.toDomain())
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-armor")
    public ResponseEntity<CharacterResponse> equipArmor(
        @PathVariable String id,
        @Valid @RequestBody EquipArmorRequest request
    ) {
        return characterService.equipArmor(id, request.toDomain())
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unequip-armor")
    public ResponseEntity<CharacterResponse> unequipArmor(
        @PathVariable String id,
        @RequestParam String armorId
    ) {
        return characterService.unequipArmor(id, armorId)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/apply-condition")
    public ResponseEntity<CharacterResponse> applyCondition(
        @PathVariable String id,
        @Valid @RequestBody ApplyConditionRequest request
    ) {
        return characterService.applyCondition(id, request.toDomain())
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rest")
    public ResponseEntity<CharacterResponse> restCharacter(@PathVariable String id) {
        return characterService.restCharacter(id)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/damage")
    public ResponseEntity<CharacterResponse> damageCharacter(
        @PathVariable String id,
        @RequestParam int amount
    ) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage amount cannot be negative");
        }
        return characterService.damageCharacter(id, amount)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/heal")
    public ResponseEntity<CharacterResponse> healCharacter(
        @PathVariable String id,
        @RequestParam int amount
    ) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing amount cannot be negative");
        }
        return characterService.healCharacter(id, amount)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start-turn")
    public ResponseEntity<CharacterResponse> startTurn(@PathVariable String id) {
        return characterService.startTurn(id)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/consume-action")
    public ResponseEntity<CharacterResponse> consumeAction(
        @PathVariable String id,
        @RequestParam com.themis.engine.domain.ActionType type
    ) {
        return characterService.consumeAction(id, type)
            .map(CharacterResponse::fromDomain)
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

        return characterService.configureSpellcasting(id, request.casterLevel(), attribute, request.maxSlots())
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/spellcasting/consume-slot")
    public ResponseEntity<CharacterResponse> consumeSpellSlot(
        @PathVariable String id,
        @RequestParam int spellLevel
    ) {
        if (spellLevel < 0 || spellLevel > 9) {
            throw new IllegalArgumentException("Spell level must be between 0 and 9");
        }
        return characterService.consumeSpellSlot(id, spellLevel)
            .map(CharacterResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
