# Research: "Try It" Interactive API Console — Proxy vs Browser-Direct vs Hybrid

**Feature:** 001-docsynth-core
**Status:** Research (decision input for `plan.md`)
**Scope:** Architectural pattern for executing live customer API requests from inside browser-rendered API reference documentation.

---

## 1. Problem Statement

DocSynth renders OpenAPI/AsyncAPI reference docs in a customer's browser. The "Try It" affordance must issue a real request from the rendered page to the customer's target API so developers can validate endpoints, headers, and payloads against live behavior. The browser is the natural origin of the request, but the target API belongs to a different tenant (the customer's customer), and DocSynth's SaaS must behave as a responsible intermediary — not as an open relay that can be turned against customers or their upstreams.

The decision is: **where does the request terminate, and what is the trust boundary?**

---

## 2. The Three Architectural Patterns

### Pattern A — Server-side proxy (backend forwards to customer API)

The browser POSTs an "intent" to a DocSynth endpoint (`POST /api/try`). The DocSynth backend reconstructs the request and calls the customer API on behalf of the user, then streams the response back.

**Pros**
- **No CORS dependency.** Works for any customer API regardless of `Access-Control-Allow-Origin` configuration — the dominant reality, since most production APIs do not advertise a wildcard CORS policy for browser origins.
- **No preflight dance.** The browser sees a same-origin call; the proxy handles any HTTP method, custom header, or non-simple content type.
- **Authentication secrets stay out of the browser.** Customers can store credentials (Bearer tokens, HMAC signing keys, mTLS material) in DocSynth and inject them server-side; the browser never sees them.
- **Centralized policy enforcement.** URL allowlist, header sanitization, rate limiting, response capping, and audit logging happen in one place the operator controls.
- **Request signing is possible.** DocSynth can attach its own signature to outbound calls so a customer WAF can distinguish DocSynth-originated traffic from attacker traffic.
- **Predictable abuse surface.** The egress IP range is known and the set of destinations is bounded — both are auditable.

**Cons**
- **SSRF exposure.** A naïve proxy that accepts an arbitrary target URL is a textbook Server-Side Request Forgery: a logged-in user (or a stolen session) can pivot the proxy at cloud metadata endpoints (`169.254.169.254`), link-local addresses, RFC1918 ranges, internal services, or non-HTTP schemes (`file://`, `gopher://`, `dict://`).
- **Auth-header redaction hazard.** If the proxy forwards the user's `Authorization` header, it is acting as a credential relay. Logging must redact it; storage must be encrypted; retention must be short.
- **Liability for abuse.** DocSynth's egress is what hits the customer's API. A misbehaving user can flood, scan, or POST large payloads — and the customer sees DocSynth's IP, not the user's.
- **Operational cost.** Every Try It call traverses DocSynth's infrastructure, adding latency and bandwidth. Streaming responses (file downloads, SSE, WebSocket upgrade) is harder.
- **Stateful backend coupling.** The console is no longer a static asset; the proxy is a runtime dependency.

### Pattern B — Browser-direct (CORS must be open on customer API)

The browser issues the call directly to the customer API. The customer's API must return permissive CORS headers — typically `Access-Control-Allow-Origin` for the DocSynth docs origin (or `*` for non-credentialed reads).

**Pros**
- **Zero relay cost.** No DocSynth server hop; latency is purely the customer's network. Streaming works natively.
- **No DocSynth liability for traffic volume.** Customer sees the user's IP (or the user's edge). Try It can be rate-limited by the customer's own gateway.
- **Trivial to implement.** No backend, no allowlist, no audit infra.
- **Aligns with how OSS doc tools ship.** Swagger UI, Stoplight Elements, and Redoc all default to browser-direct.

**Cons**
- **CORS gating breaks the feature for most production APIs.** Real APIs rarely allow `*` with credentials; many disallow cross-origin entirely. Browser-direct silently fails for a large class of customers.
- **The user's auth credential must live in the browser.** Either the user pastes a token (acceptable for personal dev keys, dangerous for shared/seated creds) or DocSynth proxies the credential through, which re-introduces pattern A.
- **Preflight limitations.** Some browsers reject redirected preflighted requests; non-simple methods/headers require the customer to enumerate `Access-Control-Allow-Methods` / `Access-Control-Allow-Headers`.
- **No centralized observability.** DocSynth cannot enforce policy, log calls, or detect abuse because the request never touches its servers.
- **Customer-API owner cannot distinguish DocSynth traffic from random browser traffic.** No signing, no shared egress IP — harder for the customer to write WAF rules.

