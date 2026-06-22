package com.themis.engine.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a numerical bonus or penalty of a specific type from a specific source.
 */
public record Modifier(int value, ModifierType type, ModifierSource source) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Modifier.class);

    public Modifier {
        if (type == null) {
            throw new IllegalArgumentException("Modifier type cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Modifier source cannot be null");
        }
    }

    @JsonCreator
    public static Modifier fromJson(
            @JsonProperty("value") int value,
            @JsonProperty("type") ModifierType type,
            @JsonProperty("source") JsonNode sourceNode) {
        
        if (sourceNode == null) {
            throw new IllegalArgumentException("Modifier source cannot be null");
        }

        ModifierSource parsedSource;
        if (sourceNode.isTextual()) {
            String legacySourceStr = sourceNode.asText();
            log.warn("Legacy string source encountered: '{}'. Converting to ModifierSource.", legacySourceStr);
            parsedSource = new ModifierSource(legacySourceStr, legacySourceStr, SourceType.GENERIC);
        } else {
            String id = sourceNode.get("id").asText();
            String name = sourceNode.get("name").asText();
            SourceType sourceType = SourceType.valueOf(sourceNode.get("type").asText());
            parsedSource = new ModifierSource(id, name, sourceType);
        }

        return new Modifier(value, type, parsedSource);
    }
}
