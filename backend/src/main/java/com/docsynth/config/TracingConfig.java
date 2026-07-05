package com.docsynth.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer + OpenTelemetry wiring (FR-013).
 *
 * - Distributed traces with correlation IDs propagated via W3C tracecontext
 *   across all service boundaries (Java backend, Python sidecar via gRPC).
 * - RED/USE metrics exposed via /actuator/prometheus.
 * - Structured JSON logs include traceId and spanId in every line.
 */
@Configuration
public class TracingConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
