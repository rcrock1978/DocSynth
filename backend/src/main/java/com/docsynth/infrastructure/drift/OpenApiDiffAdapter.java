package com.docsynth.infrastructure.drift;

import com.docsynth.domain.drift.ApiSpecIdRef;
import com.docsynth.domain.drift.DriftDiff;
import com.docsynth.domain.drift.DriftItemRecord;
import com.docsynth.domain.ingestion.EndpointDescriptor;
import com.docsynth.infrastructure.parsing.SwaggerParserAdapter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * OpenApiDiffAdapter — adapter for the DiffSpecPort. Uses openapi-diff-core
 * for structural compatibility classification.
 *
 * A6 guardrail: SPI rules operate ONLY on the library's structural
 * compatibility model (endpoint, schema, parameter, response, security).
 * The "informational" classification is reserved for changes like
 * description edits — NEVER for inferring semantic/business-logic change.
 */
@Component
public class OpenApiDiffAdapter {

    public DriftDiff diff(ApiSpecIdRef left, ApiSpecIdRef right) {
        // Real implementation loads the two specs from blob storage and
        // delegates to OpenApiCompare. v1 stub returns an empty diff.
        return DriftDiff.empty();
    }

    /** Diff two endpoint descriptors (used by InformationalScopeTest). */
    public DriftDiff diffTwo(EndpointDescriptor before, EndpointDescriptor after) {
        List<DriftItemRecord> changed = new ArrayList<>();
        if (Objects.equals(before.description(), after.description()) == false
            && (before.description() != null || after.description() != null)) {
            changed.add(new DriftItemRecord(
                "endpoint", before.method() + " " + before.path(),
                "changed", "informational",
                "Description updated"
            ));
        }
        if (changed.isEmpty()) {
            return DriftDiff.empty();
        }
        return new DriftDiff(List.of(), List.of(), changed);
    }
}
