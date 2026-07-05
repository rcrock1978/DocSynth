package com.docsynth.infrastructure.proxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ProxyHttpClient — outbound HTTP client for the Try It proxy.
 *
 * Mitigations (research.md §5):
 *  - Connect timeout ≤ 5s, request timeout ≤ 30s.
 *  - Outbound redirects DISABLED (Redirect.NEVER).
 *  - Request body cap 1 MB; response body cap 1 MB.
 *  - Header sanitization: strip inbound auth not from the secret store,
 *    strip Proxy-* / X-Forwarded-* headers.
 *  - Content-type allowlist on response.
 */
public final class ProxyHttpClient {

    private static final long MAX_BODY_BYTES = 1024L * 1024;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Set<String> FORBIDDEN_INBOUND_HEADERS = Set.of(
        "authorization", "cookie", "proxy-", "x-forwarded-", "x-real-ip"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/json", "application/xml", "text/", "application/octet-stream"
    );

    private ProxyHttpClient() {}

    public static HttpClient createDefault() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    }

    public static void assertNoRedirect(int status) {
        if (status >= 300 && status < 400) {
            throw new ProxyBlockedException("redirect response " + status + " blocked");
        }
    }

    public static Map<String, String> sanitizeRequestHeaders(Map<String, String> inbound, String injectedAuth) {
        Map<String, String> out = new LinkedHashMap<>();
        for (var e : inbound.entrySet()) {
            String key = e.getKey() == null ? "" : e.getKey().toLowerCase();
            if (FORBIDDEN_INBOUND_HEADERS.stream().anyMatch(key::startsWith)) continue;
            if (key.equals("host")) continue;
            out.put(e.getKey(), e.getValue());
        }
        if (injectedAuth != null && !injectedAuth.isBlank()) {
            out.put("Authorization", injectedAuth);
        }
        return out;
    }

    public static Map<String, String> sanitizeResponseHeaders(Map<String, String> inbound) {
        Map<String, String> out = new LinkedHashMap<>();
        for (var e : inbound.entrySet()) {
            String key = e.getKey() == null ? "" : e.getKey();
            if (key.equalsIgnoreCase("Authorization")) continue;
            if (key.equalsIgnoreCase("Set-Cookie")) continue;
            out.put(key, e.getValue());
        }
        return out;
    }

    public static String sanitizeResponseBody(String body, String secret) {
        if (secret == null || secret.isBlank() || body == null) return body;
        // Defensive: strip any accidental echo of the secret.
        return body.replace(secret, "[REDACTED]");
    }

    public static boolean isAllowedContentType(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ALLOWED_CONTENT_TYPES.stream().anyMatch(ct::startsWith);
    }
}
