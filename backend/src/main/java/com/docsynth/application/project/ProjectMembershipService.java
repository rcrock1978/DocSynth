package com.docsynth.application.project;

import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.user.UserId;
import com.docsynth.infrastructure.persistence.ProjectMembershipRepository;
import org.springframework.stereotype.Service;

/**
 * Resolves a user's role on a project. Used by ProjectRbacFilter and
 * authorization checks in REST controllers.
 */
@Service
public class ProjectMembershipService {

    private final ProjectMembershipRepository repository;

    public ProjectMembershipService(ProjectMembershipRepository repository) {
        this.repository = repository;
    }

    /**
     * @return the role name (owner/editor/viewer) or null if the user is not a member.
     */
    public String roleFor(ProjectId projectId, UserId userId) {
        return repository.findByProjectAndUser(projectId.value(), userId.value())
            .map(pm -> pm.getRole().name().toLowerCase())
            .orElse(null);
    }
}
