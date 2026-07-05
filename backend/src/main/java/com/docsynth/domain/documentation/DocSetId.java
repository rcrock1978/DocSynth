package com.docsynth.domain.documentation;

import java.util.UUID;

public record DocSetId(UUID value) {
    public DocSetId {
        if (value == null) throw new IllegalArgumentException("DocSetId value must not be null");
    }
}
