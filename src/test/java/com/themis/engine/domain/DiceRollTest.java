package com.themis.engine.domain;

import org.junit.jupiter.api.Test;
import java.util.random.RandomGenerator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiceRollTest {

    @Test
    void testParsing() {
        var roll1 = DiceRoll.parse("1d8+4");
        assertEquals(1, roll1.numberOfDice());
        assertEquals(8, roll1.sides());
        assertEquals(4, roll1.modifier());
        assertEquals("1d8+4", roll1.toString());

        var roll2 = DiceRoll.parse("2d6");
        assertEquals(2, roll2.numberOfDice());
        assertEquals(6, roll2.sides());
        assertEquals(0, roll2.modifier());
        assertEquals("2d6", roll2.toString());

        var roll3 = DiceRoll.parse("d20-1");
        assertEquals(1, roll3.numberOfDice());
        assertEquals(20, roll3.sides());
        assertEquals(-1, roll3.modifier());
        assertEquals("1d20-1", roll3.toString());
    }

    @Test
    void testInvalidParsing() {
        assertThrows(IllegalArgumentException.class, () -> DiceRoll.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> DiceRoll.parse("1d"));
        assertThrows(IllegalArgumentException.class, () -> DiceRoll.parse("d"));
    }

    @Test
    void testRollWithIntSupplier() {
        var roll = new DiceRoll(2, 6, 3); // 2d6+3
        int result = roll.roll(() -> 4);
        assertEquals(4 + 4 + 3, result);
    }

    @Test
    void testRollWithRandomGenerator() {
        var roll = new DiceRoll(1, 8, 2);
        var mockRng = mock(RandomGenerator.class);
        when(mockRng.nextInt(1, 9)).thenReturn(5);

        int result = roll.roll(mockRng);
        assertEquals(5 + 2, result);
        verify(mockRng).nextInt(1, 9);
    }
}
