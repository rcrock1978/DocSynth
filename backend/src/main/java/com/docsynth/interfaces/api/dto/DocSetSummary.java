package com.docsynth.interfaces.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocSetSummary(
    UUID id,
    String displayVersion,
    String state,
    boolean tryItEnabled,
    Instant generatedAt,
    Instant publishedAt,
    Instant deprecatedAt,
    Instant archivedAt,
    Instant goneAt
) {}
