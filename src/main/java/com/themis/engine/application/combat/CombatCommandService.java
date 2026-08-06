package com.themis.engine.application.combat;

import com.themis.engine.application.combat.command.ResolveAttackCommand;
import com.themis.engine.domain.ActionType;
import com.themis.engine.domain.AttackResult;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.RuleEngine;
import com.themis.engine.domain.Weapon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;

import java.util.Objects;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;

import java.util.random.RandomGenerator;
import java.util.List;
import java.util.ArrayList;

/**
 * Service that orchestrates state-changing combat use cases.
 */
@Service
@Transactional
@Retryable(
    retryFor = {
        org.springframework.dao.OptimisticLockingFailureException.class,
        ObjectOptimisticLockingFailureException.class,
        OptimisticLockException.class
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 50, maxDelay = 100, multiplier = 2.0)
)
public class CombatCommandService {

    private final CharacterStore characterStore;
    private final RuleEngine ruleEngine;
    private final RandomGenerator random;

    public CombatCommandService(
        CharacterStore characterStore,
        RuleEngine ruleEngine,
        RandomGenerator random
    ) {
        this.characterStore = characterStore;
        this.ruleEngine = ruleEngine;
        this.random = random;
    }

    private static class ReplayingRandom implements RandomGenerator {
        private final RandomGenerator delegate;
        private final List<Integer> recorded = new ArrayList<>();
        private int index = 0;
        private final boolean replaying;

        public ReplayingRandom(RandomGenerator delegate, boolean replaying) {
            this.delegate = delegate;
            this.replaying = replaying;
        }

        @Override
        public long nextLong() {
            return delegate.nextLong();
        }

        @Override
        public int nextInt(int origin, int bound) {
            if (replaying && index < recorded.size()) {
                return recorded.get(index++);
            }
            int val = delegate.nextInt(origin, bound);
            if (!replaying) {
                recorded.add(val);
            }
            return val;
        }

        public List<Integer> getRecorded() {
            return recorded;
        }

        public void setRecorded(List<Integer> previous) {
            this.recorded.addAll(previous);
        }
    }

    public AttackResult resolveAttack(ResolveAttackCommand command) {
        Objects.requireNonNull(command, "Resolve attack command cannot be null");

        RandomGenerator activeRandom = this.random;
        RetryContext context = RetrySynchronizationManager.getContext();

        if (context != null) {
            List<Integer> previousRolls = (List<Integer>) context.getAttribute("combat.random.rolls");
            if (previousRolls != null) {
                ReplayingRandom replayer = new ReplayingRandom(this.random, true);
                replayer.setRecorded(previousRolls);
                activeRandom = replayer;
            } else {
                ReplayingRandom recorder = new ReplayingRandom(this.random, false);
                context.setAttribute("combat.random.rolls", recorder.getRecorded());
                activeRandom = recorder;
            }
        }

        String attackerId = command.attackerId();
        String targetId = command.targetId();
        String weaponId = command.weaponId();
        Integer d20Roll = command.d20Roll();

        if (attackerId == null || attackerId.isBlank()) {
            throw new IllegalArgumentException("Attacker ID cannot be null or blank");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Target ID cannot be null or blank");
        }
        if (weaponId == null || weaponId.isBlank()) {
            throw new IllegalArgumentException("Weapon ID cannot be null or blank");
        }

        Character attacker = characterStore.findById(attackerId)
            .orElseThrow(() -> new IllegalArgumentException("Attacker character not found: " + attackerId));
        Character target = characterStore.findById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("Target character not found: " + targetId));

        Weapon weapon = attacker.getEquippedWeapons().stream()
            .filter(w -> w.id().equals(weaponId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Weapon " + weaponId + " is not equipped on attacker " + attackerId));

        if (!attacker.getTurnState().canConsume(ActionType.STANDARD)) {
            throw new IllegalStateException("Cannot attack: standard action already consumed in current turn");
        }
        attacker.getTurnState().consume(ActionType.STANDARD);

        int roll = d20Roll != null ? d20Roll : activeRandom.nextInt(1, 21);
        AttackResult result = ruleEngine.resolveAttack(attacker, weapon, target, roll, activeRandom);

        characterStore.save(attacker);
        characterStore.save(target);
        return result;
    }
}
