package com.docsynth.infrastructure.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * ProxyTokenVerifier — verifies ProxyTokenIssuer tokens.
 * Rejects mismatched (user, tenant, target host) and expired tokens.
 */
@Component
public class ProxyTokenVerifier {

    private final byte[] secret;

    public ProxyTokenVerifier(@Value("${docsynth.proxy.token-secret-env:PROXY_TOKEN_SECRET}") String envName) {
        String s = System.getenv(envName);
        if (s == null || s.isBlank()) s = "test-secret";
        this.secret = s.getBytes(StandardCharsets.UTF_8);
    }

    public boolean verify(String token, String userId, String tenantId, String targetHost) {
        if (token == null) throw new TokenMismatchException("token is null");
        String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", -1);
        if (parts.length != 7) {
            throw new TokenMismatchException("malformed token");
        }
        String tUser = parts[0];
        String tTenant = parts[1];
        String tHost = parts[2];
        long issuedAt = Long.parseLong(parts[3]);
        long expiresAt = Long.parseLong(parts[4]);
        String nonce = parts[5];
        String sig = parts[6];

        if (!constantTimeEquals(tUser, userId)) {
            throw new TokenMismatchException("user mismatch");
        }
        if (!constantTimeEquals(tTenant, tenantId)) {
            throw new TokenMismatchException("tenant mismatch");
        }
        if (!constantTimeEquals(tHost, targetHost)) {
            throw new TokenMismatchException("target host mismatch");
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new TokenExpiredException("token expired at " + expiresAt);
        }
        String expected = sign(String.join("|", tUser, tTenant, tHost,
            Long.toString(issuedAt), Long.toString(expiresAt), nonce));
        if (!constantTimeEquals(expected, sig)) {
            throw new TokenMismatchException("signature mismatch");
        }
        return true;
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

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
