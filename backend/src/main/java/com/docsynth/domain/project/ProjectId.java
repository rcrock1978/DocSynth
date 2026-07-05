package com.docsynth.domain.project;

import java.util.UUID;

public record ProjectId(UUID value) {
    public ProjectId {
        if (value == null) {
            throw new IllegalArgumentException("ProjectId value must not be null");
        }
    }
}
