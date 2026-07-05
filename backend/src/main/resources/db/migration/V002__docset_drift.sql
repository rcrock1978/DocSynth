-- V002__docset_drift.sql
-- DocSets, drift, notifications, try-it allowlist + secrets, audit log.
-- Per data-model.md §DocSet, §DriftReport, §DriftItem, §NotificationChannel,
-- §TryItAllowlistEntry, §TryItSecret, §AuditEntry.

CREATE TABLE doc_sets (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id               UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  tenant_id                UUID NOT NULL,
  api_spec_id              UUID NOT NULL REFERENCES api_specs(id),
  state                    TEXT NOT NULL DEFAULT 'active' CHECK (state IN ('active','deprecated','archived')),
  display_version          TEXT NOT NULL,
  storage_prefix           TEXT NOT NULL,
  manifest_uri             TEXT NOT NULL,
  try_it_enabled           BOOLEAN NOT NULL DEFAULT TRUE,
  generated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  generated_by_user_id     UUID REFERENCES users(id),
  published_at             TIMESTAMPTZ,
  deprecated_at            TIMESTAMPTZ,
  sunset_at                TIMESTAMPTZ,
  archived_at              TIMESTAMPTZ,
  replacement_doc_set_id   UUID REFERENCES doc_sets(id),
  gone_at                  TIMESTAMPTZ,
  metadata                 JSONB,
  UNIQUE (project_id, display_version)
);
CREATE INDEX idx_doc_sets_project_state ON doc_sets (project_id, state);
CREATE INDEX idx_doc_sets_tenant ON doc_sets (tenant_id);
CREATE INDEX idx_doc_sets_gone_at ON doc_sets (gone_at);

CREATE TABLE drift_reports (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id            UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  tenant_id             UUID NOT NULL,
  left_spec_id          UUID NOT NULL REFERENCES api_specs(id),
  right_spec_id         UUID NOT NULL REFERENCES api_specs(id),
  trigger               TEXT NOT NULL CHECK (trigger IN ('scheduled','webhook','manual','publish')),
  triggered_by_user_id  UUID REFERENCES users(id),
  summary               JSONB NOT NULL,
  generated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  report_uri            TEXT,
  notification_status   TEXT NOT NULL DEFAULT 'pending' CHECK (notification_status IN ('pending','sent','failed','skipped'))
);
CREATE INDEX idx_drift_reports_project_generated ON drift_reports (project_id, generated_at DESC);
CREATE INDEX idx_drift_reports_notification_status ON drift_reports (notification_status);

CREATE TABLE drift_items (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drift_report_id UUID NOT NULL REFERENCES drift_reports(id) ON DELETE CASCADE,
  tenant_id       UUID NOT NULL,
  change_kind     TEXT NOT NULL CHECK (change_kind IN ('added','removed','changed')),
  compatibility   TEXT NOT NULL CHECK (compatibility IN ('breaking','non_breaking','informational')),
  target_kind     TEXT NOT NULL CHECK (target_kind IN ('endpoint','schema','parameter','response','security')),
  target_path     TEXT NOT NULL,
  detail          JSONB NOT NULL,
  message         TEXT NOT NULL
);
CREATE INDEX idx_drift_items_report ON drift_items (drift_report_id);
CREATE INDEX idx_drift_items_compatibility ON drift_items (compatibility);

CREATE TABLE notification_channels (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id    UUID REFERENCES projects(id) ON DELETE CASCADE,
  tenant_id     UUID NOT NULL,
  kind          TEXT NOT NULL CHECK (kind IN ('slack','email','webhook','ci_check')),
  name          TEXT NOT NULL,
  config_ref    TEXT NOT NULL,  -- Key Vault path; never the secret itself
  enabled       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_channels_project ON notification_channels (project_id);

CREATE TABLE try_it_allowlist (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id       UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  tenant_id        UUID NOT NULL,
  host_pattern     TEXT NOT NULL,
  source           TEXT NOT NULL CHECK (source IN ('from_servers','operator_added')),
  added_by_user_id UUID REFERENCES users(id),
  added_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at       TIMESTAMPTZ
);
CREATE INDEX idx_try_it_allowlist_project ON try_it_allowlist (project_id, revoked_at);

CREATE TABLE try_it_secrets (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id            UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  tenant_id             UUID NOT NULL,
  name                  TEXT NOT NULL,
  keyvault_secret_ref   TEXT NOT NULL,
  last_rotated_at       TIMESTAMPTZ,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_try_it_secrets_project ON try_it_secrets (project_id);

-- Append-only audit log. Enforced at app layer + DB role permissions.
CREATE TABLE audit_entries (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       UUID NOT NULL,
  actor_user_id   UUID REFERENCES users(id),
  action          TEXT NOT NULL,
  resource_kind   TEXT NOT NULL,
  resource_id     UUID,
  project_id      UUID REFERENCES projects(id),
  correlation_id  TEXT,
  outcome         TEXT NOT NULL CHECK (outcome IN ('success','failure','denied')),
  detail          JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entries_tenant_created ON audit_entries (tenant_id, created_at DESC);
CREATE INDEX idx_audit_entries_project_created ON audit_entries (project_id, created_at DESC);
CREATE INDEX idx_audit_entries_action ON audit_entries (action);
CREATE INDEX idx_audit_entries_correlation ON audit_entries (correlation_id);

-- Revoked triggers: block UPDATE/DELETE on audit_entries via DB role permissions in production.
-- Application role grants: SELECT, INSERT only.
