package com.docsynth.interfaces.api;

import com.docsynth.application.documentation.DocSetStateMachine;
import com.docsynth.application.documentation.TransitionDocSetStateUseCase;
import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.interfaces.api.dto.DocSetSummary;
import com.docsynth.interfaces.api.dto.TransitionDocSetStateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * DocSetStateController — PATCH endpoint for state transitions (FR-014).
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/docsets/{docSetId}/state")
public class DocSetStateController {

    private final TransitionDocSetStateUseCase transition;
    private final ProjectRbacFilter rbac;

    public DocSetStateController(TransitionDocSetStateUseCase transition, ProjectRbacFilter rbac) {
        this.transition = transition;
        this.rbac = rbac;
    }

    @PatchMapping
    public DocSetSummary transition(
        @PathVariable UUID projectId,
        @PathVariable UUID docSetId,
        @Valid @RequestBody TransitionDocSetStateRequest body
    ) {
        rbac.requireEditor(new ProjectId(projectId));
        DocSet result = transition.execute(docSetId, body.action(), body.sunsetAt());
        return new DocSetSummary(
            result.getId().value(),
            result.getDisplayVersion(),
            result.getState(),
            result.isTryItEnabled(),
            result.getGeneratedAt(),
            result.getPublishedAt(),
            result.getDeprecatedAt(),
            result.getArchivedAt(),
            result.getGoneAt()
        );
    }
}
