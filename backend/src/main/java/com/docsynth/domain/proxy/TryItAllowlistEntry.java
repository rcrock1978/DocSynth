package com.docsynth.domain.proxy;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * TryItAllowlistEntry — a host pattern the tenant authorizes for the
 * Try It proxy. Per data-model.md §TryItAllowlistEntry.
 */
@Entity
@Table(name = "try_it_allowlist")
public class TryItAllowlistEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "host_pattern", nullable = false)
    private String hostPattern;

    @Column(name = "source", nullable = false)
    private String source; // from_servers | operator_added

    @Column(name = "added_by_user_id")
    private UUID addedByUserId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected TryItAllowlistEntry() {}

    public TryItAllowlistEntry(
        ProjectId projectId, TenantId tenantId, String hostPattern,
        String source, UUID addedByUserId
    ) {
        this.projectId = projectId.value();
        this.tenantId = tenantId.value();
        this.hostPattern = hostPattern;
        this.source = source;
        this.addedByUserId = addedByUserId;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public String getHostPattern() { return hostPattern; }
    public String getSource() { return source; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isActive() { return revokedAt == null; }
    public void revoke() { this.revokedAt = Instant.now(); }
}
