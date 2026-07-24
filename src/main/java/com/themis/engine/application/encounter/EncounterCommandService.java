package com.themis.engine.application.encounter;

import com.themis.engine.domain.Character;
import com.themis.engine.domain.CharacterStore;
import com.themis.engine.domain.CombatantType;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import com.themis.engine.domain.EncounterStore;
import com.themis.engine.domain.StatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Service that orchestrates state-changing encounter use cases.
 */
@Service
@Transactional
public class EncounterCommandService {

    private final EncounterStore encounterStore;
    private final CharacterStore characterStore;
    private final RandomGenerator random;

    public EncounterCommandService(
        EncounterStore encounterStore,
        CharacterStore characterStore,
        RandomGenerator random
    ) {
        this.encounterStore = encounterStore;
        this.characterStore = characterStore;
        this.random = random;
    }

    public Encounter createEncounter(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Encounter name cannot be null or blank");
        }
        Encounter encounter = new Encounter(UUID.randomUUID().toString(), name);
        return encounterStore.save(encounter);
    }

    public Encounter addParticipant(
        String encounterId,
        String combatantId,
        CombatantType combatantType,
        String manualName,
        Integer manualDexterityModifier
    ) {
        Encounter encounter = encounterStore.findById(encounterId)
            .orElseThrow(() -> new IllegalArgumentException("Encounter not found: " + encounterId));

        int dexterityModifier = 0;
        String name = manualName;

        if (combatantType == CombatantType.CHARACTER) {
            Character character = characterStore.findById(combatantId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + combatantId));
            dexterityModifier = character.getAttributeModifier(StatType.DEXTERITY);
            name = character.getName();
        } else {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name must be provided for non-character combatants");
            }
            if (manualDexterityModifier != null) {
                dexterityModifier = manualDexterityModifier;
            }
        }

        EncounterParticipant participant = new EncounterParticipant(
            combatantId,
            combatantType,
            name,
            null,
            null,
            dexterityModifier
        );

        encounter.addParticipant(participant);
        return encounterStore.save(encounter);
    }

    public Encounter startEncounter(String encounterId, Map<String, Integer> manualRolls) {
        Encounter encounter = encounterStore.findById(encounterId)
            .orElseThrow(() -> new IllegalArgumentException("Encounter not found: " + encounterId));

        Function<String, Integer> dexModLookup = combatantId -> {
            Character character = characterStore.findById(combatantId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + combatantId));
            return character.getAttributeModifier(StatType.DEXTERITY);
        };

        encounter.start(manualRolls, random, dexModLookup);
        triggerStartTurnIfCharacter(encounter);
        return encounterStore.save(encounter);
    }

    public Encounter nextTurn(String encounterId) {
        Encounter encounter = encounterStore.findById(encounterId)
            .orElseThrow(() -> new IllegalArgumentException("Encounter not found: " + encounterId));

        encounter.nextTurn();
        triggerStartTurnIfCharacter(encounter);
        return encounterStore.save(encounter);
    }

    public Encounter endEncounter(String encounterId) {
        Encounter encounter = encounterStore.findById(encounterId)
            .orElseThrow(() -> new IllegalArgumentException("Encounter not found: " + encounterId));

        encounter.end();
        return encounterStore.save(encounter);
    }

    private void triggerStartTurnIfCharacter(Encounter encounter) {
        Optional<EncounterParticipant> activeParticipant = encounter.getActiveParticipant();
        if (activeParticipant.isPresent() && activeParticipant.get().combatantType() == CombatantType.CHARACTER) {
            String characterId = activeParticipant.get().combatantId();
            Character character = characterStore.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Active character participant not found in CharacterStore: " + characterId));
            character.startTurn();
            characterStore.save(character);
        }
    }
}
