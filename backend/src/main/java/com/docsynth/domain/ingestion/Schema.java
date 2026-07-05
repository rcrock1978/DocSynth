package com.docsynth.domain.ingestion;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Schema — denormalized, `$ref`-resolved schema from ApiSpec.
 * Per data-model.md §Schema.
 */
@Entity
@Table(name = "schemas")
public class Schema {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "api_spec_id", nullable = false)
    private UUID apiSpecId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "schema_json", columnDefinition = "jsonb", nullable = false)
    private String schemaJson;

    @Column(name = "schema_sha256", nullable = false)
    private String schemaSha256;

    protected Schema() {}

    public Schema(UUID apiSpecId, TenantId tenantId, String name, String schemaJson, String schemaSha256) {
        this.apiSpecId = apiSpecId;
        this.tenantId = tenantId.value();
        this.name = name;
        this.schemaJson = schemaJson;
        this.schemaSha256 = schemaSha256;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSchemaSha256() { return schemaSha256; }
}
