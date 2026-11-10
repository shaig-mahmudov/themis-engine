package com.themis.engine.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themis.engine.domain.Encounter;
import com.themis.engine.domain.EncounterParticipant;
import com.themis.engine.domain.EncounterStatus;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.util.List;

/**
 * Deterministic Redis serializer for the {@link Encounter} domain aggregate.
 */
public class EncounterCacheSerializer implements RedisSerializer<Encounter> {

    private final ObjectMapper objectMapper;

    public EncounterCacheSerializer() {
        this(new ObjectMapper());
    }

    public EncounterCacheSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

    @Override
    public byte[] serialize(Encounter encounter) throws SerializationException {
        if (encounter == null) {
            return new byte[0];
        }
        try {
            EncounterCacheSnapshot snapshot = new EncounterCacheSnapshot(
                encounter.getId(),
                encounter.getVersion(),
                encounter.getName(),
                encounter.getStatus(),
                encounter.getCurrentRound(),
                encounter.getActiveParticipantIndex(),
                encounter.getParticipants()
            );
            return objectMapper.writeValueAsBytes(snapshot);
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize Encounter for cache: " + encounter.getId(), e);
        }
    }

    @Override
    public Encounter deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            EncounterCacheSnapshot snapshot = objectMapper.readValue(bytes, EncounterCacheSnapshot.class);
            Encounter encounter = new Encounter(
                snapshot.id(),
                snapshot.name(),
                snapshot.status(),
                snapshot.currentRound(),
                snapshot.activeParticipantIndex(),
                snapshot.participants() != null ? snapshot.participants() : List.of()
            );
            encounter.restoreVersion(snapshot.version());
            return encounter;
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize Encounter from cache", e);
        }
    }

    record EncounterCacheSnapshot(
        String id,
        Long version,
        String name,
        EncounterStatus status,
        int currentRound,
        int activeParticipantIndex,
        List<EncounterParticipant> participants
    ) {}
}
