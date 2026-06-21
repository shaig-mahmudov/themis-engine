package com.themis.engine.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import com.themis.engine.domain.Condition;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Modifier;

/**
 * Request DTO for applying conditions to a character.
 */
public record ApplyConditionRequestDto(
    @NotBlank(message = "Condition ID cannot be blank")
    String id,

    @NotBlank(message = "Condition Name cannot be blank")
    String name,

    Map<StatType, List<Modifier>> modifiers
) {
    public Condition toDomain() {
        return new Condition(id, name, modifiers);
    }
}
