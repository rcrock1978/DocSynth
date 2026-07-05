package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cross-tenant replay rejection (research.md §5 mitigation 11).
 * The request token is bound to (user, tenant, target host, nonce, TTL).
 * A token minted for tenant A MUST NOT be honored for tenant B.
 */
class TokenReplayTest {

    @Test
    void token_bound_to_tenant_is_rejected_for_other_tenant() {
        var issuer = new ProxyTokenIssuer("test-secret");
        String token = issuer.mint("u1", "tenantA", "api.example.com");
        var verifier = new ProxyTokenVerifier("test-secret");
        assertThat(verifier.verify(token, "u1", "tenantA", "api.example.com")).isTrue();
        assertThatThrownBy(() -> verifier.verify(token, "u1", "tenantB", "api.example.com"))
            .isInstanceOf(TokenMismatchException.class);
    }

    @Test
    void token_bound_to_target_host_rejected_for_other_host() {
        var issuer = new ProxyTokenIssuer("test-secret");
        String token = issuer.mint("u1", "t1", "api.example.com");
        var verifier = new ProxyTokenVerifier("test-secret");
        assertThatThrownBy(() -> verifier.verify(token, "u1", "t1", "evil.example.com"))
            .isInstanceOf(TokenMismatchException.class);
    }

    @Test
    void expired_token_is_rejected() {
        var issuer = new ProxyTokenIssuer("test-secret");
        // Mint with TTL=0 to force expiration.
        String token = issuer.mintWithTtl("u1", "t1", "api.example.com", 0);
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        var verifier = new ProxyTokenVerifier("test-secret");
        assertThatThrownBy(() -> verifier.verify(token, "u1", "t1", "api.example.com"))
            .isInstanceOf(TokenExpiredException.class);
    }
}