### Pattern C — Hybrid: signed/validated proxy with allowlist and per-tenant policy

The browser POSTs to a DocSynth proxy. The proxy **validates** the target against a per-tenant allowlist (host patterns drawn from the OpenAPI `servers` entries, plus any tenant-added hosts), then forwards the request with header sanitization, timeout, and response capping. Optional: DocSynth signs the outbound request with a tenant-specific key so the customer's gateway can verify origin.

**Pros**
- **CORS-agnostic** (like A): the customer's API does not need to open itself to browsers.
- **Bounded SSRF surface.** The allowlist reduces the set of valid targets from "anywhere" to "the hosts the customer declared in their OpenAPI spec." Tenant-added hosts require explicit operator action and become an audit point.
- **Tenant-scoped policy.** Different tenants can have different rate limits, response caps, header policies, and signing keys. The blast radius of a misconfigured policy is one tenant, not the whole platform.
- **Auditable.** DocSynth can prove to a customer exactly which calls were made, when, by whom, against which endpoint, with which status.
- **Plays well with the customer's security stack.** Signed egress + a known IP allowlist is exactly what an enterprise customer's API team can write a WAF rule for.

**Cons**
- **Most complex to build and operate.** Allowlist authoring, signature key management, header sanitization, response streaming, and per-tenant rate limiting are all first-class features.
- **The "tenant adds a host" path is still a soft SSRF surface.** The allowlist is a contract — it must be reviewed the same way a customer's CORS policy would be. Pattern C does not eliminate SSRF; it scopes it.
- **Streaming and long-lived responses** still need careful design (chunked transfer, backpressure, abort).
- **Latency floor** is the network path DocSynth → customer, plus the proxy hop. For geographically distant customers this is noticeable.

---

## 3. How Popular Doc Tools Handle This

| Tool | Default model | How target is selected | Notes |
|---|---|---|---|
| **Swagger UI** (open source) | Browser-direct | `servers[]` from the OpenAPI doc, plus an editable server field | Pure client-side; any CORS/auth limitation is the user's problem. |
| **SwaggerHub** (hosted) | Browser-direct with optional **proxy** behind a flag | Same as UI; hosted tier can proxy when the customer enables it | Proxy mode is opt-in precisely because of the SSRF/relay concerns. |
| **Stoplight Elements** | Browser-direct | `servers[]` from the OpenAPI doc | Embedded as a web component; assumes the customer has CORS configured or accepts direct browser calls. |
| **Redoc / Redocly** | Browser-direct | `servers[]` | Same model; no built-in proxy. |
| **ReadMe** | Hybrid: the customer's API must publish a CORS-allowing endpoint, **or** the customer can configure a ReadMe-hosted proxy | Per-API configuration in the ReadMe project | ReadMe documents both paths explicitly; the proxy is the recommended path for credentialed APIs. |
| **Mintlify** | Browser-direct with optional proxy for hosted plans | Configured per project | Hosted customers get the proxy; self-hosted embeds default to browser-direct. |
| **Postman / Insomnia** | Desktop app, no browser origin | The user's own machine issues the call | Out of scope for an in-browser console, but a useful reference for what developers expect. |

**Pattern across the industry:** OSS renderers are browser-direct by default because they cannot take a dependency on a hosted proxy. Hosted SaaS products almost universally offer an opt-in or default-on proxy because the CORS limitation is the #1 source of "Try It doesn't work" support tickets. **The trend is the hybrid/validated-proxy model**, with the allowlist and signing mitigations added as the product matures and enterprise customers ask for them.

---

## 4. Security Threat Model

The proxy pattern is the only one with a non-trivial server-side attack surface, so this section focuses on Pattern A/C. The threat model assumes: a logged-in DocSynth user is the "attacker" against an upstream API; the customer is a victim if DocSynth is misused; and DocSynth itself is the asset that must not be turned into a relay.

