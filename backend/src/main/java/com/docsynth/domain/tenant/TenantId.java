package com.docsynth.domain.tenant;

import java.util.UUID;

public record TenantId(UUID value) {
    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("TenantId value must not be null");
        }
    }
}
