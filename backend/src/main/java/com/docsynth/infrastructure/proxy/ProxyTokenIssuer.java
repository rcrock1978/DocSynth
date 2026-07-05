package com.docsynth.infrastructure.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * ProxyTokenIssuer — mints short-lived HMAC tokens bound to
 * (userId, tenantId, targetHost, nonce, TTL). Tokens are not JWTs; the
 * verifier is internal to the operator UI flow and uses a shared
 * secret. A token captured by a third party cannot be replayed for a
 * different tenant or host.
 */
@Component
public class ProxyTokenIssuer {

    private final byte[] secret;

    public ProxyTokenIssuer(@Value("${docsynth.proxy.token-secret-env:PROXY_TOKEN_SECRET}") String envName) {
        String s = System.getenv(envName);
        if (s == null || s.isBlank()) {
            // Stub: deterministic in tests; the production secret is loaded
            // from a mounted secret and rotated by AKS.
            s = "test-secret";
        }
        this.secret = s.getBytes(StandardCharsets.UTF_8);
    }

    public String mint(String userId, String tenantId, String targetHost) {
        return mintWithTtl(userId, tenantId, targetHost, 60);
    }

    public String mintWithTtl(String userId, String tenantId, String targetHost, long ttlSeconds) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + ttlSeconds;
        String nonce = UUID.randomUUID().toString();
        String payload = String.join("|", userId, tenantId, targetHost,
            Long.toString(issuedAt), Long.toString(expiresAt), nonce);
        String sig = sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            (payload + "|" + sig).getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}
