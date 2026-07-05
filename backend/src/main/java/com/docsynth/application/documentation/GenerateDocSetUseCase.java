package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.ingestion.ApiSpecRepository;
import com.docsynth.domain.ingestion.Endpoint;
import com.docsynth.domain.ingestion.EndpointDescriptor;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.application.audit.AuditEventEnvelope;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.infrastructure.documentation.CodeExampleClient;
import com.docsynth.infrastructure.documentation.DescriptionEnhancerClient;
import com.docsynth.infrastructure.documentation.ManifestEmitter;
import com.docsynth.infrastructure.documentation.ViteSsgAdapter;
import com.docsynth.infrastructure.messaging.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GenerateDocSetUseCase — orchestrates doc generation (FR-003, SC-002).
 *
 * Steps:
 *  1. Load ApiSpec + its parsed endpoints.
 *  2. For each endpoint, generate code examples (cURL/Python/Java) by
 *     calling the gRPC DocGenerator sidecar.
 *  3. Enhance descriptions via gRPC.
 *  4. Run Vite SSG build to produce static HTML/JS/CSS for the DocSet.
 *  5. Emit manifest (index.json) with version + per-endpoint metadata.
 *  6. Persist DocSet row (active state; supersedes previous active).
 *  7. Outbox emit "docset.publish".
 *  8. Audit emit.
 */
@Service
public class GenerateDocSetUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateDocSetUseCase.class);

    private final ApiSpecRepository apiSpecRepository;
    private final DocSetRepository docSetRepository;
    private final ViteSsgAdapter ssgAdapter;
    private final CodeExampleClient codeClient;
    private final DescriptionEnhancerClient descClient;
    private final ManifestEmitter manifestEmitter;
    private final Outbox outbox;
    private final AuditEmitter auditEmitter;

    public GenerateDocSetUseCase(
        ApiSpecRepository apiSpecRepository,
        DocSetRepository docSetRepository,
        ViteSsgAdapter ssgAdapter,
        CodeExampleClient codeClient,
        DescriptionEnhancerClient descClient,
        ManifestEmitter manifestEmitter,
        Outbox outbox,
        AuditEmitter auditEmitter
    ) {
        this.apiSpecRepository = apiSpecRepository;
        this.docSetRepository = docSetRepository;
        this.ssgAdapter = ssgAdapter;
        this.codeClient = codeClient;
        this.descClient = descClient;
        this.manifestEmitter = manifestEmitter;
        this.outbox = outbox;
        this.auditEmitter = auditEmitter;
    }

    @Transactional
    public DocSet execute(GenerateDocSetCommand cmd) {
        // 1. Load spec + endpoints.
        ApiSpec spec = apiSpecRepository.findById(cmd.specId())
            .orElseThrow(() -> new IllegalArgumentException("Spec not found: " + cmd.specId()));
        List<Endpoint> endpoints = apiSpecRepository.findEndpointsBySpecId(cmd.specId().value(), cmd.tenantId());

        // 2. For each endpoint, generate code examples and enhance description.
        Map<UUID, Map<String, String>> examplesByEndpoint = new HashMap<>();
        Map<UUID, String> enhancedDescriptions = new HashMap<>();
        for (Endpoint ep : endpoints) {
            // The descriptor carries method/path; summary/description come from the entity.
            EndpointDescriptor desc = new EndpointDescriptor(
                ep.getMethod(), ep.getPath(), ep.getOperationId(),
                ep.getSummary(), ep.getDescription(),
                List.of(ep.getTags()), Map.of(), null, Map.of(), List.of(),
                ep.isDeprecated()
            );
            Map<String, String> langCode = new HashMap<>();
            for (String language : cmd.targetLanguages()) {
                var response = codeClient.generate(
                    cmd.tenantId().value().toString(),
                    spec.getId().value().toString(),
                    desc,
                    language
                );
                langCode.put(language, response.getCode());
            }
            examplesByEndpoint.put(ep.getId(), langCode);
            if (ep.getDescription() == null || ep.getDescription().isBlank()) {
                var enhanced = descClient.enhance(
                    cmd.tenantId().value().toString(),
                    ep.getSummary() != null ? ep.getSummary() : ep.getPath()
                );
                enhancedDescriptions.put(ep.getId(), enhanced.getEnhanced());
            }
        }

        // 3. Build static DocSet via Vite SSG.
        int built = ssgAdapter.build(spec, endpoints);

        // 4. Emit manifest.
        String manifestUri = manifestEmitter.emit(spec, cmd.displayVersion());

        // 5. Persist DocSet + supersede previous active.
        String storagePrefix = String.format("v%s/", cmd.displayVersion());
        DocSet docSet = new DocSet(
            spec.getProjectId(),
            cmd.tenantId(),
            spec.getId(),
            cmd.displayVersion(),
            storagePrefix,
            manifestUri,
            true,
            cmd.actorUserId()
        ).published();

        DocSet saved = docSetRepository.save(docSet);
        docSetRepository.supersedePreviousActive(spec.getProjectId().value(), saved.getId());

        // 6. Outbox.
        outbox.append("docset.publish", "doc_set", saved.getId().value(), Map.of(
            "docSetId", saved.getId().value().toString(),
            "projectId", saved.getProjectId().value().toString(),
            "tenantId", saved.getTenantId().value().toString(),
            "displayVersion", saved.getDisplayVersion(),
            "endpointCount", built
        ));

        // 7. Audit.
        auditEmitter.emit(new AuditEventEnvelope(
            spec.getTenantId(),
            spec.parsedByUserId() == null ? null : new com.docsynth.domain.user.UserId(spec.parsedByUserId()),
            "publish_docset",
            "doc_set",
            saved.getId().value(),
            saved.getProjectId().value(),
            AuditEventEnvelope.Outcome.SUCCESS,
            Map.of(
                "displayVersion", saved.getDisplayVersion(),
                "endpointCount", built
            )
        ));

        log.info("docset published docSetId={} version={} endpointCount={}",
            saved.getId(), saved.getDisplayVersion(), built);

        return saved;
    }
}
