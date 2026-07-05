package com.docsynth.domain.drift;

import java.util.UUID;

public record ApiSpecIdRef(UUID value) {
    public ApiSpecIdRef {
        if (value == null) throw new IllegalArgumentException("ApiSpecIdRef value must not be null");
    }
}
