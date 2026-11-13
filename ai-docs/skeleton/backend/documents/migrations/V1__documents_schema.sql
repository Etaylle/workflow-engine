-- Schema of the domain "documents". Forward-only (db.md 15). Skeleton: keep in step with ai-docs/software-elements.md.
CREATE SCHEMA IF NOT EXISTS documents;

-- An epic = one domain of the client software; E-0 is the template setup.
CREATE TABLE documents.epics (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    key text NOT NULL,
    name text NOT NULL,
    domain_name text NOT NULL,
    seq integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (project_id, key)
);

-- One requirement with ID, priority and source.
CREATE TABLE documents.requirements (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    epic_id uuid REFERENCES documents.epics (id) ON DELETE CASCADE,
    key text NOT NULL,
    type text NOT NULL CHECK (type IN ('functional', 'quality', 'constraint')),
    text text NOT NULL,
    priority text NOT NULL CHECK (priority IN ('must', 'should', 'could', 'wont')),
    source text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (project_id, key)
);

-- Rendered requirements and project plan (insert-only).
CREATE TABLE documents.generated_documents (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    kind text NOT NULL CHECK (kind IN ('requirements', 'project_plan')),
    doc_version integer NOT NULL,
    file_format text NOT NULL CHECK (file_format IN ('docx', 'md', 'pdf')),
    content bytea NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (project_id, kind, doc_version, file_format)
);

CREATE INDEX requirements_project_idx ON documents.requirements (project_id);
