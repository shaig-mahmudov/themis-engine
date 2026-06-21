package com.themis.engine.domain;

import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Aggregate root representing an active or pending combat encounter.
 */
public class Encounter implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private EncounterStatus status;
    private int currentRound;
    private int activeParticipantIndex;
    private List<EncounterParticipant> participants = new ArrayList<>();

    public Encounter(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Encounter Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.status = EncounterStatus.PENDING;
        this.currentRound = 0;
        this.activeParticipantIndex = 0;
    }

    // Factory method/constructor to restore state from repository
    public Encounter(
        String id,
        String name,
        EncounterStatus status,
        int currentRound,
        int activeParticipantIndex,
        List<EncounterParticipant> participants
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.currentRound = currentRound;
        this.activeParticipantIndex = activeParticipantIndex;
        this.participants = new ArrayList<>(participants);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EncounterStatus getStatus() {
        return status;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getActiveParticipantIndex() {
        return activeParticipantIndex;
    }

    public List<EncounterParticipant> getParticipants() {
        return List.copyOf(participants);
    }

    public void addParticipant(EncounterParticipant participant) {
        if (status != EncounterStatus.PENDING) {
            throw new IllegalStateException("Cannot add participants to an encounter that is " + status);
        }
        if (participants.stream().anyMatch(p -> p.combatantId().equals(participant.combatantId()))) {
            throw new IllegalArgumentException("Participant with ID " + participant.combatantId() + " is already in this encounter");
        }
        participants.add(participant);
    }

    /**
     * Rolls initiative for all participants and transitions status to ACTIVE.
     * Uses optional manual die rolls if supplied, otherwise rolls 1d20.
     */
    public void start(
        Map<String, Integer> manualRolls,
        RandomGenerator random,
        Function<String, Integer> dexModLookup
    ) {
        if (status != EncounterStatus.PENDING) {
            throw new IllegalStateException("Encounter is already " + status);
        }
        if (participants.isEmpty()) {
            throw new IllegalStateException("Cannot start an encounter with no participants");
        }

        List<EncounterParticipant> rolledParticipants = new ArrayList<>();
        for (EncounterParticipant p : participants) {
            int roll;
            if (manualRolls != null && manualRolls.containsKey(p.combatantId())) {
                roll = manualRolls.get(p.combatantId());
                if (roll < 1 || roll > 20) {
                    throw new IllegalArgumentException("Manual initiative roll must be between 1 and 20");
                }
            } else {
                roll = random.nextInt(1, 21);
            }

            int modifier = 0;
            if (p.combatantType() == CombatantType.CHARACTER) {
                modifier = dexModLookup.apply(p.combatantId());
            }

            int total = roll + modifier;
            rolledParticipants.add(p.withInitiative(roll, total));
        }

        // Sort by initiativeTotal desc, then dexterityModifier desc, then combatantId asc (stable tie-breaker)
        rolledParticipants.sort((p1, p2) -> {
            int comp = Integer.compare(p2.initiativeTotal(), p1.initiativeTotal());
            if (comp != 0) return comp;
            
            comp = Integer.compare(p2.dexterityModifier(), p1.dexterityModifier());
            if (comp != 0) return comp;

            return p1.combatantId().compareTo(p2.combatantId());
        });

        this.participants = rolledParticipants;
        this.status = EncounterStatus.ACTIVE;
        this.currentRound = 1;
        this.activeParticipantIndex = 0;
    }

    /**
     * Advances turn to the next participant. If all participants have acted,
     * wraps back to the first participant and increments the current round.
     */
    public void nextTurn() {
        if (status != EncounterStatus.ACTIVE) {
            throw new IllegalStateException("Cannot advance turn: encounter is " + status);
        }
        if (participants.isEmpty()) {
            throw new IllegalStateException("Cannot advance turn: no participants in encounter");
        }

        activeParticipantIndex++;
        if (activeParticipantIndex >= participants.size()) {
            activeParticipantIndex = 0;
            currentRound++;
        }
    }

    public Optional<EncounterParticipant> getActiveParticipant() {
        if (status != EncounterStatus.ACTIVE || participants.isEmpty() || activeParticipantIndex < 0 || activeParticipantIndex >= participants.size()) {
            return Optional.empty();
        }
        return Optional.of(participants.get(activeParticipantIndex));
    }

    public void end() {
        if (status == EncounterStatus.COMPLETED) {
            throw new IllegalStateException("Encounter is already COMPLETED");
        }
        this.status = EncounterStatus.COMPLETED;
    }
}
