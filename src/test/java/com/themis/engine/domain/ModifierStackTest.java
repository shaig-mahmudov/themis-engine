package com.themis.engine.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModifierStackTest {

    private ModifierStack stack;

    @BeforeEach
    void setUp() {
        stack = new ModifierStack();
    }

    @Test
    void testEmptyStackReturnsZero() {
        assertEquals(0, stack.getTotal());
    }

    @Test
    void testSingleModifier() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, "Amulet of Natural Armor"));
        assertEquals(2, stack.getTotal());
    }

    @Test
    void testDodgeModifiersStack() {
        stack.add(new Modifier(1, ModifierType.DODGE, "Dodge Feat"));
        stack.add(new Modifier(2, ModifierType.DODGE, "Haste Spell"));
        assertEquals(3, stack.getTotal());
    }

    @Test
    void testUntypedModifiersStack() {
        stack.add(new Modifier(2, ModifierType.UNTYPED, "Rage"));
        stack.add(new Modifier(3, ModifierType.UNTYPED, "Some Circumstance"));
        assertEquals(5, stack.getTotal());
    }

    @Test
    void testNonStackableModifiersTakeMax() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, "Magic Weapon +2"));
        stack.add(new Modifier(4, ModifierType.ENHANCEMENT, "Magic Weapon +4"));
        assertEquals(4, stack.getTotal());
    }

    @Test
    void testNonStackableModifiersDifferentTypesBothApply() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, "Belt of Giant Strength"));
        stack.add(new Modifier(3, ModifierType.MORALE, "Rage"));
        assertEquals(5, stack.getTotal());
    }

    @Test
    void testMixedStackableAndNonStackable() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, "Belt +2"));
        stack.add(new Modifier(1, ModifierType.ENHANCEMENT, "Gloves +1")); // overridden
        stack.add(new Modifier(1, ModifierType.DODGE, "Feat"));
        stack.add(new Modifier(2, ModifierType.DODGE, "Haste")); // stacks with Feat -> +3
        stack.add(new Modifier(3, ModifierType.UNTYPED, "Misc")); // stacks -> +3
        
        // Expected: 2 (Enhancement) + 3 (Dodge) + 3 (Untyped) = 8
        assertEquals(8, stack.getTotal());
    }

    @Test
    void testPenaltiesAlwaysStackIfUntypedOrDodge() {
        stack.add(new Modifier(-2, ModifierType.UNTYPED, "Sickness"));
        stack.add(new Modifier(-3, ModifierType.UNTYPED, "Fatigue"));
        assertEquals(-5, stack.getTotal());
    }

    @Test
    void testNonStackablePenaltiesTakeMin() {
        // For non-stackable types, multiple penalties do not stack; only the worst (most negative) applies.
        stack.add(new Modifier(-2, ModifierType.MORALE, "Fear"));
        stack.add(new Modifier(-4, ModifierType.MORALE, "Dread"));
        assertEquals(-4, stack.getTotal());
    }

    @Test
    void testBonusAndPenaltyOfSameNonStackableTypeBothApply() {
        // A bonus and a penalty of the same type do not overwrite each other. They net out.
        stack.add(new Modifier(4, ModifierType.MORALE, "Heroism"));
        stack.add(new Modifier(-2, ModifierType.MORALE, "Shaken"));
        // Expected: +4 Morale bonus and -2 Morale penalty both apply -> +2
        assertEquals(2, stack.getTotal());
    }

    @Test
    void testRemoveModifier() {
        Modifier mod1 = new Modifier(2, ModifierType.ENHANCEMENT, "Belt +2");
        Modifier mod2 = new Modifier(1, ModifierType.DODGE, "Feat");
        stack.add(mod1);
        stack.add(mod2);
        assertEquals(3, stack.getTotal());

        stack.remove(mod1);
        assertEquals(1, stack.getTotal());
    }

    @Test
    void testCannotAddNullModifier() {
        assertThrows(IllegalArgumentException.class, () -> stack.add(null));
    }
}
