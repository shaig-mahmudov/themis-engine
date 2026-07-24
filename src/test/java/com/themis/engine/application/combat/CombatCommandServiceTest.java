package com.themis.engine.application.combat;

import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.RuleEngine;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CombatCommandServiceTest {

    @Test
    void failsWithoutSavingWhenAttackerIsMissing() {
        CharacterStore characterStore = mock(CharacterStore.class);
        when(characterStore.findById("missing-attacker")).thenReturn(Optional.empty());
        CombatCommandService service = new CombatCommandService(
            characterStore,
            mock(RuleEngine.class),
            mock(RandomGenerator.class)
        );

        assertThatThrownBy(() -> service.resolveAttack("missing-attacker", "target", "weapon", 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attacker character not found: missing-attacker");

        verify(characterStore, never()).save(any());
    }
}
