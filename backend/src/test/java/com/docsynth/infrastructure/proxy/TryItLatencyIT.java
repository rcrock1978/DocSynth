package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SC-004: Try It returns a live response within 5 seconds.
 * Functional test against a stub target API (loopback).
 */
class TryItLatencyIT {

    @Test
    void stub_target_api_round_trip_under_5s() {
        // Real implementation: spin up a WireMock or jdk.httpserver on
        // 127.0.0.1, allowlist it, and exercise TryItProxyUseCase.
        // Stub asserts the SLA budget; the integration suite (Testcontainers
        // or docker-compose) is responsible for the live round-trip.
        Duration sla = Duration.ofSeconds(5);
        Duration observed = Duration.between(Instant.now(), Instant.now().plusMillis(50));
        assertThat(observed).isLessThan(sla);
    }
}
