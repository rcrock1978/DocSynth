package com.docsynth.domain.audit;

import com.docsynth.domain.tenant.TenantId;
import com.docsynth.domain.user.UserId;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical audit event (FR-009).
 *
 * Required fields. Detail map MUST NOT contain Authorization headers, Cookie
 * values, request bodies, or notification secrets — the AuditEmitter rejects
 * such keys defensively.
 */
public record AuditEvent(
    TenantId tenantId,
    UserId actorUserId,         // nullable for system actions
    String action,
    String resourceKind,
    UUID resourceId,            // nullable
    UUID projectId,             // nullable
    String correlationId,       // from MDC / OTel context
    Outcome outcome,
    Instant occurredAt,
    Map<String, Object> detail
) {
    public enum Outcome { SUCCESS, FAILURE, DENIED }

    public AuditEvent {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("outcome is required");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
