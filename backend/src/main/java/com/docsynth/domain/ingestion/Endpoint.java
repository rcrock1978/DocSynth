package com.docsynth.domain.ingestion;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Endpoint — denormalized from ApiSpec for fast query (data-model.md §Endpoint).
 */
@Entity
@Table(name = "endpoints")
public class Endpoint {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "api_spec_id", nullable = false)
    private UUID apiSpecId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "operation_id")
    private String operationId;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "summary")
    private String summary;

    @Column(name = "description")
    private String description;

    @Column(name = "tags", nullable = false)
    private String[] tags = new String[0];

    @Column(name = "parameters", columnDefinition = "jsonb")
    private String parameters;

    @Column(name = "response_schemas", columnDefinition = "jsonb")
    private String responseSchemas;

    @Column(name = "security_requirements", columnDefinition = "jsonb")
    private String securityRequirements;

    @Column(name = "deprecated", nullable = false)
    private boolean deprecated;

    protected Endpoint() {}

    public Endpoint(
        UUID apiSpecId, TenantId tenantId, String method, String path,
        String operationId, String summary, String description, String[] tags,
        String parametersJson, String responseSchemasJson, String securityRequirementsJson,
        boolean deprecated
    ) {
        this.apiSpecId = apiSpecId;
        this.tenantId = tenantId.value();
        this.method = method;
        this.path = path;
        this.operationId = operationId;
        this.summary = summary;
        this.description = description;
        if (tags != null) this.tags = tags;
        this.parameters = parametersJson;
        this.responseSchemas = responseSchemasJson;
        this.securityRequirements = securityRequirementsJson;
        this.deprecated = deprecated;
    }

    public UUID getId() { return id; }
    public UUID getApiSpecId() { return apiSpecId; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getOperationId() { return operationId; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String[] getTags() { return tags; }
    public boolean isDeprecated() { return deprecated; }
}
