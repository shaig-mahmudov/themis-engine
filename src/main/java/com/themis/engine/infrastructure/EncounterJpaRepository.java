package com.themis.engine.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EncounterJpaRepository extends JpaRepository<EncounterEntity, String> {
}
