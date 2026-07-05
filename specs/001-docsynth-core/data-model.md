# Data Model: DocSynth Core

**Feature**: 001-docsynth-core
**Source**: `specs/001-docsynth-core/spec.md` (FR-001..FR-014, SC-001..SC-007) + `research.md`
**Status**: Phase 1 design output

This document maps the spec's Key Entities to concrete persistence and contract shapes. It is the contract between the domain model and the data layer (PostgreSQL + pgvector) and the inter-service gRPC contracts.

---

## Entity Catalog

| Entity | Bounded Context | Storage |
|---|---|---|
| `Tenant` | identity | `tenants` table |
| `User` | identity | `users` table |
| `Project` | project | `projects` table |
| `ProjectMembership` | project (RBAC) | `project_memberships` table |
| `ApiSpec` | ingestion | `api_specs` table + object storage for raw spec |
| `Endpoint` | ingestion | `endpoints` table (denormalized from ApiSpec for query) |
| `Schema` | ingestion | `schemas` table (denormalized) |
| `DocSet` | documentation | `doc_sets` table + object storage for SSG output |
| `DocSetVersion` | documentation | `doc_set_versions` table (immutable per version) |
| `DriftReport` | drift | `drift_reports` table |
| `DriftItem` | drift | `drift_items` table (one row per change) |
| `NotificationChannel` | drift | `notification_channels` table |
| `TryItAllowlistEntry` | proxy | `try_it_allowlist` table |
| `TryItSecret` | proxy | Key Vault (reference only in DB) |
| `AuditEntry` | audit | `audit_entries` table (append-only) |

---

## Entity Definitions

### Tenant

A customer organization. The top of the multi-tenant hierarchy.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `name` | text NOT NULL | Display name |
| `slug` | text UNIQUE NOT NULL | URL-safe identifier |
| `status` | enum(`active`,`suspended`) NOT NULL DEFAULT `active` | |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Indexes: `UNIQUE(slug)`.
RLS policy: `tenant_id = current_setting('app.current_tenant')::uuid`.

### User

Identity subject. AuthN is delegated to OIDC provider; DocSynth stores the subject claim and a local profile.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `oidc_subject` | text UNIQUE NOT NULL | `sub` claim from OIDC |
| `oidc_issuer` | text NOT NULL | `iss` claim |
| `email` | text NOT NULL | From OIDC profile |
| `display_name` | text | |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |
| `last_seen_at` | timestamptz | |

A user may belong to multiple tenants. Membership is recorded in `project_memberships` (and a parallel `tenant_memberships` is implied).

### Project

Tenant-scoped container for one API product.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID NOT NULL FK→tenants | RLS key |
| `name` | text NOT NULL | |
| `slug` | text NOT NULL | Unique per tenant |
| `default_drift_channel_ids` | UUID[] | Default notification targets |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |
| `archived_at` | timestamptz | Soft delete |

Constraints: `UNIQUE(tenant_id, slug)`.
Indexes: `(tenant_id, archived_at)`.

### ProjectMembership (RBAC)

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID NOT NULL FK→projects | |
| `user_id` | UUID NOT NULL FK→users | |
| `role` | enum(`owner`,`editor`,`viewer`) NOT NULL | FR-010 |
| `granted_at` | timestamptz NOT NULL DEFAULT now() | |
| `granted_by` | UUID FK→users | |

Constraints: `UNIQUE(project_id, user_id)`.

### ApiSpec

