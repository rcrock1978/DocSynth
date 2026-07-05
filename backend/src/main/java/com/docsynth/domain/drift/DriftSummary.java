package com.docsynth.domain.drift;

public record DriftSummary(int added, int removed, int changed, int breaking) {
    public static DriftSummary zero() {
        return new DriftSummary(0, 0, 0, 0);
    }
}
