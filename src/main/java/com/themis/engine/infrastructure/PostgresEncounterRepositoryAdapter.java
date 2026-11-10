package com.themis.engine.infrastructure;

import com.themis.engine.domain.CombatantType;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import com.themis.engine.domain.EncounterStatus;
import com.themis.engine.domain.EncounterStore;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresEncounterRepositoryAdapter implements EncounterStore {

    private final EncounterJpaRepository repository;
    private final CacheManager cacheManager;

    public PostgresEncounterRepositoryAdapter(EncounterJpaRepository repository) {
        this(repository, null);
    }

    @Autowired
    public PostgresEncounterRepositoryAdapter(EncounterJpaRepository repository, CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    @CachePut(value = "encounters", key = "#encounter.id")
    public Encounter save(Encounter encounter) {
        if (encounter == null) {
            throw new IllegalArgumentException("Encounter cannot be null");
        }
        EncounterEntity entity = toEntity(encounter);
        try {
            EncounterEntity savedEntity = repository.saveAndFlush(entity);
            return toDomain(savedEntity);
        } catch (OptimisticLockingFailureException | OptimisticLockException ex) {
            evictEncounterCache(encounter.getId());
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "encounters", key = "#id", unless = "#result == null")
    public Optional<Encounter> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be null or blank");
        }
        return repository.findById(id).map(this::toDomain);
    }

    private void evictEncounterCache(String encounterId) {
        if (cacheManager != null && encounterId != null) {
            Cache cache = cacheManager.getCache("encounters");
            if (cache != null) {
                cache.evictIfPresent(encounterId);
            }
        }
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
