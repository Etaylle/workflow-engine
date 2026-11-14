-- Schema of the domain "workflows". Forward-only: a released migration is never edited.
CREATE SCHEMA IF NOT EXISTS workflows;

-- The business workflow of a project (aggregate root).
CREATE TABLE workflows.workflows (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL UNIQUE,
    status text NOT NULL CHECK (status IN ('draft', 'valid', 'invalid', 'confirmed')),
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- A BPMN lane = an actor (Student, Kitchen).
CREATE TABLE workflows.lanes (
    id uuid NOT NULL PRIMARY KEY,
    workflow_id uuid NOT NULL REFERENCES workflows.workflows (id) ON DELETE CASCADE,
    name text NOT NULL,
    seq integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (workflow_id, seq)
);

-- A start event, task or end event on the canvas; version guards edits from two tabs.
CREATE TABLE workflows.nodes (
    id uuid NOT NULL PRIMARY KEY,
    workflow_id uuid NOT NULL REFERENCES workflows.workflows (id) ON DELETE CASCADE,
    lane_id uuid NOT NULL REFERENCES workflows.lanes (id) ON DELETE CASCADE,
    kind text NOT NULL CHECK (kind IN ('start_event', 'task', 'end_event')),
    title text NOT NULL,
    description text,
    task_type text CHECK (task_type IN ('software', 'manual')),
    epic_key text,
    story_key text,
    story_type text CHECK (story_type IN ('feature', 'technical', 'defect', 'spike', 'audit')),
    pos_x integer NOT NULL,
    pos_y integer NOT NULL,
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((kind = 'task') = (task_type IS NOT NULL)),
    CHECK (kind = 'task' OR story_type IS NULL)
);

-- A sequence flow arrow.
CREATE TABLE workflows.edges (
    id uuid NOT NULL PRIMARY KEY,
    workflow_id uuid NOT NULL REFERENCES workflows.workflows (id) ON DELETE CASCADE,
    source_node_id uuid NOT NULL REFERENCES workflows.nodes (id) ON DELETE CASCADE,
    target_node_id uuid NOT NULL REFERENCES workflows.nodes (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (source_node_id, target_node_id),
    CHECK (source_node_id <> target_node_id)
);

-- "Done when" of a task.
CREATE TABLE workflows.acceptance_criteria (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL REFERENCES workflows.nodes (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    text text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (node_id, seq)
);

-- A test scenario (Given-When-Then) written by the AI from a criterion.
CREATE TABLE workflows.scenarios (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL REFERENCES workflows.nodes (id) ON DELETE CASCADE,
    criterion_id uuid REFERENCES workflows.acceptance_criteria (id) ON DELETE CASCADE,
    seq integer NOT NULL,
    gherkin text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (node_id, seq)
);

-- Which requirements a task implements (key only: no cross-schema join).
CREATE TABLE workflows.node_requirements (
    node_id uuid NOT NULL REFERENCES workflows.nodes (id) ON DELETE CASCADE,
    requirement_key text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (node_id, requirement_key)
);

-- Frozen copy of wish, answers, stack, AI, requirements, workflow, team and development workflow (insert-only).
CREATE TABLE workflows.contracts (
    id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    contract_version integer NOT NULL,
    snapshot jsonb NOT NULL,
    cost_limit_amount numeric(12,2) NOT NULL,
    cost_limit_currency text NOT NULL DEFAULT 'USD',
    confirmed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (project_id, contract_version),
    CHECK (cost_limit_amount > 0)
);

CREATE INDEX nodes_workflow_idx ON workflows.nodes (workflow_id);
CREATE INDEX edges_workflow_idx ON workflows.edges (workflow_id);
