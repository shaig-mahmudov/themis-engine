package com.themis.engine.infrastructure;

import com.themis.engine.domain.CombatantType;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import com.themis.engine.domain.EncounterStatus;
import com.themis.engine.domain.EncounterStore;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresEncounterRepositoryAdapter implements EncounterStore {

    private final EncounterJpaRepository repository;

    public PostgresEncounterRepositoryAdapter(EncounterJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    @CachePut(value = "encounters", key = "#encounter.id")
    public Encounter save(Encounter encounter) {
        if (encounter == null) {
            throw new IllegalArgumentException("Encounter cannot be null");
        }
        EncounterEntity entity = toEntity(encounter);
        EncounterEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "encounters", key = "#id")
    public Optional<Encounter> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be null or blank");
        }
        return repository.findById(id).map(this::toDomain);
    }

    private EncounterEntity toEntity(Encounter domain) {
        EncounterEntity entity = new EncounterEntity();
        entity.setId(domain.getId());
        entity.setVersion(domain.getVersion());
        entity.setName(domain.getName());
        entity.setStatus(domain.getStatus().name());
        entity.setCurrentRound(domain.getCurrentRound());
        entity.setActiveParticipantIndex(domain.getActiveParticipantIndex());

        List<EncounterParticipantEntity> participantEntities = new ArrayList<>();
        for (EncounterParticipant p : domain.getParticipants()) {
            participantEntities.add(new EncounterParticipantEntity(
                domain.getId(),
                p.combatantId(),
                p.combatantType().name(),
                p.name(),
                p.initiativeRoll(),
                p.initiativeTotal(),
                p.dexterityModifier()
            ));
        }
        entity.setParticipants(participantEntities);

        return entity;
    }

    private Encounter toDomain(EncounterEntity entity) {
        List<EncounterParticipant> domainParticipants = new ArrayList<>();
        for (EncounterParticipantEntity p : entity.getParticipants()) {
            domainParticipants.add(new EncounterParticipant(
                p.getCombatantId(),
                CombatantType.valueOf(p.getCombatantType()),
                p.getName(),
                p.getInitiativeRoll(),
                p.getInitiativeTotal(),
                p.getDexterityModifier()
            ));
        }

        Encounter encounter = new Encounter(
            entity.getId(),
            entity.getName(),
            EncounterStatus.valueOf(entity.getStatus()),
            entity.getCurrentRound(),
            entity.getActiveParticipantIndex(),
            domainParticipants
        );
        encounter.restoreVersion(entity.getVersion());
        return encounter;
    }
}
