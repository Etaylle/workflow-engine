// Models of the domain "workflows" as the API returns them (camelCase, ISO-8601 times, money as decimal string).
// Placeholder until the API client is generated from api/openapi.yaml (ADR-007); keep names equal.
import type { StoryType } from '../../shared/models/enums';

export type WorkflowStatus = 'draft' | 'valid' | 'invalid' | 'confirmed';
export type NodeKind = 'start_event' | 'task' | 'end_event';
export type TaskType = 'software' | 'manual';

/** The business workflow of a project (aggregate root). (workflows.workflows) */
export interface Workflow {
  id: string;
  projectId: string;
  status: WorkflowStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** A BPMN lane = an actor (Student, Kitchen). (workflows.lanes) */
export interface Lane {
  id: string;
  workflowId: string;
  name: string;
  seq: number;
  createdAt: string;
  updatedAt: string;
}

/** A start event, task or end event on the canvas; version guards edits from two tabs. (workflows.nodes) */
export interface Node {
  id: string;
  workflowId: string;
  laneId: string;
  kind: NodeKind;
  title: string;
  description?: string | null;
  taskType?: TaskType | null;
  epicKey?: string | null;
  storyKey?: string | null;
  storyType?: StoryType | null;
  posX: number;
  posY: number;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** A sequence flow arrow. (workflows.edges) */
export interface Edge {
  id: string;
  workflowId: string;
  sourceNodeId: string;
  targetNodeId: string;
  createdAt: string;
}

/** "Done when" of a task. (workflows.acceptance_criteria) */
export interface AcceptanceCriterion {
  id: string;
  nodeId: string;
  seq: number;
  text: string;
  createdAt: string;
  updatedAt: string;
}

/** A test scenario (Given-When-Then) written by the AI from a criterion. (workflows.scenarios) */
export interface Scenario {
  id: string;
  nodeId: string;
  criterionId?: string | null;
  seq: number;
  gherkin: string;
  createdAt: string;
  updatedAt: string;
}

/** Which requirements a task implements (key only: no cross-schema join). (workflows.node_requirements) */
export interface NodeRequirement {
  nodeId: string;
  requirementKey: string;
  createdAt: string;
}

/** Frozen copy of wish, answers, stack, AI, requirements, workflow, team and development workflow (insert-only). (workflows.contracts) */
export interface Contract {
  id: string;
  projectId: string;
  contractVersion: number;
  snapshot: string;
  costLimitAmount: string;
  costLimitCurrency: string;
  confirmedAt: string;
  createdAt: string;
}