| Threat | Description | Mitigations |
|---|---|---|
| **SSRF (URL pivot)** | User submits a target URL pointing at `169.254.169.254`, `localhost`, RFC1918, link-local, multicast, or `file://`/`gopher://` schemes. | Per-tenant URL allowlist drawn from OpenAPI `servers[]`; resolve the hostname, **reject** if any returned IP is non-global; reject non-`http`/`https` schemes; disable HTTP redirects on the outbound client (to prevent redirect-based bypass); egress firewall as defense-in-depth. |
| **Auth-header leakage** | Proxy logs or returns the `Authorization` header (or `Cookie`, `X-API-Key`). | Strip auth headers from logs and from any error response; never echo request headers back in error bodies; rotate signing keys periodically. |
| **Credential exfiltration via response** | Customer API returns a response that contains someone else's secret (e.g., a misconfigured S3 endpoint). | Response size cap (e.g., 1 MB) and a content-type allowlist; truncate, do not stream, large bodies. |
| **Resource exhaustion (DoS against customer)** | User uses Try It to flood a customer's endpoint through DocSynth's egress. | Per-user and per-tenant rate limits; per-tenant concurrency caps; request timeout (connect + read); request body size cap. |
| **Resource exhaustion (DoS against DocSynth)** | User uses Try It to make DocSynth open unbounded sockets. | Same as above; also connection-pool limits; circuit breaker per target host. |
| **Unauthenticated invocation** | Someone hits the proxy endpoint without a DocSynth session. | Session-bound request token (HMAC of session, target, timestamp); short TTL; replay rejection. |
| **Replay / cross-tenant smuggling** | User captures a request and replays it against a different tenant's proxy. | Bind the request token to (user, tenant, target host); tenant ID is server-derived, not user-supplied. |
| **Audit gap** | Incident response can't tell what was sent. | Append-only audit log: tenant, user, target host, method, path, status, bytes, duration, request ID — but **never** headers, body, or auth. |
| **Redirect chain abuse** | Outbound `Location: …` header sends DocSynth to an internal host. | Disable redirects on the outbound client (or strictly revalidate each hop against the allowlist). |
| **DNS rebinding** | Hostname resolves to a public IP at validation time and a private IP at connect time. | Resolve once, validate, **then connect to the validated IP** (not the original hostname) using a custom resolver. |

The OWASP SSRF Prevention Cheat Sheet (Case 1, allowlist approach) is the canonical reference for the allowlist half of this list.

---

## 5. Required Proxy Mitigations (the non-negotiables if Pattern A or C is chosen)

If a proxy is part of the answer, these are the floor, not the ceiling:

1. **URL allowlist per tenant.** Default: hosts declared in the OpenAPI `servers[]`. Add-hosts requires explicit operator action and is recorded in the audit log.
2. **Scheme allowlist.** `https` only by default; `http` only for explicitly listed dev hosts.
3. **Hostname → IP resolution and IP-class check before connect.** Reject loopback, link-local, RFC1918, multicast, IPv6 ULA, cloud-metadata IPs.
4. **Disable outbound HTTP redirects** (or re-validate every hop against the allowlist).
5. **Header sanitization.** Strip `Cookie`, inbound auth from any source other than the tenant-configured secret store, and any header matching `Proxy-*` / `X-Forwarded-*`.
6. **Auth handling.** User-supplied tokens are accepted only in the request body, never echoed in error responses, and never logged. Tenant-configured secrets are injected server-side from a secret store and never returned to the browser.
7. **Request body cap** (e.g., 1 MB) and **request timeout** (connect ≤ 5 s, read ≤ 30 s).
8. **Response size cap** (e.g., 1 MB) and **content-type allowlist** (JSON, XML, text, octet-stream).
9. **Per-tenant and per-user rate limits** with a visible 429 response that includes a `Retry-After`.
10. **Append-only audit log** with request metadata (no payload, no headers).
11. **Session-bound, short-lived request token** (HMAC of session + tenant + target + nonce + TTL).
12. **Egress firewall** as defense-in-depth: known destination ranges only.

---

## 6. Decision

**Adopt Pattern C: a tenant-scoped, allowlist-validated server-side proxy with the full mitigation set in §5, and a tenant-configurable "browser-direct fallback" knob for tenants who prefer it.**

### Rationale

