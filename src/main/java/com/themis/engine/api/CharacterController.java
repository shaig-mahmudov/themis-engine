package com.themis.engine.api;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.Condition;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.Weapon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Spring REST Controller exposing character management operations.
 */
@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterStore characterStore;

    public CharacterController(CharacterStore characterStore) {
        this.characterStore = characterStore;
    }

    @PostMapping
    public ResponseEntity<CharacterResponseDto> createCharacter(@RequestBody CharacterRequestDto request) {
        Character character = request.toDomain();
        Character saved = characterStore.save(character);
        return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponseDto> getCharacter(@PathVariable String id) {
        return characterStore.findById(id)
            .map(CharacterResponseDto::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-item")
    public ResponseEntity<CharacterResponseDto> equipItem(@PathVariable String id, @RequestBody EquippableItem item) {
        return characterStore.findById(id)
            .map(c -> {
                c.equip(item);
                Character saved = characterStore.save(c);
                return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/equip-weapon")
    public ResponseEntity<CharacterResponseDto> equipWeapon(@PathVariable String id, @RequestBody Weapon weapon) {
        return characterStore.findById(id)
            .map(c -> {
                c.equipWeapon(weapon);
                Character saved = characterStore.save(c);
                return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/apply-condition")
    public ResponseEntity<CharacterResponseDto> applyCondition(@PathVariable String id, @RequestBody Condition condition) {
        return characterStore.findById(id)
            .map(c -> {
                c.applyCondition(condition);
                Character saved = characterStore.save(c);
                return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rest")
    public ResponseEntity<CharacterResponseDto> restCharacter(@PathVariable String id) {
        return characterStore.findById(id)
            .map(c -> {
                c.getTurnState().reset();
                if (c.getSpellcastingFeature() != null) {
                    c.getSpellcastingFeature().restAndRecover();
                }
                c.heal(c.getMaxHitPoints()); // Full heal during rest
                Character saved = characterStore.save(c);
                return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/damage")
    public ResponseEntity<CharacterResponseDto> damageCharacter(@PathVariable String id, @RequestParam int amount) {
        return characterStore.findById(id)
            .map(c -> {
                c.damage(amount);
                Character saved = characterStore.save(c);
                return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/heal")
    public ResponseEntity<CharacterResponseDto> healCharacter(@PathVariable String id, @RequestParam int amount) {
        return characterStore.findById(id)
            .map(c -> {
                c.heal(amount);
                Character saved = characterStore.save(c);
                return ResponseEntity.ok(CharacterResponseDto.fromDomain(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
