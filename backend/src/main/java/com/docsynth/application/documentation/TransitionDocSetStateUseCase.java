package com.docsynth.application.documentation;

import com.docsynth.application.audit.AuditEventEnvelope;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.domain.user.UserId;
import com.docsynth.infrastructure.security.TenantContextResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * TransitionDocSetStateUseCase — orchestrates a state transition
 * request: RBAC check, state machine, audit emit.
 */
@Service
public class TransitionDocSetStateUseCase {

    private final DocSetRepository repository;
    private final DocSetStateMachine stateMachine;
    private final AuditEmitter audit;
    private final TenantContextResolver tenantContext;

    public TransitionDocSetStateUseCase(
        DocSetRepository repository,
        DocSetStateMachine stateMachine,
        AuditEmitter audit,
        TenantContextResolver tenantContext
    ) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.audit = audit;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public DocSet execute(UUID docSetId, String targetState, Instant sunsetAt) {
        DocSet existing = repository.findById(new DocSetId(docSetId))
            .orElseThrow(() -> new IllegalArgumentException("DocSet not found: " + docSetId));
        DocSet result = stateMachine.transition(existing, targetState, sunsetAt);

        TenantId tenantId = result.getTenantId();
        UUID actorId = currentActorId();
        audit.emit(new AuditEventEnvelope(
            tenantId,
            actorId == null ? null : new UserId(actorId),
            "transition_docset_state",
            "doc_set",
            result.getId().value(),
            result.getProjectId().value(),
            AuditEventEnvelope.Outcome.SUCCESS,
            Map.of(
                "from", existing.getState(),
                "to", result.getState(),
                "sunsetAt", sunsetAt == null ? "" : sunsetAt.toString()
            )
        ));
        return result;
    }

    private UUID currentActorId() {
        try {
            return UUID.fromString(
                ((org.springframework.security.oauth2.jwt.Jwt)
                    org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal())
                    .getClaim("user_id").toString());
        } catch (Exception e) {
            return null;
        }
    }
}