1. **CORS is the dominant failure mode for a "Try It" feature.** A multi-tenant SaaS that depends on every customer having permissive CORS will work for maybe a third of real customers. The proxy is the only model that ships a Try It button that actually works in the general case.
2. **The pattern is what the market has converged on.** ReadMe, Mintlify, and SwaggerHub's hosted tier all expose a proxy exactly because of (1). Choosing browser-direct would put DocSynth behind the curve on day one.
3. **SSRF is solvable, but only if the architecture takes the threat seriously.** Pattern A is a footgun; Pattern C scopes the SSRF surface to a per-tenant allowlist that the tenant explicitly owns. That is the same trust shift enterprise customers already accept when they onboard a vendor to call their APIs.
4. **Auth handling is only safe server-side.** Even customers willing to paste a token into the browser will not paste a production key. A proxy with a tenant-configured secret store is the only way to support credentialed Try It against real customer APIs.
5. **Auditability and abuse containment are a SaaS responsibility.** If DocSynth can be used to hammer a customer's API, DocSynth is on the hook. The proxy is also the rate limiter, the abuse detector, and the audit trail.

### Why not pure browser-direct (Pattern B)

Pattern B is the right default for an open-source renderer embedded on the customer's own site (where the customer's CORS is their problem and DocSynth has no server to host). It is the wrong default for a multi-tenant SaaS that promises the feature "just works" and is responsible for what flows through it. A browser-direct toggle remains available per tenant for low-risk use (read-only public APIs), but is not the primary path.

### Why not Pattern A (open proxy)

Pattern A is what a "naïve" first cut would build, and it is what gets a SaaS company on a CVE notification. The cost difference between A and C is small — an allowlist, a request token, and an audit log — and the liability difference is large.

### What we are explicitly not building in v1

- **Public URL shorteners or open relays.** No `POST /proxy { url: "anything" }`.
- **WebSocket and SSE replay.** Streaming is a v2 capability; v1 caps responses at a fixed size and treats the call as request/response.
- **Self-hosted proxy deployment.** The proxy runs in DocSynth's infrastructure, not in the customer's VPC. A bring-your-own-egress option is a v2 conversation for regulated customers.

---

## 7. Open Questions for `plan.md`

1. **Allowlist authoring.** How do tenants add hosts beyond their `servers[]`? Self-serve UI vs. support-ticket-only. Both should be auditable.
2. **Signing.** Do we sign outbound requests (so customers can WAF by signature), or rely on egress IP allowlisting? Signing is stronger but adds a key-management surface.
3. **Where the proxy lives.** Single-tenant proxy per workspace vs. shared multi-tenant proxy with strong isolation. Single-tenant is safer; shared is cheaper.
4. **Streaming.** For v1 we cap and buffer. For v2 we need chunked streaming, abort semantics, and a decision on SSE/WebSocket upgrade.
5. **Per-tenant quotas.** Where do they live, who enforces them, and how are they surfaced in the UI?
6. **Token storage.** When a tenant saves an API key for Try It, how is it stored, rotated, scoped, and revoked?

---

# Research: OpenAPI Structural Drift Detection

**Feature:** 001-docsynth-core
**Scope:** Library and pattern for detecting added/removed/changed endpoints, schemas, and parameters between two OpenAPI 3.x spec versions or between a spec and a published DocSet.

## Decision

**Use `org.openapitools.openapidiff:openapi-diff-core` (Java) for structural drift detection.**

Pre-classifies changes as `incompatible`, `compatible`, or `unclassified`. Pairs naturally with `swagger-parser` (already a transitive dep) for loading and resolving `$ref`s. Supports custom `PathMatcher` and SPI rules for product-specific classifications (e.g., marking a new optional field as "informational" rather than "changed").

## Rationale

- Pure Java/JVM library — no Node tooling, fits Spring backend without an extra build target.
- Maintained on Maven Central, current stable line is mature.
- Classifies compatibility out of the box, which is exactly what the DriftReport entity needs (added/removed/changed/breaking/non-breaking).
- SPI lets us add product-specific rules without forking.

## Alternatives considered

- **`@pb33f/openapi-changes` (Node) and Atlassian `openapi-diff` (Node)** — Rejected. Rejected outright by the constraint that the Java backend cannot pull Node tooling. Dragging in a Node service for one library is not justified.
- **`swagger-parser` + hand-rolled diff** — Rejected. Re-implementing the OAS breaking-rules matrix (request-side additions are breaking; response-side additions are non-breaking; parameter requirement changes; etc.) is 3–6 weeks of work for no win.
- **Generic JSON tree diffs (`deepdiff`, `jsondiff`)** — Rejected. No awareness of request-vs-response asymmetry, no concept of `$ref` resolution, no compatibility classification. Useful for "did anything change?" but not for "is the change breaking?".

