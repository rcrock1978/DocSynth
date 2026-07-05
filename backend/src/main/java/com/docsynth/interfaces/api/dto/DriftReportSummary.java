package com.docsynth.interfaces.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DriftReportSummary(
    UUID id,
    String trigger,
    int added,
    int removed,
    int changed,
    int breaking,
    String notificationStatus,
    Instant generatedAt
) {}
