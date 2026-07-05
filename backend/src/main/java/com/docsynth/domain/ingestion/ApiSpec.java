package com.docsynth.domain.ingestion;

import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * ApiSpec — a parsed OpenAPI 3.x specification, immutable once ingested.
 * Per data-model.md §ApiSpec.
 */
@Entity
@Table(name = "api_specs")
public class ApiSpec {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_kind", nullable = false)
    private String sourceKind; // url | file_upload | github_repo

    @Column(name = "source_ref", nullable = false)
    private String sourceRef;

    @Column(name = "openapi_version", nullable = false)
    private String openapiVersion;

    @Column(name = "raw_spec_uri", nullable = false)
    private String rawSpecUri;

    @Column(name = "spec_sha256", nullable = false)
    private String specSha256;

    @Column(name = "title")
    private String title;

    @Column(name = "spec_version")
    private String specVersion;

    @Column(name = "endpoint_count", nullable = false)
    private int endpointCount;

    @Column(name = "schema_count", nullable = false)
    private int schemaCount;

    @Column(name = "parsed_at", nullable = false)
    private Instant parsedAt = Instant.now();

    @Column(name = "parsed_by_user_id")
    private UUID parsedByUserId;

    protected ApiSpec() {}

    public ApiSpec(
        ProjectId projectId,
        TenantId tenantId,
        SpecSource source,
        String openapiVersion,
        String rawSpecUri,
        String specSha256,
        String title,
        String specVersion,
        int endpointCount,
        int schemaCount,
        UUID parsedByUserId
    ) {
        this.projectId = projectId.value();
        this.tenantId = tenantId.value();
        this.sourceKind = source.kind();
        this.sourceRef = source.ref();
        this.openapiVersion = openapiVersion;
        this.rawSpecUri = rawSpecUri;
        this.specSha256 = specSha256;
        this.title = title;
        this.specVersion = specVersion;
        this.endpointCount = endpointCount;
        this.schemaCount = schemaCount;
        this.parsedByUserId = parsedByUserId;
    }

    /** Reconstitute a fully-formed ApiSpec from persistence (test/infra use). */
    public static ApiSpec reconstitute(
        ApiSpecId id, ProjectId projectId, TenantId tenantId, SpecSource source,
        String openapiVersion, String rawSpecUri, String specSha256,
        String title, String specVersion,
        int endpointCount, int schemaCount, Instant parsedAt, UUID parsedByUserId
    ) {
        ApiSpec s = new ApiSpec(projectId, tenantId, source, openapiVersion, rawSpecUri,
            specSha256, title, specVersion, endpointCount, schemaCount, parsedByUserId);
        s.id = id.value();
        s.parsedAt = parsedAt;
        return s;
    }

    public ApiSpecId getId() { return new ApiSpecId(id); }
    public ProjectId getProjectId() { return new ProjectId(projectId); }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public String sourceKind() { return sourceKind; }
    public String sourceRef() { return sourceRef; }
    public String openapiVersion() { return openapiVersion; }
    public String rawSpecUri() { return rawSpecUri; }
    public String specSha256() { return specSha256; }
    public String title() { return title; }
    public String specVersion() { return specVersion; }
    public int endpointCount() { return endpointCount; }
    public int schemaCount() { return schemaCount; }
    public Instant parsedAt() { return parsedAt; }
    public UUID parsedByUserId() { return parsedByUserId; }
}
