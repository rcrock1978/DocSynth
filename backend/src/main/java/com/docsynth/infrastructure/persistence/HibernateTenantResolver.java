package com.docsynth.infrastructure.persistence;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the current tenant identifier for Hibernate's tenant discriminator.
 *
 * Source of truth is the app.current_tenant PostgreSQL session variable, set
 * by TenantContextResolver on every request. The data layer is enforced by
 * RLS (V003__rls_policies.sql) which evaluates current_setting('app.current_tenant').
 */
@Component
public class HibernateTenantResolver implements CurrentTenantIdentifierResolver {

    public static final String TENANT_KEY = "app.current_tenant";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String value = org.hibernate.engine.spi.SharedSessionContractImplementor
            .extractTenantIdentifierFromContext();
        return value == null ? "default" : value;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
