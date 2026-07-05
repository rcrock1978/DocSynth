package com.docsynth.infrastructure.security;

import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.user.UserId;
import com.docsynth.application.project.ProjectMembershipService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * RBAC enforcement at the application layer (FR-010).
 *
 * Project roles: owner, editor, viewer. Resolved from project_memberships table
 * via ProjectMembershipService. Cached per-request.
 */
@Component
public class ProjectRbacFilter {

    public enum Role { OWNER, EDITOR, VIEWER }

    private static final Set<String> EDITOR_ROLES = Set.of("owner", "editor");
    private static final Set<String> VIEWER_ROLES = Set.of("owner", "editor", "viewer");

    private final ProjectMembershipService memberships;
    private final TenantContextResolver tenantContext;

    public ProjectRbacFilter(ProjectMembershipService memberships, TenantContextResolver tenantContext) {
        this.memberships = memberships;
        this.tenantContext = tenantContext;
    }

    public void requireViewer(ProjectId projectId) {
        require(projectId, VIEWER_ROLES);
    }

    public void requireEditor(ProjectId projectId) {
        require(projectId, EDITOR_ROLES);
    }

    public void requireOwner(ProjectId projectId) {
        require(projectId, Set.of("owner"));
    }

    private void require(ProjectId projectId, Set<String> allowed) {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userUuid = UUID.fromString(jwt.getClaim("user_id").toString());
        UserId userId = new UserId(userUuid);
        String role = memberships.roleFor(projectId, userId);
        if (role == null || !allowed.contains(role)) {
            throw new RbacDeniedException(
                "User " + userId + " lacks required role on project " + projectId
                    + " (required one of " + allowed + ", have " + role + ")");
        }
    }
}
