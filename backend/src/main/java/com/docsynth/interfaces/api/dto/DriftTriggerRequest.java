package com.docsynth.interfaces.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DriftTriggerRequest(
    @NotNull UUID specId,
    String trigger
) {}
