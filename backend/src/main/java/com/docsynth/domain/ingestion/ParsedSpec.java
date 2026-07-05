package com.docsynth.domain.ingestion;

import java.util.List;

/**
 * Result of parsing a spec text. EndpointDescriptors and SchemaDescriptors
 * are flat representations derived from the OpenAPI model. The use case
 * persists these into endpoints/schemas tables.
 */
public record ParsedSpec(
    String openapiVersion,
    String title,
    String specVersion,
    List<EndpointDescriptor> endpoints,
    List<SchemaDescriptor> schemas
) {
    public int endpointCount() { return endpoints.size(); }
    public int schemaCount() { return schemas.size(); }
}
