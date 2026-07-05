package com.docsynth.domain.proxy;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * TryItSecret — reference to a Key Vault secret used to authenticate
 * outbound Try It calls. The secret value is NEVER stored in this row;
 * the `keyvaultSecretRef` is the only field. Per data-model.md §TryItSecret.
 */
@Entity
@Table(name = "try_it_secrets")
public class TryItSecret {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "keyvault_secret_ref", nullable = false)
    private String keyvaultSecretRef;

    @Column(name = "last_rotated_at")
    private Instant lastRotatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected TryItSecret() {}

    public TryItSecret(
        ProjectId projectId, TenantId tenantId, String name, String keyvaultSecretRef
    ) {
        this.projectId = projectId.value();
        this.tenantId = tenantId.value();
        this.name = name;
        this.keyvaultSecretRef = keyvaultSecretRef;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public String getName() { return name; }
    public String getKeyvaultSecretRef() { return keyvaultSecretRef; }
    public Instant getLastRotatedAt() { return lastRotatedAt; }
    public void markRotated() { this.lastRotatedAt = Instant.now(); }
}
