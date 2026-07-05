package com.docsynth.domain.project;

import com.docsynth.domain.user.UserId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_memberships")
public class ProjectMembership {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role; // owner | editor | viewer

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "granted_by")
    private UUID grantedBy;

    protected ProjectMembership() {}

    public ProjectMembership(ProjectId projectId, UserId userId, Role role, UserId grantedBy) {
        this.projectId = projectId.value();
        this.userId = userId.value();
        this.role = role.name().toLowerCase();
        this.grantedBy = grantedBy == null ? null : grantedBy.value();
    }

    public enum Role { OWNER, EDITOR, VIEWER }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UserId getUserId() { return new UserId(userId); }
    public Role getRole() {
        return Role.valueOf(role.toUpperCase());
    }
}
