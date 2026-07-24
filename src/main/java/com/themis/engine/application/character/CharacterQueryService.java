package com.themis.engine.application.character;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service that orchestrates read-only character use cases.
 */
@Service
@Transactional(readOnly = true)
public class CharacterQueryService {

    private final CharacterStore characterStore;

    public CharacterQueryService(CharacterStore characterStore) {
        this.characterStore = characterStore;
    }

    public Optional<Character> getCharacter(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Character ID cannot be null or blank");
        }
        return characterStore.findById(id);
    }
}
