// Models of the domain "settings" as the API returns them (camelCase, ISO-8601 times, money as decimal string).
// Placeholder until the API client is generated from api/openapi.yaml (ADR-007); keep names equal.
import type { Provider } from '../../shared/models/enums';

export type ModelTier = 'best' | 'standard';
export type RunsWhen = 'always' | 'if_screen' | 'if_interface_changes' | 'if_data_changes' | 'if_needed' | 'after_every_change' | 'on_failure';
export type FixedPosition = 'first' | 'last' | 'none';
export type MemberPart = 'does' | 'checks';
export type TeamPreset = 'starter' | 'standard' | 'full' | 'custom';
export type LessonStatus = 'proposed' | 'applied' | 'dismissed';

/** An editable team plus development workflow; the default comes from team.md and workflow.md. (settings.team_profiles) */
export interface TeamProfile {
  id: string;
  name: string;
  isDefault: boolean;
  source: string;
  preset: TeamPreset;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** One member of the AI team (editable). (settings.team_members) */
export interface TeamMember {
  id: string;
  profileId: string;
  roleKey: string;
  name: string;
  mission: string;
  instructions: string;
  modelTier: ModelTier;
  active: boolean;
  canDisable: boolean;
  writePaths: string[];
  seq: number;
  createdAt: string;
  updatedAt: string;
}

/** One step every task runs (editable, ordered by seq). (settings.dev_workflow_steps) */
export interface DevWorkflowStep {
  id: string;
  profileId: string;
  seq: number;
  key: string;
  name: string;
  runsWhen: RunsWhen;
  storyTypes: string[];
  doneWhen?: string | null;
  timeLimitMinutes?: number | null;
  fixedPosition: FixedPosition;
  createdAt: string;
  updatedAt: string;
}

/** Which members do or check a step. (settings.dev_workflow_step_members) */
export interface DevWorkflowStepMember {
  stepId: string;
  memberId: string;
  part: MemberPart;
  createdAt: string;
}

/** Rules for every task: fix loops before asking a human, CI stages, time. (settings.workflow_rules) */
export interface WorkflowRules {
  profileId: string;
  maxFixLoops: number;
  ciStages: string[];
  timeTargetMinutes: number;
  ceilingMinutes: number;
  raiseModelBeforeAsking: boolean;
  temporaryRetries: number;
  rebuildEnvironmentOnce: boolean;
  setAsideFlakyTests: boolean;
  createdAt: string;
  updatedAt: string;
}

/** A repeated cause across runs and the automatic check proposed for it (workflow.md 7.5). (settings.lessons) */
export interface Lesson {
  id: string;
  profileId: string;
  cause: string;
  occurrences: number;
  runCount: number;
  proposal: string;
  targetStepKey?: string | null;
  status: LessonStatus;
  createdAt: string;
  updatedAt: string;
}

/** Which model is "best" and "standard" per provider. (settings.provider_models) */
export interface ProviderModel {
  id: string;
  provider: Provider;
  tier: ModelTier;
  modelId: string;
  createdAt: string;
  updatedAt: string;
}
