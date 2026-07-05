package com.docsynth.infrastructure.security;

import com.azure.security.keyvault.secrets.SecretClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Thin wrapper over Azure Key Vault SecretClient.
 *
 * FR-011: secrets are read on demand, never logged, never returned to the
 * browser. Callers that need to attach a secret to an outbound call resolve
 * it at request time and pass the in-memory value directly to the consumer
 * (e.g., the Try It proxy injecting an Authorization header).
 */
@Component
public class KeyVaultSecretStore {

    private final SecretClient client;

    public KeyVaultSecretStore(SecretClient client) {
        this.client = client;
    }

    public Optional<String> getSecret(String keyVaultSecretRef) {
        if (keyVaultSecretRef == null || keyVaultSecretRef.isBlank()) {
            return Optional.empty();
        }
        // The SecretClient is configured with managed identity; no secret values
        // are logged at any layer. Callers must not include the value in
        // response bodies or audit entries.
        return Optional.ofNullable(client.getSecret(keyVaultSecretRef)).map(s -> s.getValue());
    }
}
