// Models of the domain "execution" as the API returns them (camelCase, ISO-8601 times, money as decimal string).
// Placeholder until the API client is generated from api/openapi.yaml (ADR-007); keep names equal.
import type { StoryType } from '../../shared/models/enums';

export type RunStatus = 'queued' | 'preparing' | 'executing' | 'paused' | 'waiting_for_human' | 'resuming' | 'done' | 'stopped' | 'failed';
export type TaskRunStatus = 'pending' | 'running' | 'fixing' | 'waiting_for_human' | 'done' | 'failed' | 'skipped' | 'manual';
export type StepRunStatus = 'waiting' | 'running' | 'done' | 'not_needed' | 'failed' | 'skipped';
export type CiRunStatus = 'running' | 'passed' | 'failed';
export type ConsoleLevel = 'info' | 'warn' | 'error' | 'success';
export type RunErrorStatus = 'open' | 'fixed' | 'needs_human' | 'skipped';
export type HumanQuestionStatus = 'open' | 'answered' | 'cancelled';
export type AgentCallStatus = 'ok' | 'invalid_output' | 'error';
export type JobStatus = 'pending' | 'processing' | 'done' | 'failed';
export type PushStatus = 'not_pushed' | 'pushed' | 'failed';
export type ErrorClass = 'temporary' | 'broken_environment' | 'flaky_test' | 'real_defect' | 'requirement_conflict';
export type TestLevel = 'unit' | 'component' | 'integration' | 'contract' | 'frontend_integration' | 'end_to_end' | 'smoke';
export type TestResult = 'waiting' | 'running' | 'passed' | 'failed';
export type Verdict = 'pass' | 'fail' | 'not_applicable';
export type ReportKind = 'task_spec' | 'report' | 'blocker';
export type LensVerdict = 'pass' | 'pass_with_findings' | 'fail' | 'not_yet';
export type Severity = 'critical' | 'major' | 'minor' | 'observation';
export type Lens = 'technical' | 'tests' | 'business';
export type FindingStatus = 'open' | 'fixed' | 'accepted';

export interface AnswerOption {
  key: string;
  label: string;
  hint: string;
  recommended: boolean;
}

/** One execution of a confirmed contract (aggregate root). (execution.runs) */
export interface Run {
  id: string;
  projectId: string;
  contractId: string;
  status: RunStatus;
  resumeCount: number;
  costUsedAmount: string;
  sandboxRef?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** One business task in a run (box on the canvas). (execution.task_runs) */
export interface TaskRun {
  id: string;
  runId: string;
  nodeId: string;
  storyKey?: string | null;
  seq: number;
  storyType: StoryType;
  status: TaskRunStatus;
  fixLoops: number;
  modelRaised: boolean;
  summary?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** One development workflow step of one task. (execution.step_runs) */
export interface StepRun {
  id: string;
  taskRunId: string;
  seq: number;
  stepKey: string;
  stepName: string;
  memberRoleKeys: string[];
  status: StepRunStatus;
  result?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** One continuous integration run (ci/run all-local). (execution.ci_runs) */
export interface CiRun {
  id: string;
  taskRunId: string;
  number: number;
  status: CiRunStatus;
  stages: Record<string, string>;
  startedAt: string;
  endedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** The console of the AI work (insert-only). (execution.console_lines) */
export interface ConsoleLine {
  id: string;
  runId: string;
  taskRunId?: string | null;
  source: string;
  level: ConsoleLevel;
  message: string;
  createdAt: string;
}

/** An error, its type, the automatic reaction and how it ended. (execution.run_errors) */
export interface RunError {
  id: string;
  taskRunId: string;
  stepRunId?: string | null;
  ciRunId?: string | null;
  errorClass: ErrorClass;
  message: string;
  detail?: string | null;
  status: RunErrorStatus;
  attempts: number;
  autoAction?: string | null;
  fixedInLoop?: number | null;
  createdAt: string;
  updatedAt: string;
}

/** Human action required: question with short answer options. (execution.human_questions) */
export interface HumanQuestion {
  id: string;
  taskRunId: string;
  runErrorId?: string | null;
  question: string;
  expected: string;
  actual: string;
  requirementRefs: string[];
  attemptsSummary: string;
  cause: string;
  options: AnswerOption[];
  status: HumanQuestionStatus;
  chosenOption?: string | null;
  answerText?: string | null;
  answeredAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** A decision the AI took on its own (also DECISIONS.md). (execution.decisions) */
export interface Decision {
  id: string;
  runId: string;
  taskRunId?: string | null;
  madeBy: string;
  title: string;
  reason: string;
  createdAt: string;
}

/** A git commit after a green task, and its push to GitHub (step "Merge and push"). (execution.checkpoints) */
export interface Checkpoint {
  id: string;
  runId: string;
  taskRunId?: string | null;
  seq: number;
  commitSha: string;
  pushStatus: PushStatus;
  pushedAt?: string | null;
  commitUrl?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Evidence: which test proves which criterion. (execution.criterion_tests) */
export interface CriterionTest {
  id: string;
  taskRunId: string;
  criterionText: string;
  testName: string;
  testLevel: TestLevel;
  result: TestResult;
  createdAt: string;
  updatedAt: string;
}

/** Evidence: verdict of each member in step Team verification. (execution.verdicts) */
export interface MemberVerdict {
  id: string;
  taskRunId: string;
  roleKey: string;
  verdict: Verdict;
  reason?: string | null;
  createdAt: string;
}

/** Task spec, report or blocker report of a member (insert-only). (execution.member_reports) */
export interface MemberReport {
  id: string;
  stepRunId: string;
  roleKey: string;
  kind: ReportKind;
  content: string;
  createdAt: string;
}

/** Audit report of a run: three lenses, risks, what was not verified. (execution.audit_reports) */
export interface AuditReport {
  id: string;
  runId: string;
  isFinal: boolean;
  technical: LensVerdict;
  tests: LensVerdict;
  business: LensVerdict;
  acceptedRisks: string[];
  notVerified: string;
  createdAt: string;
  updatedAt: string;
}

/** One audit finding. (execution.findings) */
export interface Finding {
  id: string;
  auditReportId: string;
  severity: Severity;
  lens: Lens;
  text: string;
  requirementKey?: string | null;
  ownerRoleKey?: string | null;
  status: FindingStatus;
  createdAt: string;
  updatedAt: string;
}

/** Metrics of a run (workflow.md 7.9). (execution.run_metrics) */
export interface RunMetrics {
  runId: string;
  loopsPerTask: string;
  firstTryTasks: number;
  finishedTasks: number;
  avgTaskMinutes: number;
  waitingMinutes: number;
  createdAt: string;
  updatedAt: string;
}

/** One AI call (no prompts, no keys). (execution.agent_calls) */
export interface AgentCall {
  id: string;
  projectId: string;
  runId?: string | null;
  agent: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  durationMs: number;
  status: AgentCallStatus;
  createdAt: string;
}

/** Durable job table of the orchestrator (ADR-018). (execution.jobs) */
export interface Job {
  id: string;
  kind: string;
  payload: string;
  status: JobStatus;
  attempts: number;
  availableAt: string;
  leaseUntil?: string | null;
  createdAt: string;
  updatedAt: string;
}
