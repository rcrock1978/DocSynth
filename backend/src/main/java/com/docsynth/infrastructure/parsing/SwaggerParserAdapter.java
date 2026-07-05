package com.docsynth.infrastructure.parsing;

import com.docsynth.domain.ingestion.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * SwaggerParserAdapter — adapter for the ParseSpecPort.
 * Uses swagger-parser (io.swagger.parser.v3) to load and resolve $refs.
 */
@Component
public class SwaggerParserAdapter implements ParseSpecPort {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public ParsedSpec parse(String specText, SpecSource source) {
        OpenAPI openAPI;
        try {
            // swagger-parser auto-detects YAML vs JSON.
            openAPI = new OpenAPIV3Parser().readContents(specText, null, null).getOpenAPI();
            if (openAPI == null) {
                // Fallback: convert Swagger 2.0 to OpenAPI 3.0.
                openAPI = new SwaggerConverter().readContents(specText, null, null).getOpenAPI();
            }
        } catch (Exception e) {
            throw new InvalidSpecException("Failed to parse OpenAPI spec: " + e.getMessage());
        }
        if (openAPI == null) {
            throw new InvalidSpecException("Spec is not a valid OpenAPI 3.x document");
        }

        String version = openAPI.getOpenapi() != null ? openAPI.getOpenapi() : "3.0.0";
        String title = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : null;
        String specVersion = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : null;

        List<EndpointDescriptor> endpoints = extractEndpoints(openAPI);
        List<SchemaDescriptor> schemas = extractSchemas(openAPI);

        return new ParsedSpec(version, title, specVersion, endpoints, schemas);
    }

    private List<EndpointDescriptor> extractEndpoints(OpenAPI openAPI) {
        List<EndpointDescriptor> out = new ArrayList<>();
        if (openAPI.getPaths() == null) return out;
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem item = pathEntry.getValue();
            for (var methodOp : methodOperations(item)) {
                Operation op = methodOp.value();
                if (op == null) continue;
                out.add(new EndpointDescriptor(
                    methodOp.key(),
                    path,
                    op.getOperationId(),
                    op.getSummary(),
                    op.getDescription(),
                    op.getTags() == null ? List.of() : new ArrayList<>(op.getTags()),
                    parametersAsMap(op),
                    requestBodyAsMap(op),
                    responsesAsMap(op),
                    securityAsList(op),
                    Boolean.TRUE.equals(op.getDeprecated())
                ));
            }
        }
        return out;
    }

    private List<Map.Entry<String, Operation>> methodOperations(PathItem item) {
        List<Map.Entry<String, Operation>> ops = new ArrayList<>();
        if (item.getGet() != null) ops.add(Map.entry("GET", item.getGet()));
        if (item.getPost() != null) ops.add(Map.entry("POST", item.getPost()));
        if (item.getPut() != null) ops.add(Map.entry("PUT", item.getPut()));
        if (item.getPatch() != null) ops.add(Map.entry("PATCH", item.getPatch()));
        if (item.getDelete() != null) ops.add(Map.entry("DELETE", item.getDelete()));
        if (item.getHead() != null) ops.add(Map.entry("HEAD", item.getHead()));
        if (item.getOptions() != null) ops.add(Map.entry("OPTIONS", item.getOptions()));
        if (item.getTrace() != null) ops.add(Map.entry("TRACE", item.getTrace()));
        return ops;
    }

    private Map<String, Object> parametersAsMap(Operation op) {
        return op.getParameters() == null
            ? Map.of()
            : Map.of("items", op.getParameters());
    }

    private Map<String, Object> requestBodyAsMap(Operation op) {
        if (op.getRequestBody() == null) return null;
        return Map.of(
            "required", Boolean.TRUE.equals(op.getRequestBody().getRequired()),
            "content", op.getRequestBody().getContent()
        );
    }

    private Map<String, Object> responsesAsMap(Operation op) {
        if (op.getResponses() == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        op.getResponses().forEach((status, response) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("description", response.getDescription());
            if (response.getContent() != null) entry.put("content", response.getContent());
            result.put(status, entry);
        });
        return result;
    }

    private List<Map<String, Object>> securityAsList(Operation op) {
        if (op.getSecurity() == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        op.getSecurity().forEach(requirement -> {
            requirement.forEach((name, scopes) -> {
                out.add(Map.of("name", name, "scopes", scopes == null ? List.of() : scopes));
            });
        });
        return out;
    }

    private List<SchemaDescriptor> extractSchemas(OpenAPI openAPI) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return List.of();
        }
        List<SchemaDescriptor> out = new ArrayList<>();
        openAPI.getComponents().getSchemas().forEach((name, schema) -> {
            Map<String, Object> map = yamlMapper.convertValue(schema, Map.class);
            out.add(new SchemaDescriptor(name, map == null ? Map.of() : map));
        });
        return out;
    }
}
