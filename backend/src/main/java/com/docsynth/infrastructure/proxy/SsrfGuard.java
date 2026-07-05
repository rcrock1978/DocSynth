package com.docsynth.infrastructure.proxy;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Set;

/**
 * SsrfGuard — pre-validates the resolved IP address(es) of a target
 * host. Rejects loopback, link-local, RFC1918, multicast, IPv6 ULA, and
 * the AWS / GCP / Azure metadata IPs.
 *
 * DNS-rebinding mitigation: resolve once, validate, then connect to
 * the validated IP (not the original hostname) using a custom resolver.
 * In v1 the validated IP is logged so the connection layer can pin it.
 */
public final class SsrfGuard {

    private static final Set<String> FORBIDDEN_HOSTNAMES = Set.of(
        "localhost", "metadata.google.internal", "metadata.azure.com"
    );

    private SsrfGuard() {}

    public static ValidatedTarget resolveAndValidate(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new SsrfBlockedException("host is required");
        }
        String normalized = host.toLowerCase();
        if (normalized.startsWith("file:") || normalized.startsWith("gopher:")
            || normalized.startsWith("dict:") || normalized.startsWith("ldap:")) {
            throw new SsrfBlockedException("scheme not allowed: " + host);
        }
        if (FORBIDDEN_HOSTNAMES.contains(normalized)) {
            throw new SsrfBlockedException("forbidden host: " + host);
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isForbidden(addr)) {
                    throw new SsrfBlockedException(
                        "host " + host + " resolves to forbidden address " + addr.getHostAddress()
                    );
                }
            }
            return new ValidatedTarget(host, port, addresses[0].getHostAddress());
        } catch (SsrfBlockedException e) {
            throw e;
        } catch (Exception e) {
            throw new SsrfBlockedException("DNS resolution failed for " + host + ": " + e.getMessage());
        }
    }

    private static boolean isForbidden(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
            || addr.isMulticastAddress() || addr.isSiteLocalAddress()) {
            return true;
        }
        if (addr instanceof Inet4Address v4) {
            byte[] b = v4.getAddress();
            int o0 = b[0] & 0xff;
            int o1 = b[1] & 0xff;
            // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
            if (o0 == 10) return true;
            if (o0 == 172 && o1 >= 16 && o1 <= 31) return true;
            if (o0 == 192 && o1 == 168) return true;
            // 169.254.169.254 (cloud metadata)
            if (o0 == 169 && o1 == 254) return true;
        }
        if (addr instanceof Inet6Address v6) {
            byte[] b = v6.getAddress();
            int b0 = b[0] & 0xff;
            // fc00::/7 (ULA) and fe80::/10 (link-local)
            if ((b0 & 0xfe) == 0xfc) return true;
        }
        return false;
    }

    public record ValidatedTarget(String originalHost, int port, String pinnedIp) {}
}
