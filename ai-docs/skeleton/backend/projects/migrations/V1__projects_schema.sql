-- Schema of the domain "projects". Forward-only (db.md 15). Skeleton: keep in step with ai-docs/software-elements.md.
CREATE SCHEMA IF NOT EXISTS projects;

-- A client project (aggregate root).
CREATE TABLE projects.projects (
    id uuid NOT NULL PRIMARY KEY,
    name text NOT NULL,
    wish text NOT NULL,
    status text NOT NULL CHECK (status IN ('draft', 'questions_ready', 'answered', 'requirements_ready', 'workflow_drafted', 'confirmed', 'executing', 'done', 'stopped', 'failed')),
    agent_harness text NOT NULL CHECK (agent_harness IN ('opencode', 'claude_code')),
    provider text NOT NULL CHECK (provider IN ('anthropic')),
    model text NOT NULL,
    backend_stack text CHECK (backend_stack IN ('java', 'csharp', 'python')),
    frontend_stack text CHECK (frontend_stack IN ('svelte', 'angular', 'nuxt')),
    document_language text NOT NULL DEFAULT 'en' CHECK (document_language IN ('en', 'de')),
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- A file or link added to the wish; material, never instructions.
CREATE TABLE projects.project_attachments (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES projects.projects (id) ON DELETE CASCADE,
    kind text NOT NULL CHECK (kind IN ('file', 'link')),
    name text NOT NULL,
    url text,
    media_type text,
    size_bytes bigint,
    content bytea,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((kind = 'link') = (url IS NOT NULL))
);

-- The brief written by the prompt agent.
CREATE TABLE projects.project_briefs (
    project_id uuid NOT NULL PRIMARY KEY REFERENCES projects.projects (id) ON DELETE CASCADE,
    goal text NOT NULL,
    actors jsonb NOT NULL DEFAULT '[]',
    domain_terms jsonb NOT NULL DEFAULT '[]',
    risks jsonb NOT NULL DEFAULT '[]',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- GitHub account of the person who started the project: each finished task is pushed to a public repository there, created on the first push. The token is never stored: it lives in engine memory for the session and the run only.
CREATE TABLE projects.github_connections (
    project_id uuid NOT NULL PRIMARY KEY REFERENCES projects.projects (id) ON DELETE CASCADE,
    account_login text NOT NULL,
    repository_name text NOT NULL,
    visibility text NOT NULL CHECK (visibility IN ('public')),
    branch text NOT NULL DEFAULT 'main',
    push_after_each_task boolean NOT NULL DEFAULT true,
    repository_url text,
    repository_created_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

