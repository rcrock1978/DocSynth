package com.docsynth.domain.documentation;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * DocSet — a generated documentation set derived from an ApiSpec at a
 * specific version. The unit of publishing. Per data-model.md §DocSet.
 *
 * Lifecycle states (FR-014): active, deprecated, archived. Transitions
 * are guarded by DocSetStateMachine (US5).
 */
@Entity
@Table(name = "doc_sets")
public class DocSet {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "api_spec_id", nullable = false)
    private UUID apiSpecId;

    @Column(name = "state", nullable = false)
    private String state = "active";

    @Column(name = "display_version", nullable = false)
    private String displayVersion;

    @Column(name = "storage_prefix", nullable = false)
    private String storagePrefix;

    @Column(name = "manifest_uri", nullable = false)
    private String manifestUri;

    @Column(name = "try_it_enabled", nullable = false)
    private boolean tryItEnabled = true;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(name = "generated_by_user_id")
    private UUID generatedByUserId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "sunset_at")
    private Instant sunsetAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "replacement_doc_set_id")
    private UUID replacementDocSetId;

    @Column(name = "gone_at")
    private Instant goneAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    protected DocSet() {}

    public DocSet(
        ProjectId projectId, TenantId tenantId, ApiSpecId apiSpecId,
        String displayVersion, String storagePrefix, String manifestUri,
        boolean tryItEnabled, UUID generatedByUserId
    ) {
        this.projectId = projectId.value();
        this.tenantId = tenantId.value();
        this.apiSpecId = apiSpecId.value();
        this.displayVersion = displayVersion;
        this.storagePrefix = storagePrefix;
        this.manifestUri = manifestUri;
        this.tryItEnabled = tryItEnabled;
        this.generatedByUserId = generatedByUserId;
    }

    public static DocSet reconstitute(
        DocSetId id, ProjectId projectId, TenantId tenantId, ApiSpecId apiSpecId,
        String displayVersion, String storagePrefix, String manifestUri,
        boolean tryItEnabled, Instant generatedAt, UUID generatedByUserId
    ) {
        DocSet d = new DocSet(projectId, tenantId, apiSpecId, displayVersion, storagePrefix, manifestUri, tryItEnabled, generatedByUserId);
        d.id = id.value();
        d.generatedAt = generatedAt;
        return d;
    }

    public DocSetId getId() { return new DocSetId(id); }
    public ProjectId getProjectId() { return new ProjectId(projectId); }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public ApiSpecId getApiSpecId() { return new ApiSpecId(apiSpecId); }
    public String getState() { return state; }
    public String getDisplayVersion() { return displayVersion; }
    public String getStoragePrefix() { return storagePrefix; }
    public String getManifestUri() { return manifestUri; }
    public boolean isTryItEnabled() { return tryItEnabled; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getDeprecatedAt() { return deprecatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public Instant getGoneAt() { return goneAt; }
    public UUID getReplacementDocSetId() { return replacementDocSetId; }

    public DocSet published() {
        this.publishedAt = Instant.now();
        return this;
    }
}
