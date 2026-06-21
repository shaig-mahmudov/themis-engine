package com.themis.engine.api;

import jakarta.validation.constraints.NotBlank;

public record CreateEncounterRequest(
    @NotBlank(message = "Encounter name cannot be blank")
    String name
) {}
