package com.docsynth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IngestSpecRequest(
    @NotBlank
    @Pattern(regexp = "url|file_upload|github_repo", message = "sourceKind must be url|file_upload|github_repo")
    String sourceKind,

    @NotBlank
    String sourceRef,

    String accessTokenRef
) {}
