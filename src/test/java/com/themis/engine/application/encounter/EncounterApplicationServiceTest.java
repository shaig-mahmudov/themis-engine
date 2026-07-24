package com.themis.engine.application.encounter;

import com.themis.engine.application.encounter.command.AddParticipantCommand;
import com.themis.engine.application.encounter.command.StartEncounterCommand;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.CombatantType;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterStore;
import com.themis.engine.domain.EncounterStatus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
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

    @Test
    void addParticipantRejectsNullCommand() {
        EncounterCommandService service = new EncounterCommandService(
            mock(EncounterStore.class), mock(CharacterStore.class), mock(RandomGenerator.class)
        );

        assertThatThrownBy(() -> service.addParticipant(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Add participant command cannot be null");
    }

    @Test
    void startEncounterRejectsNullCommand() {
        EncounterCommandService service = new EncounterCommandService(
            mock(EncounterStore.class), mock(CharacterStore.class), mock(RandomGenerator.class)
        );

        assertThatThrownBy(() -> service.startEncounter(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Start encounter command cannot be null");
    }

    @Test
    void addParticipantFailsWithoutSavingWhenEncounterIsMissing() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        when(encounterStore.findById("missing")).thenReturn(Optional.empty());
        EncounterCommandService service = new EncounterCommandService(
            encounterStore, mock(CharacterStore.class), mock(RandomGenerator.class)
        );

        assertThatThrownBy(() -> service.addParticipant(new AddParticipantCommand(
            "missing", "combatant", CombatantType.LAIR_ACTION, "Hazard", 0
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Encounter not found: missing");

        verify(encounterStore, never()).save(any(Encounter.class));
    }

    @Test
    void addParticipantLooksUpCharacterAndUsesItsDexterityModifier() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        CharacterStore characterStore = mock(CharacterStore.class);
        Encounter encounter = new Encounter("encounter-1", "Skirmish");
        Character character = new Character("character-1", "Valeros", 1, 10, 14, 10, 10, 10, 10, 10, 0, 0, 0, 0);
        when(encounterStore.findById("encounter-1")).thenReturn(Optional.of(encounter));
        when(characterStore.findById("character-1")).thenReturn(Optional.of(character));
        when(encounterStore.save(encounter)).thenReturn(encounter);

        Encounter result = new EncounterCommandService(encounterStore, characterStore, mock(RandomGenerator.class))
            .addParticipant(new AddParticipantCommand(
                "encounter-1", "character-1", CombatantType.CHARACTER, "Ignored", 99
            ));

        assertThat(result.getParticipants()).singleElement().satisfies(participant -> {
            assertThat(participant.name()).isEqualTo("Valeros");
            assertThat(participant.dexterityModifier()).isEqualTo(2);
        });
        verify(encounterStore).save(encounter);
    }

    @Test
    void addParticipantCreatesManualCombatant() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        Encounter encounter = new Encounter("encounter-1", "Skirmish");
        when(encounterStore.findById("encounter-1")).thenReturn(Optional.of(encounter));
        when(encounterStore.save(encounter)).thenReturn(encounter);

        Encounter result = new EncounterCommandService(encounterStore, mock(CharacterStore.class), mock(RandomGenerator.class))
            .addParticipant(new AddParticipantCommand(
                "encounter-1", "lair-1", CombatantType.LAIR_ACTION, "Falling Rocks", 4
            ));

        assertThat(result.getParticipants()).singleElement().satisfies(participant -> {
            assertThat(participant.name()).isEqualTo("Falling Rocks");
            assertThat(participant.dexterityModifier()).isEqualTo(4);
        });
    }

    @Test
    void startEncounterUsesDefensivelyCopiedManualRolls() {
        EncounterStore encounterStore = mock(EncounterStore.class);
        Encounter encounter = new Encounter("encounter-1", "Skirmish");
        encounter.addParticipant(new com.themis.engine.domain.EncounterParticipant(
            "lair-1", CombatantType.LAIR_ACTION, "Falling Rocks", null, null, 0
        ));
        Map<String, Integer> rolls = new HashMap<>(Map.of("lair-1", 15));
        StartEncounterCommand command = new StartEncounterCommand("encounter-1", rolls);
        rolls.put("lair-1", 1);
        when(encounterStore.findById("encounter-1")).thenReturn(Optional.of(encounter));
        when(encounterStore.save(encounter)).thenReturn(encounter);

        Encounter result = new EncounterCommandService(encounterStore, mock(CharacterStore.class), mock(RandomGenerator.class))
            .startEncounter(command);

        assertThat(result.getStatus()).isEqualTo(EncounterStatus.ACTIVE);
        assertThat(result.getParticipants()).singleElement().satisfies(participant ->
            assertThat(participant.initiativeRoll()).isEqualTo(15)
        );
        assertThatThrownBy(() -> command.manualRolls().put("new", 10))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
