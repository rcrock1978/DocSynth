package com.docsynth.infrastructure.proxy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InMemoryRateLimiter — token-bucket per-user and per-tenant.
 * Production swap: Redis-backed token bucket for cross-pod accuracy.
 */
public class InMemoryRateLimiter {

    private final int perUserLimit;
    private final int perTenantLimit;
    private final ConcurrentHashMap<String, AtomicInteger> userBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> tenantBuckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int perUserLimit, int perTenantLimit) {
        this.perUserLimit = perUserLimit;
        this.perTenantLimit = perTenantLimit;
    }

    public boolean tryAcquire(String userId, String tenantId) {
        AtomicInteger ub = userBuckets.computeIfAbsent(userId, k -> new AtomicInteger(0));
        if (ub.incrementAndGet() > perUserLimit) return false;
        AtomicInteger tb = tenantBuckets.computeIfAbsent(tenantId, k -> new AtomicInteger(0));
        if (tb.incrementAndGet() > perTenantLimit) {
            ub.decrementAndGet();
            return false;
        }
        return true;
    }

    public int retryAfterSeconds(String userId, String tenantId) {
        // Stub: returns the configured limit. Real impl: returns time until
        // the bucket refills based on the configured refill rate.
        return Math.max(perUserLimit, perTenantLimit);
    }
}
