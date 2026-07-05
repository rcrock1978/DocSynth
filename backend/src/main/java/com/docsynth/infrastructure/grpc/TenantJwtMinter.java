package com.docsynth.infrastructure.grpc;

import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.infrastructure.security.TenantResolutionException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

/**
 * Mints short-lived RS256 JWTs for outbound gRPC tenant assertion.
 * Public key is published as a JWKS endpoint consumed by the Python sidecar.
 */
@Component
public class TenantJwtMinter {

    private final RSAPrivateKey privateKey;
    private final String issuer;
    private final String audience;

    public TenantJwtMinter(@Value("${docsynth.jwt.private-key-path}") String keyPath,
                           @Value("${docsynth.jwt.issuer}") String issuer,
                           @Value("${docsynth.jwt.audience}") String audience) {
        this.privateKey = readPrivateKey(keyPath);
        this.issuer = issuer;
        this.audience = audience;
    }

    public String mint() {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("docsynth-backend")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .claim("tenant_id", currentTenantClaim())
                .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to mint tenant JWT", e);
        }
    }

    private String currentTenantClaim() {
        // Resolved at mint-time; not stored statically.
        return new TenantContextResolverAdapter().currentTenantIdString();
    }

    private static RSAPrivateKey readPrivateKey(String path) {
        // Production: load PKCS#8 PEM from mounted secret. Skipped in this scaffold.
        throw new UnsupportedOperationException("Wire key loader in deployment; see secrets config");
    }

    /**
     * Adapter to call TenantContextResolver without circular Spring injection.
     */
    private static class TenantContextResolverAdapter {
        String currentTenantIdString() {
            return new TenantContextResolver().currentTenantId().value().toString();
        }
    }
}
