package com.docsynth.domain.ingestion;

import java.util.Map;

public record SchemaDescriptor(String name, Map<String, Object> schema) {}
