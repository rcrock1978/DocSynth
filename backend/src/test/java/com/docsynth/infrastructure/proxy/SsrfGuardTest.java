package com.docsynth.infrastructure.proxy;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SSRF guard tests (research.md §4-5 mitigation 1).
 * Targets in private/loopback/link-local/multicast/IPv6 ULA ranges
 * MUST be rejected. Cloud metadata IPs MUST be rejected.
 */
class SsrfGuardTest {

    @Test
    void rejects_localhost_ipv4() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("localhost", 443))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_127_0_0_1() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("127.0.0.1", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_rfc1918_10_dot() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("10.0.0.1", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_rfc1918_192_168() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("192.168.1.1", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_rfc1918_172_16() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("172.16.0.1", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_aws_metadata_169_254_169_254() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("169.254.169.254", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_file_scheme_in_host() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("file://etc/passwd", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }

    @Test
    void rejects_ipv6_loopback() {
        assertThatThrownBy(() -> SsrfGuard.resolveAndValidate("[::1]", 80))
            .isInstanceOf(SsrfBlockedException.class);
    }
}