A parsed OpenAPI 3.x specification, immutable once ingested.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID NOT NULL FK→projects | |
| `tenant_id` | UUID NOT NULL | RLS key (denormalized for RLS) |
| `source_kind` | enum(`url`,`file_upload`,`github_repo`) NOT NULL | FR-001 |
| `source_ref` | text NOT NULL | The URL, blob URI, or `repo:owner/name@ref` |
| `openapi_version` | text NOT NULL | e.g., "3.0.3", "3.1.0" |
| `raw_spec_uri` | text NOT NULL | Pointer to object storage for the original YAML/JSON |
| `spec_sha256` | text NOT NULL | Hash of canonicalized spec for drift baseline |
| `title` | text | From `info.title` |
| `spec_version` | text | From `info.version` (the API's own version) |
| `endpoint_count` | int NOT NULL | Computed at parse time |
| `schema_count` | int NOT NULL | Computed at parse time |
| `parsed_at` | timestamptz NOT NULL DEFAULT now() | |
| `parsed_by_user_id` | UUID FK→users | |

Constraints: `UNIQUE(project_id, spec_sha256)` — same content can be re-ingested but the SHA is the dedup key.
Indexes: `(project_id, parsed_at DESC)`.

### Endpoint

Denormalized for fast query; rebuilt from `raw_spec_uri` whenever an `api_specs` row is inserted (sync) — kept current.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `api_spec_id` | UUID NOT NULL FK→api_specs ON DELETE CASCADE | |
| `tenant_id` | UUID NOT NULL | RLS key (denormalized) |
| `operation_id` | text | May be null (edge case) |
| `method` | enum(GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS,TRACE) NOT NULL | |
| `path` | text NOT NULL | Normalized |
| `summary` | text | |
| `description` | text | |
| `tags` | text[] | |
| `parameters` | jsonb | |
| `request_body_schema_id` | UUID FK→schemas | Nullable |
| `response_schemas` | jsonb | Map of status code → schema_id |
| `security_requirements` | jsonb | |
| `deprecated` | bool NOT NULL DEFAULT false | |

Constraints: `UNIQUE(api_spec_id, method, path)`.
Indexes: `(api_spec_id)`, `(tenant_id, tags)`.

### Schema

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `api_spec_id` | UUID NOT NULL FK→api_specs ON DELETE CASCADE | |
| `tenant_id` | UUID NOT NULL | RLS key (denormalized) |
| `name` | text NOT NULL | |
| `schema_json` | jsonb NOT NULL | Resolved (no `$ref`s) |
| `schema_sha256` | text NOT NULL | |

Constraints: `UNIQUE(api_spec_id, schema_sha256)`.

### DocSet

A generated documentation set derived from an `ApiSpec` at a specific version. Belongs to a Project; the unit of publishing.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID NOT NULL FK→projects | |
| `tenant_id` | UUID NOT NULL | RLS key |
| `api_spec_id` | UUID NOT NULL FK→api_specs | |
| `state` | enum(`active`,`deprecated`,`archived`) NOT NULL DEFAULT `active` | FR-007, FR-014 |
| `display_version` | text NOT NULL | e.g., "1.2.0" — the version string shown in URLs |
| `storage_prefix` | text NOT NULL | e.g., `v1.2.0/` — content-addressed, immutable |
| `manifest_uri` | text NOT NULL | Pointer to `index.json` in object storage |
| `try_it_enabled` | bool NOT NULL DEFAULT true | Per-project toggle |
| `generated_at` | timestamptz NOT NULL DEFAULT now() | |
| `generated_by_user_id` | UUID FK→users | |
| `published_at` | timestamptz | When first transitioned to `active` |
| `deprecated_at` | timestamptz | When transitioned to `deprecated` (FR-014) |
| `sunset_at` | timestamptz | Optional deprecation sunset date |
| `archived_at` | timestamptz | When transitioned to `archived` |
| `replacement_doc_set_id` | UUID FK→doc_sets | Set when deprecated |
| `gone_at` | timestamptz | When 410 is in effect (90d after archived) |
| `metadata` | jsonb | Build ID, console bundle hash, retention tier, etc. |

Constraints: `UNIQUE(project_id, display_version)`.
Indexes: `(project_id, state)`, `(tenant_id)`, `(gone_at)` (for the 410 worker).

**State transition rules** (FR-014):

| From → To | Actor | Notes |
|---|---|---|
| `active → deprecated` | Owner or Editor, manual | Optional `sunset_at` |
| `active → active` (supersede) | System on publish | New version auto-supersedes previous `active` |
| `deprecated → active` | Owner or Editor, manual | Revert (corrects mistake) |
| `deprecated → archived` | Owner or Editor, manual | Min 90 days after `deprecated_at` |
| `archived → active` | — | Not allowed; publish a new DocSet |
| any → any | — | All transitions append to `audit_entries` |

**Side effects**:
- `→ deprecated`: deprecation banner partial uploaded; manifest updated.
- `→ archived`: `archived_at` set; after 90d the 410 worker sets `gone_at` and the path prefix serves `410.html`.
- Archived URLs return `410 Gone` once `gone_at` is set.

### DriftReport

A comparison result between two specs or between a spec and a published DocSet.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID NOT NULL FK→projects | |
| `tenant_id` | UUID NOT NULL | RLS key |
| `left_spec_id` | UUID FK→api_specs NOT NULL | The newer (or current) spec |
| `right_spec_id` | UUID FK→api_specs NOT NULL | The baseline (older or published) |
| `trigger` | enum(`scheduled`,`webhook`,`manual`,`publish`) NOT NULL | |
| `triggered_by_user_id` | UUID FK→users | NULL when trigger = `webhook`/`scheduled` |
| `summary` | jsonb NOT NULL | `{added: N, removed: N, changed: N, breaking: N}` |
| `generated_at` | timestamptz NOT NULL DEFAULT now() | |
| `report_uri` | text | Optional pointer to full diff in object storage |
| `notification_status` | enum(`pending`,`sent`,`failed`,`skipped`) NOT NULL DEFAULT `pending` | |

Indexes: `(project_id, generated_at DESC)`, `(notification_status)`.

### DriftItem

One row per detected change within a `DriftReport`.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `drift_report_id` | UUID NOT NULL FK→drift_reports ON DELETE CASCADE | |
| `tenant_id` | UUID NOT NULL | RLS key (denormalized) |
| `change_kind` | enum(`added`,`removed`,`changed`) NOT NULL | FR-004 |
| `compatibility` | enum(`breaking`,`non_breaking`,`informational`) NOT NULL | From `openapi-diff-core` + custom SPI rules |
| `target_kind` | enum(`endpoint`,`schema`,`parameter`,`response`,`security`) NOT NULL | |
| `target_path` | text NOT NULL | e.g., `GET /users/{id}` |
| `detail` | jsonb NOT NULL | Structured detail of what changed |
| `message` | text NOT NULL | Human-readable summary |

Indexes: `(drift_report_id)`, `(compatibility)`.

### NotificationChannel

A configured target for drift alerts (FR-005).

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID FK→projects | Project-scoped; NULL for tenant-default |
| `tenant_id` | UUID NOT NULL | RLS key |
| `kind` | enum(`slack`,`email`,`webhook`,`ci_check`) NOT NULL | |
| `name` | text NOT NULL | Display label |
| `config_ref` | text NOT NULL | Key Vault path; never the secret itself |
| `enabled` | bool NOT NULL DEFAULT true | |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Secrets (webhook URLs, Slack tokens) are stored in Key Vault and referenced by `config_ref`. They MUST NEVER appear in the DB and MUST NEVER appear in any API response or log line (FR-011).

### TryItAllowlistEntry

A host pattern the tenant authorizes for the Try It proxy.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID NOT NULL FK→projects | |
| `tenant_id` | UUID NOT NULL | RLS key |
| `host_pattern` | text NOT NULL | e.g., `api.example.com` or `*.api.example.com` |
| `source` | enum(`from_servers`,`operator_added`) NOT NULL | `from_servers` seeded from OpenAPI `servers[]` |
| `added_by_user_id` | UUID FK→users | Required when source = `operator_added` |
| `added_at` | timestamptz NOT NULL DEFAULT now() | |
| `revoked_at` | timestamptz | |

Indexes: `(project_id, revoked_at)`.
The proxy resolves and validates targets against the union of `from_servers` (active) and `operator_added` (active) entries (see `research.md §4-5`).

### TryItSecret

Not stored in the DB. **Key Vault only.** DB stores a reference and metadata:

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `project_id` | UUID NOT NULL FK→projects | |
| `tenant_id` | UUID NOT NULL | RLS key |
| `name` | text NOT NULL | Display name, e.g., "Production API key" |
| `keyvault_secret_ref` | text NOT NULL | Full Key Vault secret URI |
| `last_rotated_at` | timestamptz | |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

The proxy resolves the secret at request time and **never** returns or logs it (FR-011).

### AuditEntry

Append-only audit log (FR-009). No UPDATE, no DELETE.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID NOT NULL | RLS key |
| `actor_user_id` | UUID FK→users | NULL for system actions |
| `action` | text NOT NULL | e.g., `ingest_spec`, `publish_docset`, `transition_docset_state`, `tryit_proxy_call` |
| `resource_kind` | text NOT NULL | e.g., `api_spec`, `doc_set`, `drift_report` |
| `resource_id` | UUID | |
| `project_id` | UUID FK→projects | |
| `correlation_id` | text | From distributed trace (FR-013) |
| `outcome` | enum(`success`,`failure`,`denied`) NOT NULL | |
| `detail` | jsonb | Structured detail; **no secrets, no payload bodies, no auth headers** (per proxy audit rules) |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Indexes: `(tenant_id, created_at DESC)`, `(project_id, created_at DESC)`, `(action)`, `(correlation_id)`.
RLS: read-only for project members; insert-only service principal; no UPDATE/DELETE permitted by application role.

---

## Relationships (ER overview)

```
Tenant 1───* User
Tenant 1───* Project
Project 1───* ProjectMembership *───1 User
Project 1───* ApiSpec 1───* Endpoint
                    │
                    └────* Schema
Project 1───* DocSet ─── (api_spec_id) ──→ ApiSpec
DocSet 1───0..1 DocSet (replacement_doc_set_id)
Project 1───* DriftReport 1───* DriftItem
                      │
                      └──── (left_spec_id, right_spec_id) ──→ ApiSpec
Project 1───* NotificationChannel
Project 1───* TryItAllowlistEntry
Project 1───* TryItSecret (Key Vault reference)
Tenant 1───* AuditEntry
```

---

## Row-Level Security (RLS) Policy

Every tenant-scoped table carries `tenant_id NOT NULL`. RLS policies use a session variable set at the start of every request:

```sql
SET LOCAL app.current_tenant = '<tenant-uuid>';
```

Policy template:

```sql
CREATE POLICY tenant_isolation ON <table>
  USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
```

The data layer MUST set `app.current_tenant` from the authenticated OIDC token before any query and MUST fail closed if the variable is unset (FR-012).

**Cross-tenant access MUST fail closed.** Service-to-service calls (e.g., the AI sidecar) pass the tenant assertion in a JWT; the database connection re-asserts it from that JWT.

---

## State Machine: DocSet

The `DocSet.state` transitions per FR-014 (see `DocSet` entity definition above). The state machine is enforced in the application layer; the DB has no trigger (a misfiring trigger is harder to debug than a rejected service call). All transitions are append-only `AuditEntry` rows.

The `gone_at` timestamp is set by a scheduled job (cron in AKS, daily) that scans for `archived` DocSets whose `archived_at` is older than 90 days, sets `gone_at`, and updates the manifest in object storage so the CDN serves `410.html`.

---

## Vector Store (pgvector)

The constitution requires pgvector. The MVP uses it for **retrieval-augmented generation** of code examples and description enhancements: an example corpus is embedded alongside the spec, and the AI sidecar retrieves similar prior examples when generating new ones.

| Table | Columns | Notes |
|---|---|---|
| `example_embeddings` | `id`, `tenant_id`, `endpoint_id`, `language`, `code`, `embedding vector(1536)`, `created_at` | Embeddings for the example corpus |
| `description_embeddings` | `id`, `tenant_id`, `endpoint_id`, `text`, `embedding vector(1536)`, `created_at` | Used for "find similar endpoint descriptions" suggestions |

Vector columns use the same `tenant_id` + RLS. Embedding dimension is 1536 (matches the primary embedding model; revisitable if the model changes).

---

## Out-of-Scope (Persistence)

The following are intentionally **not** in this data model and would be added in a future feature:

- GraphQL/gRPC/AsyncAPI spec storage (deferred per Out-of-Scope section in `spec.md`).
- Semantic diff storage (only structural drift is detected).
- On-prem / customer-managed storage (DocSynth is multi-tenant SaaS only).
- Enterprise SSO / SCIM tables (per-tenant SSO is deferred).
- AI-generated changelog entries (deferred).

---

## Open Questions (carried forward to implementation)

1. **Drift baseline strategy**: per DocSet version, or per Project (last-published snapshot)? Spec implies "previously published docs" (SC-003). Decision: per DocSet — the latest published DocSet per project is the baseline; spec updates produce drift against that baseline.
2. **Retention tier default for new DocSets**: hot (0–6 mo), cool (6–18 mo), archive (18+ mo). Hot by default. Configurable per Project in v2.
3. **Per-tenant rate limit values for the Try It proxy**: per-tenant token-bucket in Redis. Default values TBD (start: 60 req/min/user, 600 req/min/tenant).
