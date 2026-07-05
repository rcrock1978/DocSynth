package com.docsynth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateChannelRequest(
    @NotBlank @Pattern(regexp = "slack|email|webhook|ci_check") String kind,
    @NotBlank String name,
    @NotBlank String configRef
) {}