## Breaking / non-breaking matrix (to encode in product rules)

| Change | Breaking? |
|---|---|
| Add a new endpoint | No |
| Remove an endpoint | Yes |
| Add a new required request parameter | Yes |
| Add a new optional request parameter | No |
| Add a new request body field (required) | Yes |
| Add a new request body field (optional) | No |
| Remove a request body field | Yes |
| Change a response schema field type (narrowing) | Yes |
| Add a new response field | No |
| Remove a response field | No (typically non-breaking for clients) |
| Change operationId | No (clients should not depend on it) |
| Add a new required response (e.g., 201 → 201/202) | No |
| Remove a response code | Yes (if it was documented) |
| Add a new security scheme requirement | Yes |
| Loosen a security requirement | No |

The `openapi-diff-core` library covers most of these; product-specific rules (especially around our notion of "informational" changes like adding an optional header) belong in a custom SPI rule set.

## Open questions for `plan.md`

1. Do we need a separate "informational" classification beyond compatible/incompatible? (e.g., "new optional header on existing endpoint" — interesting to a maintainer, not a breaking change.)
2. Where do we store the baseline? Per DocSet version, or per Project (last-published snapshot)?
3. Drift diff runs asynchronously (FR-003 implies ≤2 min) — which message queue? (Service Bus per constitution; confirmed.)

---

# Research: AI Orchestration (Java + Python split)

**Feature:** 001-docsynth-core
**Scope:** Where AI work runs and how it is orchestrated, given the constitution mandates both Java 21 / Spring AI and Python 3.12 / LangChain+LlamaIndex.

## Decision

**Java/Spring AI orchestrates. Python services (LangChain / LlamaIndex) perform model-bound work. Inter-service contract is gRPC primary, REST for admin/health. Tenant context is a signed JWT in gRPC metadata, asserted at the Java boundary and re-validated at the Python boundary.**

Spring AI is the right orchestrator because it speaks the same language as the rest of the backend, integrates with Spring's transaction/outbox/observability stack, and is where the "what should we ask the model?" logic belongs. The Python sidecar is where actual model invocation, retrieval pipelines, and long-running agent work happens, because the LangChain/LlamaIndex ecosystem is more mature on the Python side.

## Rationale

- Single-language orchestration (Java) keeps the request lifecycle, retries, idempotency keys, and tenant context in one transactional model.
- gRPC for the primary contract gives streaming (for agent/token progress) and a typed schema that both sides can codegen against.
- REST is kept only for admin/health, where gRPC's tooling friction is not worth the win.
- JWT-as-tenant-assertion survives sidecar restarts and re-issues without re-issuing certs. If security review demands mTLS, swap the assertion mechanism but keep the metadata shape.
- MCP (Model Context Protocol) is scoped to **tool exposure** (LLM-callable tools surfaced back to Spring), not the main service contract. MCP is weak for long-running agent RPC and tenant assertion enforcement.

## Alternatives considered

- **Pure Spring AI, no Python** — Rejected. LangChain/LlamaIndex have stronger agent and retrieval pipelines; rebuilding them in Java is a multi-quarter effort.
- **Pure Python (FastAPI + LangChain), no Spring AI** — Rejected. The constitution mandates Spring Boot as the application platform; orchestration in Java keeps everything else (transactions, RBAC, observability) consistent.
- **HTTP/REST between Java and Python** — Rejected as primary. Used for admin/health only. gRPC is preferred for hot-path calls for performance and schema enforcement.
- **MCP as the primary service contract** — Rejected. MCP is great for tool exposure but weak for long-running agent RPC and tenant assertion. The MCP server in this stack is an *adapter* the Java side uses to expose tools to models, not the main inter-service wire.

## Open questions for `plan.md`

1. Do we deploy the Python sidecar as a separate Kubernetes Deployment, or as a Knative/Container Apps sidecar on the same pod? Sidecar reduces network hops; separate Deployment is operationally simpler.
2. Which LLM provider is primary for generation? (Affects prompt-evals, cost model, region pinning.)
3. How is the model-evaluation harness wired in CI? (Constitution Principle III requires it.)
4. How do we version the gRPC contract and the prompt templates together?

---

# Research: Versioned Doc Set Publishing (render + storage + CDN)

