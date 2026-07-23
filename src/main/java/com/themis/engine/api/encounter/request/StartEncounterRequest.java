package com.themis.engine.api.encounter.request;

import java.util.Map;

public record StartEncounterRequest(
    Map<String, Integer> manualRolls
) {}
