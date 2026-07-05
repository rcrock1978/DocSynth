package com.docsynth.interfaces.api;

import com.docsynth.domain.proxy.TryItAllowlistEntry;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.persistence.TryItAllowlistJpaRepository;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TryItAllowlistController — CRUD on the per-project Try It allowlist.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tryit/allowlist")
public class TryItAllowlistController {

    private final TryItAllowlistJpaRepository repository;
    private final TenantContextResolver tenantContext;
    private final ProjectRbacFilter rbac;

    public TryItAllowlistController(
        TryItAllowlistJpaRepository repository,
        TenantContextResolver tenantContext,
        ProjectRbacFilter rbac
    ) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<TryItAllowlistEntry> add(
        @PathVariable UUID projectId,
        @RequestParam String hostPattern
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireEditor(pid);
        TryItAllowlistEntry entry = new TryItAllowlistEntry(
            pid, tenantContext.currentTenantId(),
            hostPattern, "operator_added",
            currentActorId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(entry));
    }

    @GetMapping
    public List<TryItAllowlistEntry> list(@PathVariable UUID projectId) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findActiveByProjectId(projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireEditor(new ProjectId(projectId));
        repository.revoke(id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentActorId() {
        var jwt = (org.springframework.security.oauth2.jwt.Jwt)
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id").toString());
    }
}
