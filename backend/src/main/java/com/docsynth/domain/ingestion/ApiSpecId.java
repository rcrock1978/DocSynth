package com.docsynth.domain.ingestion;

import java.util.UUID;

public record ApiSpecId(UUID value) {
    public ApiSpecId {
        if (value == null) throw new IllegalArgumentException("ApiSpecId value must not be null");
    }
}
