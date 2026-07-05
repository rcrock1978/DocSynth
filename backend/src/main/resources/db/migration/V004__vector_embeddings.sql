-- V004__vector_embeddings.sql
-- Vector store for RAG over code examples and description embeddings.
-- Per data-model.md §Vector Store (pgvector).

CREATE TABLE example_embeddings (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   UUID NOT NULL,
  endpoint_id UUID NOT NULL REFERENCES endpoints(id) ON DELETE CASCADE,
  language    TEXT NOT NULL,
  code        TEXT NOT NULL,
  embedding   VECTOR(1536) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_example_embeddings_tenant ON example_embeddings (tenant_id);
CREATE INDEX idx_example_embeddings_endpoint ON example_embeddings (endpoint_id);
CREATE INDEX idx_example_embeddings_vector ON example_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE description_embeddings (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   UUID NOT NULL,
  endpoint_id UUID NOT NULL REFERENCES endpoints(id) ON DELETE CASCADE,
  text        TEXT NOT NULL,
  embedding   VECTOR(1536) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_description_embeddings_tenant ON description_embeddings (tenant_id);
CREATE INDEX idx_description_embeddings_endpoint ON description_embeddings (endpoint_id);
CREATE INDEX idx_description_embeddings_vector ON description_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

ALTER TABLE example_embeddings      ENABLE ROW LEVEL SECURITY;
ALTER TABLE description_embeddings  ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_example_embeddings ON example_embeddings
  USING (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_description_embeddings ON description_embeddings
  USING (tenant_id = current_tenant_id());
