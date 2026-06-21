package com.themis.engine.domain;

import java.util.Optional;

/**
 * Outbound port for persisting and retrieving Character aggregates.
 */
public interface CharacterStore {
    /**
     * Saves the character to the data store.
     */
    Character save(Character character);

    /**
     * Retrieves a character by their unique ID.
     */
    Optional<Character> findById(String id);
}
