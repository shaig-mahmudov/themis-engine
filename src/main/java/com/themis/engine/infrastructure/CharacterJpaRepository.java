package com.themis.engine.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for CharacterEntity.
 */
@Repository
public interface CharacterJpaRepository extends JpaRepository<CharacterEntity, String> {
}
