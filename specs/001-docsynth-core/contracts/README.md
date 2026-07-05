# Contracts: DocSynth Core

**Feature**: 001-docsynth-core
**Status**: Phase 1 design output

This directory contains the cross-language contract definitions for DocSynth. The product exposes two surfaces:

1. **External (operator + public)**: REST + OpenAPI 3.x — for the operator UI and for the Try It proxy endpoint.
2. **Internal (service-to-service)**: gRPC + Protocol Buffers — for the Java backend ↔ Python AI sidecar contract.

REST admin/health surfaces are also exposed for Kubernetes probes; they are intentionally minimal and not listed in detail here.

---

## Directory Layout

```text
contracts/
├── README.md                    # This file
├── proto/                       # gRPC service definitions (inter-service)
│   ├── ai_orchestration.proto
│   ├── ingestion.proto
│   └── drift.proto
└── openapi/                     # OpenAPI 3.x specifications (external REST)
    ├── operator-api.yaml
    └── proxy-api.yaml
```

**Note**: detailed protobuf message and OpenAPI path bodies are not fully generated in `plan.md` (which is design, not implementation). They are produced as part of `tasks.md` Phase 2 work. The service and message names listed below are the agreed contract surface.

---

## 1. External REST API (operator + Try It proxy)

### `operator-api.yaml`

The DocSynth operator UI. OAuth2 / OIDC-protected (FR-010). Tenant-scoped (FR-008, FR-012). All requests require a Bearer token; the tenant is derived from the token claims, never from query/path parameters (server-derived, fail-closed).

**Resource groups**:

| Group | Path prefix | Notes |
|---|---|---|
| Identity | `/api/v1/me`, `/api/v1/tenants/{tenantId}/members` | Current user, tenant roster |
| Projects | `/api/v1/tenants/{tenantId}/projects` | CRUD; role-gated |
| Specs | `/api/v1/projects/{projectId}/specs` | Ingest, list, retrieve parsed spec |
| DocSets | `/api/v1/projects/{projectId}/docsets` | Generate, publish, transition state, list |
| Drift | `/api/v1/projects/{projectId}/drift` | List reports, retrieve items, configure baselines |
| Notifications | `/api/v1/projects/{projectId}/channels` | CRUD for Slack/email/webhook/CI channels |
| Try It allowlist | `/api/v1/projects/{projectId}/tryit/allowlist` | Add/revoke hosts |
| Try It secrets | `/api/v1/projects/{projectId}/tryit/secrets` | Reference Key Vault secrets (value never returned) |
| Audit | `/api/v1/projects/{projectId}/audit` | Read-only audit log |

**Key endpoints (representative)**:

- `POST /api/v1/projects/{projectId}/specs` — submit a new spec (body: `{sourceKind, sourceRef, accessTokenRef?}`). Returns `201 {specId}` and enqueues parse work. Rate-limited at 10 req/min/user.
- `GET /api/v1/projects/{projectId}/specs/{specId}` — retrieve parsed spec summary + endpoints.
- `POST /api/v1/projects/{projectId}/docsets` — generate + publish (`{specId, displayVersion}`). Returns `202 {docSetId}` and enqueues build work.
- `PATCH /api/v1/projects/{projectId}/docsets/{docSetId}/state` — transition state (body: `{action: "deprecate" | "archive" | "reactivate", sunsetAt?}`). Enforces FR-014.
- `GET /api/v1/projects/{projectId}/docsets` — list, filterable by state, paginated.
- `GET /api/v1/projects/{projectId}/drift?since=...&compatibility=breaking` — list reports.
- `POST /api/v1/projects/{projectId}/drift` — trigger an on-demand drift comparison.
- `POST /api/v1/projects/{projectId}/channels` — register a notification channel (body: `{kind, name, configRef, ...}`).
- `POST /api/v1/projects/{projectId}/tryit/allowlist` — add a host pattern (audited, requires Editor or Owner).
- `GET /api/v1/tenants/{tenantId}/projects` — list projects for the calling user's tenant.

**Error contract**: standard Problem Details (RFC 9457) for 4xx/5xx. The `correlation_id` from the trace is included in the `traceId` extension field.

**Authentication**: OIDC Bearer token. The OIDC `sub` is mapped to `users.id`; the OIDC `tenant` claim maps to `tenants.id`. Tokens are validated against the configured OIDC provider's JWKS endpoint.

