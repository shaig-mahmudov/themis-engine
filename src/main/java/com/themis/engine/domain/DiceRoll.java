package com.themis.engine.domain;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Value Object holding the parsed structure of a dice roll (e.g. "1d8+4").
 */
public record DiceRoll(int numberOfDice, int sides, int modifier) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final Pattern PATTERN = Pattern.compile("^(\\d*)d(\\d+)(?:([+-]\\d+))?$");

    public DiceRoll {
        if (numberOfDice < 1) {
            throw new IllegalArgumentException("Number of dice must be at least 1");
        }
        if (sides < 1) {
            throw new IllegalArgumentException("Sides of the dice must be at least 1");
        }
    }

    /**
     * Parses standard dice notation (e.g., "1d8+4", "2d6", "d20-1").
     */
    public static DiceRoll parse(String input) {
        Objects.requireNonNull(input, "Input cannot be null");
        Matcher matcher = PATTERN.matcher(input.trim().replaceAll("\\s+", ""));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid dice roll format: " + input);
        }
        int numDice = matcher.group(1).isEmpty() ? 1 : Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int modifier = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new DiceRoll(numDice, sides, modifier);
    }

    /**
     * Executes the dice roll using a custom supplier of random numbers.
     * Useful for testing with deterministic values.
     */
    public int roll(IntSupplier dieSource) {
        int sum = 0;
        for (int i = 0; i < numberOfDice; i++) {
            sum += dieSource.getAsInt();
        }
        return sum + modifier;
    }

    /**
     * Executes the dice roll using Java's RandomGenerator interface.
     */
    public int roll(RandomGenerator random) {
        return roll(() -> random.nextInt(1, sides + 1));
    }

    @Override
    public String toString() {
        if (modifier == 0) {
            return numberOfDice + "d" + sides;
        }
        return numberOfDice + "d" + sides + (modifier > 0 ? "+" + modifier : modifier);
    }
}
