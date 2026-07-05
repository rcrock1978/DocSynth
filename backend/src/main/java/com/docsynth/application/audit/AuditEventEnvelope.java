package com.docsynth.application.audit;

import com.docsynth.domain.audit.AuditEvent;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.domain.user.UserId;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Application-level envelope for AuditEmitter.emit(...).
 *
 * The emitter is responsible for translating the envelope into an
 * AuditEvent with correlation_id, sanitization of detail keys, and
 * persistence. The use case never sees the storage layer.
 */
public record AuditEventEnvelope(
    TenantId tenantId,
    UserId actorUserId,
    String action,
    String resourceKind,
    UUID resourceId,
    UUID projectId,
    AuditEvent.Outcome outcome,
    Map<String, Object> detail
) {
    public AuditEvent toEvent(String correlationId) {
        return new AuditEvent(
            tenantId,
            actorUserId,
            action,
            resourceKind,
            resourceId,
            projectId,
            correlationId,
            outcome,
            Instant.now(),
            detail == null ? Map.of() : detail
        );
    }
}
