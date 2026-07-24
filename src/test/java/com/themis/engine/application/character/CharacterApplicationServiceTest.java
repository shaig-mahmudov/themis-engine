package com.themis.engine.application.character;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import org.junit.jupiter.api.Test;

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

    private Character character(String id) {
        return new Character(id, "Valeros", 1, 10, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);
    }
}
