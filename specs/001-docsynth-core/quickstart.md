# Quickstart: DocSynth Core

**Purpose**: Runnable end-to-end validation of each user story against a local or
staging DocSynth deployment.

## Prerequisites

- Docker (for Postgres+pgvector, Service Bus emulator, Key Vault emulator, Azurite blob)
- `make` (or invoke the underlying commands directly)
- A public OpenAPI 3.x spec URL (e.g., `https://petstore3.swagger.io/api/v3/openapi.json`)
- An OIDC dev tenant (e.g., Keycloak in dev profile) with one test user

## Setup

```bash
make setup   # boots Postgres+pgvector, Service Bus emulator, Key Vault emulator, Azurite
make seed    # creates dev tenant + project + Owner-role user
```

## US1 — Ingest (MVP)

```bash
make quickstart-us1
```

Submits `https://petstore3.swagger.io/api/v3/openapi.json` via the operator REST API.

**Expected**:
- `201 Created` on POST `/api/v1/projects/{id}/specs`
- GET `/api/v1/projects/{id}/specs/{specId}` returns parsed endpoints within 30 s (SC-001)
- `spec.parsed` event lands on the Service Bus topic
- `endpoint_count > 0`

## US2 — Doc Generation

```bash
make quickstart-us2
```

POSTs to `/api/v1/projects/{id}/docsets` with the spec from US1.

**Expected**:
- `202 Accepted` on POST; polling returns `state=active`
- `/v1/` serves the static SSG output
- `curl https://docs.localtest.me/v1/` returns HTML
- At least one code example is present in the rendered page

## US3 — Drift Detection

```bash
make quickstart-us3
```

Submits a modified copy of the same spec; configures a Slack channel pointing at a webhook capture.

**Expected**:
- Drift report within 2 minutes (SC-003)
- Webhook capture shows a Slack-formatted message
- Report contains ≥ 1 "changed" item; Slack message has the correct tenant prefix

## US4 — Try It Console

```bash
make quickstart-us4
```

Spins up a stub target API on `localhost:9090`; adds the host to the try-it allowlist.

**Expected**:
- Try It call returns `200` with the stub response within 5 s (SC-004)
- `AuditEntry` for `tryit_proxy_call` exists with `targetHost=localhost:9090`, `status=200`
- SSRF probe (`localhost:9091`) is rejected with `400 Bad Request`

## US5 — Versioned Publishing

```bash
make quickstart-us5
```

Publishes v1, then publishes a v2; PATCHes v1 to deprecated; archives v1; waits for 410 simulation (override env var for the 90-day timer).

**Expected**:
- `/v1/` shows deprecation banner
- `/v2/` shows new content
- After 90-day simulation, `/v1/` returns `410 Gone` (SC-007 archive retirement)
- Manifest `index.json` reflects state transitions; `gone_at` populated for v1

## Cleanup

```bash
make teardown
```
