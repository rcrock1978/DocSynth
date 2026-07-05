-- V003__rls_policies.sql
-- Row-Level Security enforcement on every tenant-scoped table (FR-012).
-- Pattern: app.current_tenant session variable set by TenantContextResolver.

-- Helper function to read the current tenant (NULL if unset).
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID
LANGUAGE SQL STABLE AS $$
  SELECT NULLIF(current_setting('app.current_tenant', TRUE), '')::UUID
$$;

-- Enable RLS on every tenant-scoped table.
ALTER TABLE projects              ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_memberships   ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_specs             ENABLE ROW LEVEL SECURITY;
ALTER TABLE endpoints             ENABLE ROW LEVEL SECURITY;
ALTER TABLE schemas               ENABLE ROW LEVEL SECURITY;
ALTER TABLE doc_sets              ENABLE ROW LEVEL SECURITY;
ALTER TABLE drift_reports         ENABLE ROW LEVEL SECURITY;
ALTER TABLE drift_items           ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE try_it_allowlist      ENABLE ROW LEVEL SECURITY;
ALTER TABLE try_it_secrets        ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_entries         ENABLE ROW LEVEL SECURITY;

-- Policy template: deny access unless tenant_id matches current_tenant_id().
-- The TenantContextResolver sets app.current_tenant before any query.
-- If the variable is unset (NULL), the policy yields zero rows — fail-closed.

CREATE POLICY tenant_isolation_projects ON projects
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_api_specs ON api_specs
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_endpoints ON endpoints
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_schemas ON schemas
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_doc_sets ON doc_sets
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_drift_reports ON drift_reports
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_drift_items ON drift_items
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_notification_channels ON notification_channels
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_try_it_allowlist ON try_it_allowlist
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_try_it_secrets ON try_it_secrets
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_audit_entries ON audit_entries
  USING (tenant_id = current_tenant_id());

-- users and project_memberships: tenant comes via JOIN; explicit policy enforces
-- membership-driven access. The application sets app.current_tenant, then a query
-- like "list users in projects where I'm a member" naturally scopes via project_memberships.

CREATE POLICY tenant_isolation_project_memberships ON project_memberships
  USING (
    EXISTS (
      SELECT 1 FROM projects p
      WHERE p.id = project_memberships.project_id
        AND p.tenant_id = current_tenant_id()
    )
  );

-- Bypass for migrations / admin tooling (e.g., Flyway) — use a separate role.
-- The application role MUST be subject to RLS; only the migration role is exempted.
