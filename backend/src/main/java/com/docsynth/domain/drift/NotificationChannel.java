package com.docsynth.domain.drift;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * NotificationChannel — configured target for drift alerts (FR-005).
 * Per data-model.md §NotificationChannel. Secrets are stored in Key Vault;
 * the `configRef` field references the secret path; the secret value
 * MUST NEVER be present in this row.
 */
@Entity
@Table(name = "notification_channels")
public class NotificationChannel {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "kind", nullable = false)
    private String kind; // slack|email|webhook|ci_check

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "config_ref", nullable = false)
    private String configRef;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected NotificationChannel() {}

    public NotificationChannel(ProjectId projectId, TenantId tenantId, String kind, String name, String configRef) {
        this.projectId = projectId == null ? null : projectId.value();
        this.tenantId = tenantId.value();
        this.kind = kind;
        this.name = name;
        this.configRef = configRef;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public String getKind() { return kind; }
    public String getName() { return name; }
    public String getConfigRef() { return configRef; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
