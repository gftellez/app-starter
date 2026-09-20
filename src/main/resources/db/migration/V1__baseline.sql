-- The baseline every new app starts from: the tenant table, and one example owned table
-- showing the shape every other table copies.
--
-- Conventions worth keeping:
--   * UUID primary keys, so an id can be generated before the row is written and never
--     leaks a row count.
--   * timestamptz, never timestamp — a naive timestamp silently means a different instant
--     depending on who reads it.
--   * numeric(19, 4) for anything that is money. Never float.
--   * Every owned table carries tenant_id NOT NULL with a foreign key, and an index on it,
--     because every query filters by it.

CREATE TABLE tenants (
    id   UUID PRIMARY KEY,
    name TEXT NOT NULL
);

-- DELETE ME once the app has tables of its own; keep the shape.
CREATE TABLE notes (
    id         UUID PRIMARY KEY,
    tenant_id  UUID        NOT NULL REFERENCES tenants (id),
    text       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notes_tenant ON notes (tenant_id, created_at DESC);
