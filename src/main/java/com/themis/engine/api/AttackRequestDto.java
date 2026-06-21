package com.themis.engine.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object representing the request body to execute an attack.
 */
public record AttackRequestDto(
    @NotBlank(message = "Attacker ID cannot be blank")
    String attackerId,

    @NotBlank(message = "Target ID cannot be blank")
    String targetId,

    @NotBlank(message = "Weapon ID cannot be blank")
    String weaponId,

    @Min(value = 1, message = "d20 roll must be at least 1")
    @Max(value = 20, message = "d20 roll cannot be greater than 20")
    Integer d20Roll
) {}
