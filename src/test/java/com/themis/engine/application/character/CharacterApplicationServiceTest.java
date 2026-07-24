package com.themis.engine.application.character;

import com.themis.engine.application.character.command.ConfigureSpellcastingCommand;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.StatType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CharacterApplicationServiceTest {

    @Test
    void commandServiceUpdatesAndSavesCharacterWithoutSpringTransactionProxy() {
        CharacterStore characterStore = mock(CharacterStore.class);
        Character character = character("character-1");
        when(characterStore.findById("character-1")).thenReturn(Optional.of(character));
        when(characterStore.save(character)).thenReturn(character);

        CharacterCommandService service = new CharacterCommandService(characterStore);

        Optional<Character> result = service.damageCharacter("character-1", 3);

        assertThat(result).containsSame(character);
        assertThat(character.getCurrentHitPoints()).isEqualTo(7);
        verify(characterStore).save(character);
    }

    @Test
    void commandServiceReturnsEmptyAndDoesNotSaveWhenCharacterIsMissing() {
        CharacterStore characterStore = mock(CharacterStore.class);
        when(characterStore.findById("missing")).thenReturn(Optional.empty());

        Optional<Character> result = new CharacterCommandService(characterStore).healCharacter("missing", 2);

        assertThat(result).isEmpty();
        verify(characterStore, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void queryServiceReturnsEmptyWhenCharacterIsMissing() {
        CharacterStore characterStore = mock(CharacterStore.class);
        when(characterStore.findById("missing")).thenReturn(Optional.empty());

        Optional<Character> result = new CharacterQueryService(characterStore).getCharacter("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void queryServiceRejectsBlankCharacterId() {
        CharacterStore characterStore = mock(CharacterStore.class);

        assertThatThrownBy(() -> new CharacterQueryService(characterStore).getCharacter(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Character ID cannot be null or blank");
    }

    @Test
    void configureSpellcastingRejectsNullCommand() {
        CharacterCommandService service = new CharacterCommandService(mock(CharacterStore.class));

        assertThatThrownBy(() -> service.configureSpellcasting(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Configure spellcasting command cannot be null");
    }

    @Test
    void configureSpellcastingRejectsNullCastingAttribute() {
        CharacterCommandService service = new CharacterCommandService(mock(CharacterStore.class));

        assertThatThrownBy(() -> service.configureSpellcasting(new ConfigureSpellcastingCommand(
            "character-1", 1, null, List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Casting attribute cannot be null");
    }

    @Test
    void configureSpellcastingRejectsInvalidSlotListSize() {
        CharacterCommandService service = new CharacterCommandService(mock(CharacterStore.class));

        assertThatThrownBy(() -> service.configureSpellcasting(new ConfigureSpellcastingCommand(
            "character-1", 1, StatType.INTELLIGENCE, List.of(0, 0, 0, 0, 0, 0, 0, 0, 0)
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Max slots must have exactly 10 elements");
    }

    @Test
    void configureSpellcastingConfiguresAndSavesCharacter() {
        CharacterStore characterStore = mock(CharacterStore.class);
        Character character = character("character-1");
        when(characterStore.findById("character-1")).thenReturn(Optional.of(character));
        when(characterStore.save(character)).thenReturn(character);

        Optional<Character> result = new CharacterCommandService(characterStore).configureSpellcasting(
            new ConfigureSpellcastingCommand(
                "character-1", 3, StatType.INTELLIGENCE, List.of(3, 2, 0, 0, 0, 0, 0, 0, 0, 0)
            )
        );

        assertThat(result).containsSame(character);
        assertThat(character.getSpellcastingFeature().getCasterLevel()).isEqualTo(3);
        assertThat(character.getSpellcastingFeature().getMaxSlots(0)).isEqualTo(3);
        assertThat(character.getSpellcastingFeature().getRemainingSlots(1)).isEqualTo(2);
        verify(characterStore).save(character);
    }

    @Test
    void configureSpellcastingCommandDefensivelyCopiesSlots() {
        List<Integer> slots = new ArrayList<>(List.of(3, 2, 0, 0, 0, 0, 0, 0, 0, 0));
        ConfigureSpellcastingCommand command = new ConfigureSpellcastingCommand(
            "character-1", 1, StatType.INTELLIGENCE, slots
        );

        slots.set(0, 9);

        assertThat(command.maxSlots()).containsExactly(3, 2, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThatThrownBy(() -> command.maxSlots().set(0, 9))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private Character character(String id) {
        return new Character(id, "Valeros", 1, 10, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);
    }
}
