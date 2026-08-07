package com.themis.engine.application.combat;

import com.themis.engine.application.combat.command.ResolveAttackCommand;
import com.themis.engine.domain.AttackResult;
import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.DiceRoll;
import com.themis.engine.domain.Weapon;
import com.themis.engine.domain.WeaponType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CombatCommandServiceRetryTest {

    @Autowired
    private CombatCommandService combatCommandService;

    @MockBean
    private CharacterStore characterStore;

    @MockBean
    private RandomGenerator randomGenerator;

    @Test
    void shouldRetainRandomResultsAcrossRetries() {
        Character attacker = new Character("attacker", "Attacker", 1, 14, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);
        Character target = new Character("target", "Target", 1, 14, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);

        Weapon weapon = new Weapon("weapon", "Longsword", WeaponType.MELEE, Map.of(), DiceRoll.parse("1d8"), 19, 2);
        attacker.equipWeapon(weapon);

        when(characterStore.findById("attacker")).thenAnswer(invocation -> {
            Character freshAttacker = new Character("attacker", "Attacker", 1, 14, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0);
            Weapon w = new Weapon("weapon", "Longsword", WeaponType.MELEE, Map.of(), DiceRoll.parse("1d8"), 19, 2);
            freshAttacker.equipWeapon(w);
            return Optional.of(freshAttacker);
        });
        when(characterStore.findById("target")).thenAnswer(invocation -> {
            return Optional.of(new Character("target", "Target", 1, 14, 10, 10, 10, 10, 10, 10, 1, 0, 0, 0));
        });

        // Mock random behavior
        when(randomGenerator.nextInt(1, 21)).thenReturn(20).thenReturn(19); // attack roll (20), confirm roll (19)
        when(randomGenerator.nextInt(1, 9)).thenReturn(8).thenReturn(7); // damage rolls for 1d8 (crit x2)

        // Make the first save fail with OptimisticLockingFailureException
        when(characterStore.save(any(Character.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException("test", "test"))
            .thenReturn(attacker);

        AttackResult result = combatCommandService.resolveAttack(
            new ResolveAttackCommand("attacker", "target", "weapon", null)
        );

        // Verify that the result uses the exact random sequence generated once
        assertThat(result.isCritical()).isTrue();
        assertThat(result.attackRoll()).isEqualTo(23); // 20 + 1 (bab) + 2 (str modifier)
        assertThat(result.damageDealt()).isEqualTo(19); // 8 + 2 (str modifier) + 7 + 2 (str modifier)

        // Ensure random generator was invoked exactly 4 times in total
        verify(randomGenerator, times(2)).nextInt(1, 21);
        verify(randomGenerator, times(2)).nextInt(1, 9);

        // Ensure save was called three times (1st failed attacker, 2nd succeeded attacker, 3rd succeeded target)
        verify(characterStore, times(3)).save(any(Character.class));
    }
}
