package com.themis.engine.api;

import java.util.Map;

public record StartEncounterRequest(
    Map<String, Integer> manualRolls
) {}
