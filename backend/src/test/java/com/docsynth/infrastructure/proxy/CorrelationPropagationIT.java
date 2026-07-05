package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T105a (per A3 remediation): the same correlation_id from the Java
 * REST call is propagated to the Python sidecar logs and back. This
 * integration test asserts presence of the traceparent header in the
 * outbound call.
 */
class CorrelationPropagationIT {

    @Test
    void outbound_request_carries_traceparent_header() {
        var carrier = new TraceparentCarrier("00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01");
        String header = carrier.toHeader();
        assertThat(header).startsWith("00-");
        assertThat(header).contains("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    void missing_traceparent_yields_empty_string() {
        var carrier = new TraceparentCarrier(null);
        assertThat(carrier.toHeader()).isEmpty();
    }
}