**Authorization**: RBAC roles per project (Owner, Editor, Viewer) per FR-010. Role checks enforced in the application layer (Spring Security `@PreAuthorize`); tenant isolation enforced at the data layer (RLS).

### `proxy-api.yaml`

The Try It proxy endpoint. **Not** part of the operator API — this is a separate, narrowly-scoped, heavily-mitigated surface (per `research.md §4-5`).

- `POST /api/v1/proxy/try` — execute a request through the proxy.
  - Request body: `{specId, operationId, parameters, body?, headers?}` (browser cannot specify the target host — the proxy resolves it from the spec's `servers[]` + the tenant allowlist).
  - The proxy resolves the target, applies the allowlist, calls the customer API, returns the response.
  - Response: `{status, headers, body, durationMs, requestId}` (the `Authorization` header is never echoed in `headers`).
  - All calls audited with `(tenant, user, targetHost, method, path, status, bytes, duration, requestId)`. No headers, no body, no auth in the audit entry.

**Authentication**: same OIDC token. **Authorization**: any project member; the per-project `try_it_enabled` flag gates the whole surface.

**Critical mitigations** (must be implemented; not optional):
- URL allowlist per tenant (`try_it_allowlist`).
- Scheme allowlist (`https` by default; `http` only for explicitly listed dev hosts).
- Hostname → IP resolution with IP-class check (reject loopback, link-local, RFC1918, multicast, IPv6 ULA, cloud-metadata IPs).
- Outbound HTTP redirects disabled.
- Header sanitization (strip inbound auth from any source other than the secret store; strip `Proxy-*` / `X-Forwarded-*`).
- Request body cap (1 MB), request timeout (connect ≤ 5 s, read ≤ 30 s), response size cap (1 MB), content-type allowlist.
- Per-tenant and per-user rate limits with `429` + `Retry-After`.
- Session-bound, short-lived request token (HMAC of session + tenant + target + nonce + TTL).
- Append-only audit log.

---

## 2. Internal gRPC Contracts (Java backend ↔ Python AI sidecar)

The Java backend is the **client** of the Python sidecar for AI-bound work. The contracts below are the agreed wire formats.

### `ingestion.proto`

Service: `IngestionEnhancer` — used during spec parsing to enrich spec metadata (e.g., "what does this operation look like in plain English?" — though the v1 spec keeps description enhancement minimal).

```proto
syntax = "proto3";
package docsynth.ingestion.v1;

service IngestionEnhancer {
  // Enrich a parsed endpoint with derived metadata.
  rpc EnrichEndpoint(EnrichEndpointRequest) returns (EnrichEndpointResponse);

  // Suggest operationId for endpoints that lack one (edge case in spec).
  rpc SuggestOperationId(SuggestOperationIdRequest) returns (SuggestOperationIdResponse);
}

message EnrichEndpointRequest {
  string tenant_id = 1;          // asserted by Java; re-validated by Python
  string api_spec_id = 2;
  EndpointDescriptor endpoint = 3;
  TenantContext tenant_context = 10; // signed JWT for cross-service assertion
}

message EndpointDescriptor {
  string method = 1;
  string path = 2;
  string summary = 3;
  string description = 4;
  repeated string tags = 5;
}

message EnrichEndpointResponse {
  string enriched_description = 1;
  repeated string suggested_tags = 2;
  float confidence = 3;
}

message SuggestOperationIdRequest {
  string tenant_id = 1;
  EndpointDescriptor endpoint = 2;
  TenantContext tenant_context = 10;
}

message SuggestOperationIdResponse {
  string operation_id = 1;
  float confidence = 2;
}

message TenantContext {
  string jwt = 1; // signed JWT; signed by Java, validated by Python
}
```

### `documentation.proto`

Service: `DocGenerator` — generates code examples, enhances descriptions, suggests code-sample languages.

```proto
syntax = "proto3";
package docsynth.documentation.v1;

service DocGenerator {
  // Generate a code example for an endpoint in a target language.
  rpc GenerateCodeExample(GenerateCodeExampleRequest) returns (GenerateCodeExampleResponse);

  // Enhance or fill in a description from a short summary.
  rpc EnhanceDescription(EnhanceDescriptionRequest) returns (EnhanceDescriptionResponse);

  // Stream progress for long-running generation work (e.g., large docset).
  rpc GenerateDocSet(GenerateDocSetRequest) returns (stream GenerateDocSetProgress);
}

message GenerateCodeExampleRequest {
  string tenant_id = 1;
  string api_spec_id = 2;
  EndpointDescriptor endpoint = 3;
  string language = 4;  // e.g., "curl", "python", "java"
  TenantContext tenant_context = 10;
}

message GenerateCodeExampleResponse {
  string code = 1;
  string language = 2;
  string prompt_template_version = 3;  // for eval reproducibility
  float confidence = 4;
}

message EnhanceDescriptionRequest {
  string tenant_id = 1;
  string summary = 2;
  TenantContext tenant_context = 10;
}

message EnhanceDescriptionResponse {
  string enhanced = 1;
  string prompt_template_version = 2;
  float confidence = 3;
}

message GenerateDocSetRequest {
  string tenant_id = 1;
  string api_spec_id = 2;
  string doc_set_id = 3;
  repeated string target_languages = 4;
  TenantContext tenant_context = 10;
}

message GenerateDocSetProgress {
  oneof update {
    EndpointGenerated endpoint_done = 1;
    DocSetCompleted docset_done = 2;
    GenerationFailed failed = 3;
  }
}

message EndpointGenerated {
  string method = 1;
  string path = 2;
}

message DocSetCompleted {
  int32 endpoints_processed = 1;
  int32 endpoints_failed = 2;
}

message GenerationFailed {
  string reason = 1;
  bool retryable = 2;
}

message EndpointDescriptor {
  string method = 1;
  string path = 2;
  string summary = 3;
  string description = 4;
  repeated string tags = 5;
}

message TenantContext {
  string jwt = 1;
}
```

### `drift.proto`

Service: `DriftNarrator` (v2; not used in v1). Reserved for when semantic-diff / AI-narrated changelogs are added — out of scope for v1 per `spec.md` "Out of Scope" section, but the proto is reserved.

```proto
syntax = "proto3";
package docsynth.drift.v1;

service DriftNarrator {
  // Reserved for v2 — semantic changelog generation.
  rpc NarrateDrift(NarrateDriftRequest) returns (NarrateDriftResponse);
}

message NarrateDriftRequest {
  string tenant_id = 1;
  string drift_report_id = 2;
  TenantContext tenant_context = 10;
}

message NarrateDriftResponse {
  string changelog_markdown = 1;
  string prompt_template_version = 2;
}

message TenantContext {
  string jwt = 1;
}
```

### Cross-cutting: Tenant Context Assertion

Every gRPC request carries a `TenantContext.jwt`. The Java side signs this JWT with a private key (HS256 or RS256 depending on deployment); the Python side validates it before doing any work. The Python side MUST treat the in-message `tenant_id` as a hint, not an authority — the JWT is the authority. If the JWT is missing/invalid/expired, the call is rejected with `UNAUTHENTICATED` (`code = 16`).

### Streaming

`GenerateDocSet` returns a server-streaming response. The client (Java) reads progress and updates the `doc_sets` row's progress metadata. On `GenerationFailed { retryable: true }`, the Java side re-enqueues with exponential backoff via Service Bus.

### Versioning

Proto packages are versioned (`v1`). A breaking change → `v2`. gRPC allows multiple versions to coexist during migration; old clients keep working on the old version until they migrate.

---

## 3. MCP (Model Context Protocol) Adapter

Per the constitution, **MCP is for tool exposure**, not the main service contract. The Java side exposes an MCP server that wraps certain internal capabilities (e.g., "query this endpoint's doc string", "look up the schema for type X") as tools the AI sidecar can call back. This is a one-way contract: MCP is server-side only from Java's perspective; the Python side consumes the tools, the Java side provides them.

MCP contracts are not protobufs; they use the MCP JSON-RPC surface. The MCP tool catalog is generated from the domain model at startup and versioned with the service.

---

## 4. Open Questions (carried to implementation)

1. **gRPC vs HTTP/JSON for the sidecar during local dev**: gRPC everywhere is the production answer, but local dev may benefit from `grpc-web` or a thin REST shim. Decision: gRPC always; local dev runs the Python sidecar locally and the Java backend connects over `localhost:50051`.
2. **Proto contract versioning policy**: SemVer on the proto package. Major version bump for any wire-incompatible change.
3. **MCP tool catalog stability**: tools MUST be backwards-compatible within a major version; tool removal requires a `v2` MCP server.
