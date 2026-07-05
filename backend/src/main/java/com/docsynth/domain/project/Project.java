package com.docsynth.domain.project;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "default_drift_channel_ids", nullable = false)
    private UUID[] defaultDriftChannelIds = new UUID[0];

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Project() {}

    public Project(TenantId tenantId, String name, String slug) {
        this.tenantId = tenantId.value();
        this.name = name;
        this.slug = slug;
    }

    public UUID getId() { return id; }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getArchivedAt() { return archivedAt; }

    public ProjectId getProjectId() { return new ProjectId(id); }
}
