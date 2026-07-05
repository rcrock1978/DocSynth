package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redirects MUST be disabled on the outbound proxy client (research.md §5
 * mitigation 4). Otherwise a malicious 3xx Location to a private IP
 * would bypass the allowlist.
 */
class RedirectDisabledTest {

    @Test
    void http_client_does_not_follow_redirects() {
        var client = ProxyHttpClient.createDefault();
        // Java HttpClient.Redirect.NEVER is the default for createDefault;
        // this test asserts the configuration is honoured.
        assertThat(client.followRedirects()).isEqualTo(java.net.http.HttpClient.Redirect.NEVER);
    }

    @Test
    void redirect_response_is_rejected() {
        var client = ProxyHttpClient.createDefault();
        // Direct invocation: the client surfaces 3xx as a normal response
        // (since redirects are disabled). The CALLER (TryItProxyUseCase)
        // MUST reject any 3xx to prevent redirect-based bypass.
        assertThatThrownBy(() -> ProxyHttpClient.assertNoRedirect(302))
            .isInstanceOf(ProxyBlockedException.class);
        assertThatThrownBy(() -> ProxyHttpClient.assertNoRedirect(301))
            .isInstanceOf(ProxyBlockedException.class);
    }
}
