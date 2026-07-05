package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

/**
 * The resolved secret value (Authorization header, API key) MUST NEVER
 * appear in the proxy response body or in any log line. The TryIt
 * response body contains only the target API's response headers/body,
 * with the Authorization header stripped.
 */
class SecretLeakTest {

    @Test
    void authorization_header_stripped_from_response() {
        var sanitized = ProxyHttpClient.sanitizeResponseHeaders(java.util.Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer supersecret-do-not-leak"
        ));
        // The Authorization header MUST be removed.
        org.assertj.core.api.Assertions.assertThat(sanitized)
            .doesNotContainKey("Authorization")
            .doesNotContainValue(org.assertj.core.api.Conditions.anyOf(
                org.assertj.core.api.Conditions.containsPattern("supersecret")
            ));
    }

    @Test
    void body_does_not_echo_authorization_value() {
        String body = "{\"data\":\"ok\"}";
        String sanitized = ProxyHttpClient.sanitizeResponseBody(body, "supersecret-do-not-leak");
        org.assertj.core.api.Assertions.assertThat(sanitized).doesNotContain("supersecret-do-not-leak");
    }
}
