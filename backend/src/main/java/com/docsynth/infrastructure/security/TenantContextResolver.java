package com.docsynth.infrastructure.security;

import com.docsynth.domain.tenant.TenantId;
import com.docsynth.domain.user.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves tenant and user identity from the validated JWT.
 *
 * Fail-closed (FR-012): if the JWT is missing, the tenant claim is absent, or
 * the claim cannot be parsed as a UUID, the resolver throws. Callers MUST NOT
 * catch and default — the resulting request is rejected.
 */
@Component
public class TenantContextResolver {

    public TenantId currentTenantId() {
        Jwt jwt = currentJwt();
        Object claim = jwt.getClaim("tenant_id");
        if (claim == null) {
            throw new TenantResolutionException("JWT missing required 'tenant_id' claim");
        }
        try {
            return new TenantId(UUID.fromString(claim.toString()));
        } catch (IllegalArgumentException ex) {
            throw new TenantResolutionException("JWT 'tenant_id' is not a valid UUID");
        }
    }

    public UserId currentUserId() {
        Jwt jwt = currentJwt();
        Object claim = jwt.getClaim("user_id");
        if (claim == null) {
            throw new TenantResolutionException("JWT missing required 'user_id' claim");
        }
        return new UserId(UUID.fromString(claim.toString()));
    }

    private Jwt currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new TenantResolutionException("No authenticated JWT in security context");
        }
        return jwt;
    }
}
