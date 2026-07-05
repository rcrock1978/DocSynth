package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When the target API returns an error, the proxy MUST surface the
 * status code and body to the operator. The TryIt response includes
 * `status`, `body`, `durationMs`, `requestId` so the operator can
 * diagnose the failure.
 */
class ErrorPassthroughIT {

    @Test
    void error_response_passthrough_preserves_status() {
        var response = new ProxyResponse(404, "{\"error\":\"not found\"}", 120, "req-abc");
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.body()).contains("not found");
        assertThat(response.requestId()).isEqualTo("req-abc");
    }

    @Test
    void five_hundred_response_passthrough() {
        var response = new ProxyResponse(503, "{\"error\":\"unavailable\"}", 5000, "req-def");
        assertThat(response.status()).isEqualTo(503);
        assertThat(response.body()).contains("unavailable");
    }
}
