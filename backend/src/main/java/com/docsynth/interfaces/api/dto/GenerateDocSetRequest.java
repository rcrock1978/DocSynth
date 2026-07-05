package com.docsynth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GenerateDocSetRequest(
    @NotNull UUID specId,
    @NotBlank String displayVersion,
    List<String> targetLanguages
) {}
