package com.themis.engine.application.encounter;

import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service that orchestrates read-only encounter use cases.
 */
@Service
@Transactional(readOnly = true)
public class EncounterQueryService {

    private final EncounterStore encounterStore;

    public EncounterQueryService(EncounterStore encounterStore) {
        this.encounterStore = encounterStore;
    }

    public Optional<Encounter> getEncounter(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be null or blank");
        }
        return encounterStore.findById(id);
    }
}