**Feature:** 001-docsynth-core
**Scope:** How DocSynth publishes stable versioned URLs (`/v1/`, `/v2/`, …) where each version is an immutable snapshot, and how it serves the interactive "Try It" console against that snapshot.

## Decision

**Static-first hybrid. Pre-render all documentation pages at publish time into versioned, immutable prefixes in a single object store, served via CDN with content-hashed assets. Render the "Try It" console as a client island backed by a thin API proxy. Use a manifest-driven status model (`current` / `deprecated` / `archived` / `gone`) to drive routing, banners, and `410 Gone` retirement.**

## Rationale

- The product's contract is "stable URLs per version." SSG delivers that as a property of the storage model, not as code we must keep writing. An immutable prefix *is* the snapshot.
- CDN cost and latency dominate for doc sites. SSG puts the bytes one hop from the reader.
- `410 Gone` is correctly expressed as a static file with a cache header (RFC 9110). SSG makes this a one-line upload; SSR makes it a route handler.
- The interactive console is a small, well-defined exception. A client island + thin proxy is the standard pattern (Stripe, Twilio, ReadMe).
- Vue 3 is a first-class citizen in SSG (Vite SSG / Nuxt prerender); no impedance mismatch.

## Storage layout (single container, version-segmented prefixes)

- `index.json` — manifest: list of versions, status, per-version metadata.
- `v1/`, `v2/`, … — version prefixes; each is **read-only** once published.
  - `index.html` and per-endpoint pre-rendered pages.
  - `assets/app.[hash].js`, `assets/style.[hash].css` — content-hashed, shared across versions if unchanged.
  - `410.html` if archived — served for any unknown subpath under this prefix.
  - `_meta.json` — snapshot metadata: source spec SHA, build ID, publisher, timestamp, console bundle hash, retention tier.
- `current → v2` — pointer that resolves the unversioned `/` to the current version.
- `archived/v0/` — older versions moved to cool/archive storage tier.

## Cache headers

| Asset | Cache-Control |
|---|---|
| Hashed JS/CSS/fonts | `public, max-age=31536000, immutable` |
| HTML entry points | `public, max-age=300, s-maxage=86400, stale-while-revalidate=604800` |
| `410.html` | `public, max-age=86400` (per RFC 9110, 410 is cacheable) |
| `index.json` (manifest) | `public, max-age=60, s-maxage=300` |
| `/current/` | short `max-age` (always reflects current pointer) |

## Lifecycle policy

Versions move through storage tiers: hot (0–6 mo) → cool (6–18 mo) → archive (18+ mo). Lifecycle rules are part of the architecture, not an afterthought.

## Immutability rule

A published version's key prefix is read-only. Any post-publish typo fix is a new version (`v1.0.1`), never an in-place edit. This is what makes "is this URL the same as yesterday?" trivially `true`.

## Alternatives considered

- **Full SSR (no SSG)** — Rejected. Violates the snapshot contract unless the data layer reimplements immutability, and reintroduces a hot-path server for a read-only workload.
- **Pure SSG with no backend** — Rejected only because the spec explicitly requires the interactive console. If the console were removed, pure SSG would be even simpler.
- **Per-version subdomains (`v1.docs.example.com`)** — Rejected. Breaks link sharing, complicates certificates, fans out CDN config. Path-prefix versioning (`/v1/`) keeps one origin and one TLS cert.
- **Read-time DB snapshot per version** — Rejected. SSR with extra steps; same cost profile plus a new build pipeline in the data layer.
- **Git-backed docs (every version is a git tag, every URL is a Netlify deploy)** — Considered. It's SSG in disguise and the right model for open-source project docs. DocSynth is a product publishing user-generated API docs at higher cadence with a status lifecycle (current/deprecated/archived/gone) that the git-tag model expresses awkwardly. The object-store-with-manifest model captures the same immutability without coupling to a VCS workflow.

## Open questions for `plan.md`

1. CDN choice (Azure Front Door vs Cloudflare vs Fastly) — affects purge semantics and tier-aware origin shielding.
2. Build pipeline location: same Kubernetes cluster as backend, or separate build worker pool?
3. Per-version retention tier: is the 6/18-month policy productized as a configurable Project setting, or fixed?
4. Search index strategy: build-time full-text index per version, or external search service (e.g., Meilisearch / Azure Cognitive Search) keyed by version?
