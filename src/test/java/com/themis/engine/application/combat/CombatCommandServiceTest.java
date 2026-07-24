package com.themis.engine.application.combat;

import com.themis.engine.application.combat.command.ResolveAttackCommand;
import com.themis.engine.domain.AttackResult;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.RuleEngine;
import com.themis.engine.domain.Weapon;
import com.themis.engine.domain.WeaponType;
import com.themis.engine.domain.DiceRoll;
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

        assertThatThrownBy(() -> service.resolveAttack(new ResolveAttackCommand(
            "missing-attacker", "target", "weapon", 10
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attacker character not found: missing-attacker");

        verify(characterStore, never()).save(any());
    }

    @Test
    void rejectsNullCommand() {
        CombatCommandService service = new CombatCommandService(
            mock(CharacterStore.class), mock(RuleEngine.class), mock(RandomGenerator.class)
        );

        assertThatThrownBy(() -> service.resolveAttack(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Resolve attack command cannot be null");
    }

    @Test
    void resolvesAttackUsingCommandValuesAndSavesBothCharacters() {
        CharacterStore characterStore = mock(CharacterStore.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        RandomGenerator random = mock(RandomGenerator.class);
        Character attacker = character("attacker", "Attacker");
        Character target = character("target", "Target");
        Weapon weapon = new Weapon("weapon", "Longsword", WeaponType.MELEE, java.util.Map.of(), DiceRoll.parse("1d8"), 19, 2);
        attacker.equipWeapon(weapon);
        AttackResult expected = new AttackResult(true, false, 17, 0, 5, "Hit");
        when(characterStore.findById("attacker")).thenReturn(Optional.of(attacker));
        when(characterStore.findById("target")).thenReturn(Optional.of(target));
        when(ruleEngine.resolveAttack(attacker, weapon, target, 14, random)).thenReturn(expected);

        AttackResult result = new CombatCommandService(characterStore, ruleEngine, random)
            .resolveAttack(new ResolveAttackCommand("attacker", "target", "weapon", 14));

        org.assertj.core.api.Assertions.assertThat(result).isSameAs(expected);
        verify(ruleEngine).resolveAttack(attacker, weapon, target, 14, random);
        verify(characterStore).save(attacker);
        verify(characterStore).save(target);
    }

    private Character character(String id, String name) {
        return new Character(id, name, 1, 14, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);
    }
}
