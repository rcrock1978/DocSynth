-- V001__init.sql
-- Foundation schema: pgcrypto + pgvector extensions, tenants, users, projects,
-- project_memberships, api_specs, endpoints, schemas.
-- Per data-model.md §Tenant, §User, §Project, §ProjectMembership, §ApiSpec, §Endpoint, §Schema.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE tenants (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  slug        TEXT NOT NULL UNIQUE,
  status      TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  oidc_subject    TEXT NOT NULL,
  oidc_issuer     TEXT NOT NULL,
  email           TEXT NOT NULL,
  display_name    TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at    TIMESTAMPTZ,
  UNIQUE (oidc_issuer, oidc_subject)
);

CREATE TABLE projects (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   UUID NOT NULL REFERENCES tenants(id),
  name        TEXT NOT NULL,
  slug        TEXT NOT NULL,
  default_drift_channel_ids UUID[] NOT NULL DEFAULT '{}',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  archived_at TIMESTAMPTZ,
  UNIQUE (tenant_id, slug)
);
CREATE INDEX idx_projects_tenant_archived ON projects (tenant_id, archived_at);

CREATE TABLE project_memberships (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role        TEXT NOT NULL CHECK (role IN ('owner', 'editor', 'viewer')),
  granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  granted_by  UUID REFERENCES users(id),
  UNIQUE (project_id, user_id)
);

CREATE TABLE api_specs (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  tenant_id       UUID NOT NULL,
  source_kind     TEXT NOT NULL CHECK (source_kind IN ('url', 'file_upload', 'github_repo')),
  source_ref      TEXT NOT NULL,
  openapi_version TEXT NOT NULL,
  raw_spec_uri    TEXT NOT NULL,
  spec_sha256     TEXT NOT NULL,
  title           TEXT,
  spec_version    TEXT,
  endpoint_count  INTEGER NOT NULL DEFAULT 0,
  schema_count    INTEGER NOT NULL DEFAULT 0,
  parsed_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  parsed_by_user_id UUID REFERENCES users(id),
  UNIQUE (project_id, spec_sha256)
);
CREATE INDEX idx_api_specs_project_parsed_at ON api_specs (project_id, parsed_at DESC);

CREATE TABLE endpoints (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  api_spec_id           UUID NOT NULL REFERENCES api_specs(id) ON DELETE CASCADE,
  tenant_id             UUID NOT NULL,
  operation_id          TEXT,
  method                TEXT NOT NULL CHECK (method IN ('GET','POST','PUT','PATCH','DELETE','HEAD','OPTIONS','TRACE')),
  path                  TEXT NOT NULL,
  summary               TEXT,
  description           TEXT,
  tags                  TEXT[] NOT NULL DEFAULT '{}',
  parameters            JSONB,
  request_body_schema_id UUID,
  response_schemas      JSONB,
  security_requirements JSONB,
  deprecated            BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (api_spec_id, method, path)
);
CREATE INDEX idx_endpoints_api_spec ON endpoints (api_spec_id);
CREATE INDEX idx_endpoints_tenant_tags ON endpoints (tenant_id, tags);

CREATE TABLE schemas (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  api_spec_id   UUID NOT NULL REFERENCES api_specs(id) ON DELETE CASCADE,
  tenant_id     UUID NOT NULL,
  name          TEXT NOT NULL,
  schema_json   JSONB NOT NULL,
  schema_sha256 TEXT NOT NULL,
  UNIQUE (api_spec_id, schema_sha256)
);
