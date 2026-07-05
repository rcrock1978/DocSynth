package com.docsynth.infrastructure.ai;

import org.springframework.stereotype.Component;

/**
 * MCP (Model Context Protocol) server exposing read-only domain tools
 * for the AI sidecar and any agent that wishes to query the DocSynth
 * domain model.
 *
 * Per the constitution: MCP is for tool exposure, not the main service
 * contract. The Java side is the MCP server; the Python sidecar (and
 * future agent runtimes) consume the tools.
 *
 * v1 tool surface (read-only):
 *  - find_endpoint(method, path) -> EndpointDescriptor
 *  - get_schema_for_endpoint(method, path) -> SchemaDescriptor
 *  - list_drift_items_since(timestamp) -> [DriftItem]
 */
@Component
public class McpServer {

    public McpServer() {
        // Tools are exposed over MCP transport (stdio or sse). Concrete
        // implementations are wired in a later task; the skeleton exists
        // so the package boundary is established and ArchUnit sees the
        // tool-exposure code in infrastructure/ai/ as planned.
    }
}
