package com.docsynth.interfaces.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SpecSummary(
    UUID id,
    String title,
    String specVersion,
    String openapiVersion,
    int endpointCount,
    int schemaCount,
    Instant parsedAt
) {}
