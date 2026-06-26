package com.themis.engine.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Data Transfer Object representing the request body to configure character spellcasting.
 */
public record ConfigureSpellcastingRequestDto(
    @Min(value = 1, message = "Caster level must be at least 1")
    int casterLevel,

    @NotBlank(message = "Casting attribute cannot be blank")
    String castingAttribute,

    @NotNull(message = "Max slots cannot be null")
    @Size(min = 10, max = 10, message = "Max slots must have exactly 10 elements (levels 0-9)")
    List<Integer> maxSlots
) {}
