package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-tenant and per-user rate limits (research.md §5 mitigation 9).
 * Returns 429 with Retry-After.
 */
class RateLimitTest {

    @Test
    void per_user_limit_returns_429_with_retry_after() {
        var limiter = new InMemoryRateLimiter(60, 60); // 60 req/min/user, 60 req/min/tenant
        // First 60 requests pass.
        for (int i = 0; i < 60; i++) {
            assertThat(limiter.tryAcquire("u1", "t1")).isTrue();
        }
        // 61st is denied.
        assertThat(limiter.tryAcquire("u1", "t1")).isFalse();
        assertThat(limiter.retryAfterSeconds("u1", "t1")).isGreaterThan(0).isLessThanOrEqualTo(60);
    }

    @Test
    void per_tenant_limit_independent_of_user() {
        var limiter = new InMemoryRateLimiter(100, 5);
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("u1", "t1")).isTrue();
        }
        // 6th user in same tenant also denied (tenant bucket exhausted).
        assertThat(limiter.tryAcquire("u2", "t1")).isFalse();
    }
}
