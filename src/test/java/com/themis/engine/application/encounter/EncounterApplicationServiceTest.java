package com.themis.engine.application.encounter;

import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncounterApplicationServiceTest {

    @Test
    void commandServiceCreatesAndSavesEncounterWithoutSpringTransactionProxy() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        when(encounterStore.save(any(Encounter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EncounterCommandService service = new EncounterCommandService(
            encounterStore,
            mock(CharacterStore.class),
            mock(RandomGenerator.class)
        );

        Encounter result = service.createEncounter("Goblin Skirmish");

        assertThat(result.getName()).isEqualTo("Goblin Skirmish");
        verify(encounterStore).save(result);
    }

    @Test
    void commandServiceFailsWithoutSavingWhenEncounterIsMissing() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        when(encounterStore.findById("missing")).thenReturn(Optional.empty());
        EncounterCommandService service = new EncounterCommandService(
            encounterStore,
            mock(CharacterStore.class),
            mock(RandomGenerator.class)
        );

        assertThatThrownBy(() -> service.nextTurn("missing"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Encounter not found: missing");

        verify(encounterStore, never()).save(any(Encounter.class));
    }

    @Test
    void queryServiceReturnsEmptyWhenEncounterIsMissing() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        when(encounterStore.findById("missing")).thenReturn(Optional.empty());

        Optional<Encounter> result = new EncounterQueryService(encounterStore).getEncounter("missing");

        assertThat(result).isEmpty();
    }
}
