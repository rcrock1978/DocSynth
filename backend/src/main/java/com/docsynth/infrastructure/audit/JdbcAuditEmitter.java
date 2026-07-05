package com.docsynth.infrastructure.audit;

import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.application.audit.AuditEventEnvelope;
import com.docsynth.domain.audit.AuditEvent;
import com.docsynth.infrastructure.persistence.AuditEntryRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Persists audit events to the append-only audit_entries table.
 *
 * Defensive sanitization: rejects keys that look like secrets
 * (Authorization, Cookie, password, token, secret, api_key).
 * Enforces append-only at the application layer; the database role
 * used at runtime grants INSERT and SELECT only.
 */
@Component
public class JdbcAuditEmitter implements AuditEmitter {

    private static final java.util.Set<String> FORBIDDEN_KEYS = java.util.Set.of(
        "authorization", "cookie", "password", "token", "secret", "api_key", "apikey"
    );

    private final AuditEntryRepository repository;

    public JdbcAuditEmitter(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void emit(AuditEventEnvelope envelope) {
        var sanitized = sanitize(envelope.detail());
        var correlationId = MDC.get("traceId");
        AuditEvent event = envelope.toEvent(correlationId);
        repository.append(event);
    }

    private java.util.Map<String, Object> sanitize(java.util.Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<String, Object> cleaned = new java.util.HashMap<>(detail.size());
        for (var entry : detail.entrySet()) {
            String key = entry.getKey();
            if (key != null && FORBIDDEN_KEYS.contains(key.toLowerCase())) {
                continue; // silently drop — never log the rejected key
            }
            cleaned.put(key, entry.getValue());
        }
        return cleaned;
    }
}
