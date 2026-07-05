package com.docsynth.domain.ingestion;

import java.util.List;
import java.util.Map;

public record EndpointDescriptor(
    String method,
    String path,
    String operationId,
    String summary,
    String description,
    List<String> tags,
    Map<String, Object> parameters,
    Map<String, Object> requestBody,
    Map<String, Object> responses,
    List<Map<String, Object>> securityRequirements,
    boolean deprecated
) {}
