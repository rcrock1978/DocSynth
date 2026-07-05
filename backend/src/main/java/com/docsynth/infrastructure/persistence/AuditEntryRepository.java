package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * AuditEntryRepository. Append-only; no update or delete methods are exposed.
 * The application role in production grants INSERT, SELECT only.
 */
@Repository
public class AuditEntryRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditEntryRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void append(AuditEvent event) {
        jdbc.update(
            "INSERT INTO audit_entries "
            + "(id, tenant_id, actor_user_id, action, resource_kind, resource_id, project_id, "
            +  "correlation_id, outcome, detail, created_at) "
            + "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?::text, ?::jsonb, ?)",
            ps -> {
                ps.setObject(1, event.tenantId().value());
                ps.setObject(2, event.actorUserId() == null ? null : event.actorUserId().value());
                ps.setString(3, event.action());
                ps.setString(4, event.resourceKind());
                ps.setObject(5, event.resourceId());
                ps.setObject(6, event.projectId());
                ps.setString(7, event.correlationId());
                ps.setString(8, event.outcome().name().toLowerCase());
                ps.setString(9, serialize(event));
                ps.setTimestamp(10, Timestamp.from(event.occurredAt() == null ? Instant.now() : event.occurredAt()));
            }
        );
    }

    private String serialize(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event.detail());
        } catch (Exception e) {
            return "{}";
        }
    }
}
