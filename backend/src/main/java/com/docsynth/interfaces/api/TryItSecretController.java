package com.docsynth.interfaces.api;

import com.docsynth.domain.proxy.TryItSecret;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.persistence.TryItSecretJpaRepository;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TryItSecretController — manage Try It secret references. The secret
 * value is NEVER returned (FR-011).
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tryit/secrets")
public class TryItSecretController {

    private final TryItSecretJpaRepository repository;
    private final TenantContextResolver tenantContext;
    private final ProjectRbacFilter rbac;

    public TryItSecretController(
        TryItSecretJpaRepository repository,
        TenantContextResolver tenantContext,
        ProjectRbacFilter rbac
    ) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<TryItSecret> add(
        @PathVariable UUID projectId,
        @RequestParam String name,
        @RequestParam String keyvaultSecretRef
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireOwner(pid);
        TryItSecret secret = new TryItSecret(pid, tenantContext.currentTenantId(), name, keyvaultSecretRef);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(secret));
    }

    @GetMapping
    public List<TryItSecret> list(@PathVariable UUID projectId) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findByProjectIdAndTenantId(projectId, tenantContext.currentTenantId());
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<TryItSecret> rotate(
        @PathVariable UUID projectId,
        @PathVariable UUID id,
        @RequestParam String newKeyvaultSecretRef
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireOwner(pid);
        TryItSecret secret = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("TryItSecret not found: " + id));
        TryItSecret updated = new TryItSecret(pid, secret.getTenantId(), secret.getName(), newKeyvaultSecretRef);
        updated.markRotated();
        return ResponseEntity.ok(repository.save(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireOwner(new ProjectId(projectId));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
