package com.docsynth.domain.drift;

public record DriftItemRecord(
    String targetKind,
    String targetPath,
    String changeKind,
    String compatibility,
    String message
) {}
