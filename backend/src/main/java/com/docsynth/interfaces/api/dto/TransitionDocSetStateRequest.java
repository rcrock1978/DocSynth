package com.docsynth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record TransitionDocSetStateRequest(
    @NotBlank @Pattern(regexp = "active|deprecated|archived") String action,
    Instant sunsetAt
) {}
