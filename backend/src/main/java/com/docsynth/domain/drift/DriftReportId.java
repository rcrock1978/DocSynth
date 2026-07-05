package com.docsynth.domain.drift;

import java.util.UUID;

public record DriftReportId(UUID value) {
    public DriftReportId {
        if (value == null) throw new IllegalArgumentException("DriftReportId value must not be null");
    }
}
