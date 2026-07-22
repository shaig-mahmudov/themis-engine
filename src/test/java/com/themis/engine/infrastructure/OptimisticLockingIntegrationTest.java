package com.themis.engine.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OptimisticLockingIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void staleCharacterWriteIsRejected() {
        persistCharacter("optimistic-character");

        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();

        try {
            firstManager.getTransaction().begin();
            staleManager.getTransaction().begin();

            CharacterEntity first = firstManager.find(CharacterEntity.class, "optimistic-character");
            CharacterEntity stale = staleManager.find(CharacterEntity.class, "optimistic-character");

            first.setName("First writer");
            firstManager.flush();
            firstManager.getTransaction().commit();

            stale.setName("Stale writer");
            assertThrows(OptimisticLockException.class, staleManager::flush);
        } finally {
            rollbackIfActive(firstManager);
            rollbackIfActive(staleManager);
            firstManager.close();
            staleManager.close();
        }
    }

    private void persistCharacter(String id) {
        EntityManager manager = entityManagerFactory.createEntityManager();
        try {
            manager.getTransaction().begin();
            manager.persist(characterEntity(id));
            manager.getTransaction().commit();
        } finally {
            rollbackIfActive(manager);
            manager.close();
        }
    }

    private CharacterEntity characterEntity(String id) {
        CharacterEntity entity = new CharacterEntity();
        entity.setId(id);
        entity.setName("Initial writer");
        entity.setLevel(1);
        entity.setBaseStr(10);
        entity.setBaseDex(10);
        entity.setBaseCon(10);
        entity.setBaseInt(10);
        entity.setBaseWis(10);
        entity.setBaseCha(10);
        entity.setBaseHitPoints(8);
        entity.setBaseAttackBonus(0);
        entity.setBaseFortitude(0);
        entity.setBaseReflex(0);
        entity.setBaseWill(0);
        entity.setCurrentDamage(0);
        entity.setStandardUsed(false);
        entity.setMoveUsed(false);
        entity.setSwiftUsed(false);
        return entity;
    }

    private void rollbackIfActive(EntityManager manager) {
        if (manager.getTransaction().isActive()) {
            manager.getTransaction().rollback();
        }
    }
}
