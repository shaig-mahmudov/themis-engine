package com.themis.engine.domain;

import java.util.Optional;

/**
 * Outbound port for persisting and retrieving Encounter aggregates.
 */
public interface EncounterStore {
    /**
     * Saves the encounter to the data store.
     */
    Encounter save(Encounter encounter);

    /**
     * Retrieves an encounter by its unique ID.
     */
    Optional<Encounter> findById(String id);
}
