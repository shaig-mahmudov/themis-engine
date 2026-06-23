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
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("amulet", "Amulet of Natural Armor", SourceType.ITEM)));
        assertEquals(2, stack.getTotal());
    }

    @Test
    void testDodgeModifiersStack() {
        stack.add(new Modifier(1, ModifierType.DODGE, new ModifierSource("dodge-feat", "Dodge Feat", SourceType.FEAT)));
        stack.add(new Modifier(2, ModifierType.DODGE, new ModifierSource("haste-spell", "Haste Spell", SourceType.SPELL)));
        assertEquals(3, stack.getTotal());
    }

    @Test
    void testUntypedModifiersStack() {
        stack.add(new Modifier(2, ModifierType.UNTYPED, new ModifierSource("rage", "Rage", SourceType.SPELL)));
        stack.add(new Modifier(3, ModifierType.UNTYPED, new ModifierSource("some-circumstance", "Some Circumstance", SourceType.GENERIC)));
        assertEquals(5, stack.getTotal());
    }

    @Test
    void testNonStackableModifiersTakeMax() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("magic-weapon-2", "Magic Weapon +2", SourceType.ITEM)));
        stack.add(new Modifier(4, ModifierType.ENHANCEMENT, new ModifierSource("magic-weapon-4", "Magic Weapon +4", SourceType.ITEM)));
        assertEquals(4, stack.getTotal());
    }

    @Test
    void testNonStackableModifiersDifferentTypesBothApply() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("belt-giant-strength", "Belt of Giant Strength", SourceType.ITEM)));
        stack.add(new Modifier(3, ModifierType.MORALE, new ModifierSource("rage", "Rage", SourceType.SPELL)));
        assertEquals(5, stack.getTotal());
    }

    @Test
    void testMixedStackableAndNonStackable() {
        stack.add(new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("belt-2", "Belt +2", SourceType.ITEM)));
        stack.add(new Modifier(1, ModifierType.ENHANCEMENT, new ModifierSource("gloves-1", "Gloves +1", SourceType.ITEM))); // overridden
        stack.add(new Modifier(1, ModifierType.DODGE, new ModifierSource("feat", "Feat", SourceType.FEAT)));
        stack.add(new Modifier(2, ModifierType.DODGE, new ModifierSource("haste", "Haste", SourceType.SPELL))); // stacks with Feat -> +3
        stack.add(new Modifier(3, ModifierType.UNTYPED, new ModifierSource("misc", "Misc", SourceType.GENERIC))); // stacks -> +3
        
        // Expected: 2 (Enhancement) + 3 (Dodge) + 3 (Untyped) = 8
        assertEquals(8, stack.getTotal());
    }

    @Test
    void testPenaltiesAlwaysStackIfUntypedOrDodge() {
        stack.add(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("sickness", "Sickness", SourceType.CONDITION)));
        stack.add(new Modifier(-3, ModifierType.UNTYPED, new ModifierSource("fatigue", "Fatigue", SourceType.CONDITION)));
        assertEquals(-5, stack.getTotal());
    }

    @Test
    void testNonStackablePenaltiesTakeMin() {
        // For non-stackable types, multiple penalties do not stack; only the worst (most negative) applies.
        stack.add(new Modifier(-2, ModifierType.MORALE, new ModifierSource("fear", "Fear", SourceType.CONDITION)));
        stack.add(new Modifier(-4, ModifierType.MORALE, new ModifierSource("dread", "Dread", SourceType.CONDITION)));
        assertEquals(-4, stack.getTotal());
    }

    @Test
    void testBonusAndPenaltyOfSameNonStackableTypeBothApply() {
        // A bonus and a penalty of the same type do not overwrite each other. They net out.
        stack.add(new Modifier(4, ModifierType.MORALE, new ModifierSource("heroism", "Heroism", SourceType.SPELL)));
        stack.add(new Modifier(-2, ModifierType.MORALE, new ModifierSource("shaken", "Shaken", SourceType.CONDITION)));
        // Expected: +4 Morale bonus and -2 Morale penalty both apply -> +2
        assertEquals(2, stack.getTotal());
    }

    @Test
    void testRemoveModifier() {
        Modifier mod1 = new Modifier(2, ModifierType.ENHANCEMENT, new ModifierSource("belt-2", "Belt +2", SourceType.ITEM));
        Modifier mod2 = new Modifier(1, ModifierType.DODGE, new ModifierSource("feat", "Feat", SourceType.FEAT));
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

    @Test
    void testModifiersFromSameSourceDoNotStack() {
        stack.add(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("sickened", "Sickened", SourceType.CONDITION)));
        stack.add(new Modifier(-2, ModifierType.UNTYPED, new ModifierSource("sickened", "Sickened", SourceType.CONDITION)));
        // Since they have the same source "Sickened", they should not stack, even though they are UNTYPED.
        assertEquals(-2, stack.getTotal());
    }

    @Test
    void testDodgeModifiersFromSameSourceDoNotStack() {
        stack.add(new Modifier(1, ModifierType.DODGE, new ModifierSource("haste", "Haste", SourceType.SPELL)));
        stack.add(new Modifier(1, ModifierType.DODGE, new ModifierSource("haste", "Haste", SourceType.SPELL)));
        assertEquals(1, stack.getTotal());
    }

    @Test
    void testModifierDeserialization() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = "{\"value\":2,\"type\":\"ENHANCEMENT\",\"source\":{\"id\":\"belt-strength\",\"name\":\"Belt of Giant Strength\",\"type\":\"ITEM\"}}";
        Modifier modifier = mapper.readValue(json, Modifier.class);
        
        assertEquals(2, modifier.value());
        assertEquals(ModifierType.ENHANCEMENT, modifier.type());
        assertNotNull(modifier.source());
        assertEquals("belt-strength", modifier.source().id());
        assertEquals("Belt of Giant Strength", modifier.source().name());
        assertEquals(SourceType.ITEM, modifier.source().type());
    }
}
