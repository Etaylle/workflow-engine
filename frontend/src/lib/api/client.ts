export type EngineStatus = {
	name: string;
	buildId: string;
	serverTime: string;
	aiStub: boolean;
};
export type AiProviderId = 'anthropic' | 'openai';
export type KeyState = { set: boolean; last4: string | null };
export type CompatibleKeyState = KeyState & { baseUrl: string | null; model: string | null };
export type AiKeys = {
	anthropic: KeyState;
	openai: CompatibleKeyState;
	planning: { provider: AiProviderId; model: string } | null;
	defaults: { baseUrl: string; model: string };
};
export type AiCheckResult = { ok: boolean; model: string };
export type Problem = { title: string; status: number; detail?: string };

export class ApiError extends Error {
	constructor(readonly problem: Problem) {
		super(problem.detail ?? problem.title);
	}
}

async function failure(response: Response): Promise<ApiError> {
	try {
		return new ApiError((await response.json()) as Problem);
	} catch {
		return new ApiError({ title: response.statusText, status: response.status });
	}
}

export async function getStatus(): Promise<EngineStatus> {
	const response = await fetch('/api/status');
	if (!response.ok) throw await failure(response);
	return (await response.json()) as EngineStatus;
}

export type CheckRequest = {
	provider: AiProviderId;
	apiKey?: string;
	baseUrl?: string;
	model?: string;
};

export async function checkAiConnection(request: CheckRequest): Promise<AiCheckResult> {
	const response = await fetch('/api/ai/check', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(request)
	});
	if (!response.ok) throw await failure(response);
	return (await response.json()) as AiCheckResult;
}

export async function getAiKeys(): Promise<AiKeys> {
	const response = await fetch('/api/ai/keys');
	if (!response.ok) throw await failure(response);
	return (await response.json()) as AiKeys;
}

export async function setAiKey(
	provider: AiProviderId,
	body: { apiKey: string; baseUrl?: string; model?: string }
): Promise<AiKeys> {
	const response = await fetch(`/api/ai/keys/${provider}`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(body)
	});
	if (!response.ok) throw await failure(response);
	return (await response.json()) as AiKeys;
}

export async function deleteAiKey(provider: AiProviderId): Promise<void> {
	const response = await fetch(`/api/ai/keys/${provider}`, { method: 'DELETE' });
	if (!response.ok) throw await failure(response);
}

export async function choosePlanningProvider(provider: AiProviderId): Promise<AiKeys> {
	const response = await fetch('/api/ai/planning', {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ provider })
	});
	if (!response.ok) throw await failure(response);
	return (await response.json()) as AiKeys;
}

export type Project = {
	id: string;
	name: string;
	wish: string;
	status: string;
	agentHarness: string;
	model: string;
	createdAt: string;
	updatedAt: string;
};

export type WorkflowNode = {
	id: string;
	laneId: string;
	kind: 'start_event' | 'task' | 'end_event';
	title: string;
	description?: string | null;
	taskType?: 'software' | 'manual' | null;
	posX: number;
	posY: number;
};

export type Workflow = {
	id: string;
	projectId: string;
	status: 'valid' | 'confirmed';
	version: number;
	confirmedContractVersion?: number | null;
	lanes: { id: string; name: string }[];
	nodes: WorkflowNode[];
	edges: { id: string; source: string; target: string }[];
};

export type Contract = { id: string; projectId: string; version: number; confirmedAt: string };

async function json<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
	const init: RequestInit = { method };
	if (body !== undefined) {
		init.headers = { 'Content-Type': 'application/json' };
		init.body = JSON.stringify(body);
	}
	const response = await fetch(path, init);
	if (!response.ok) throw await failure(response);
	return (await response.json()) as T;
}

export const createProject = (wish: string, apiKey: string, name: string) =>
	json<Project>('/api/projects', 'POST', { wish, apiKey, name });
export const updateProject = (
	projectId: string,
	change: { name?: string; wish?: string; agentHarness?: string }
) => json<Project>(`/api/projects/${projectId}`, 'PATCH', change);
export async function deleteProject(projectId: string): Promise<void> {
	const response = await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
	if (!response.ok) throw await failure(response);
}
export const listProjects = () => json<Project[]>('/api/projects');
export const generateWorkflow = (projectId: string) =>
	json<Workflow>(`/api/projects/${projectId}/workflow`, 'POST');
export const editWorkflow = (projectId: string, body: unknown) =>
	json<Workflow>(`/api/projects/${projectId}/workflow`, 'PUT', body);
export const confirmWorkflow = (projectId: string) =>
	json<Contract>(`/api/projects/${projectId}/workflow/confirm`, 'POST');
export const listContracts = (projectId: string) =>
	json<Contract[]>(`/api/projects/${projectId}/contracts`);

/** The workflow of a project, or null when it has none yet. */
export async function getWorkflow(projectId: string): Promise<Workflow | null> {
	const response = await fetch(`/api/projects/${projectId}/workflow`);
	if (response.status === 404) return null;
	if (!response.ok) throw await failure(response);
	return (await response.json()) as Workflow;
}

export type Brief = { goal: string; actors: string[]; domainTerms: string[]; risks: string[] };
export type AnswerValue = string | string[];
export type Question = {
	id: string;
	seq: number;
	category: 'requirement' | 'feature';
	answerType: 'single_choice' | 'multiple_choice' | 'text';
	text: string;
	options: string[];
	proposal: AnswerValue;
	explanation?: string | null;
	answer?: { value: AnswerValue; decidedBy: 'client' | 'ai' } | null;
};
export type QuestionInput = {
	category: 'requirement' | 'feature';
	answerType: 'single_choice' | 'multiple_choice' | 'text';
	text: string;
	options: string[];
	proposal: string[];
};
export type AnswerInput = { questionId: string; acceptProposal?: boolean; value?: AnswerValue };
export type Requirement = {
	key: string;
	type: 'functional' | 'quality' | 'constraint';
	priority: 'must' | 'should' | 'could' | 'wont';
	text: string;
	source?: string | null;
};
export type Epic = { key: string; name: string; domainName: string; requirements: Requirement[] };
export type Requirements = { epics: Epic[] };

export const generateBrief = (projectId: string) =>
	json<Brief>(`/api/projects/${projectId}/brief`, 'POST');
export const getBrief = async (projectId: string): Promise<Brief | null> => {
	const response = await fetch(`/api/projects/${projectId}/brief`);
	if (response.status === 404) return null;
	if (!response.ok) throw await failure(response);
	return (await response.json()) as Brief;
};
export const generateQuestions = (projectId: string) =>
	json<Question[]>(`/api/projects/${projectId}/questions`, 'POST');
export const getQuestions = (projectId: string) =>
	json<Question[]>(`/api/projects/${projectId}/questions`);
export const saveAnswers = (projectId: string, answers: AnswerInput[]) =>
	json<Question[]>(`/api/projects/${projectId}/answers`, 'PUT', { answers });
export const generateRequirements = (projectId: string) =>
	json<Requirements>(`/api/projects/${projectId}/requirements`, 'POST');
export const getRequirements = (projectId: string) =>
	json<Requirements>(`/api/projects/${projectId}/requirements`);
export const addQuestion = (projectId: string, input: QuestionInput) =>
	json<Question[]>(`/api/projects/${projectId}/question-items`, 'POST', input);
export const editQuestion = (projectId: string, questionId: string, input: QuestionInput) =>
	json<Question[]>(`/api/projects/${projectId}/question-items/${questionId}`, 'PUT', input);
export const deleteQuestion = (projectId: string, questionId: string) =>
	json<Question[]>(`/api/projects/${projectId}/question-items/${questionId}`, 'DELETE');

export type DocumentKind = 'requirements' | 'plan';
export type ProjectDocument = {
	title: string;
	sections: { heading: string; paragraphs: string[]; bullets: string[] }[];
};
export const getDocument = (projectId: string, kind: DocumentKind) =>
	json<ProjectDocument>(`/api/projects/${projectId}/documents/${kind}`);
export const documentUrl = (projectId: string, kind: DocumentKind, format: 'docx' | 'md') =>
	`/api/projects/${projectId}/documents/${kind}/download?format=${format}`;

export type RunStatus = 'preparing' | 'executing' | 'waiting' | 'done' | 'interrupted' | 'failed';
export type RunWaitKind = 'question' | 'cost_limit' | 'tokens';
export type RunQuestion = {
	id: string;
	text: string;
	stepTitle: string;
	options: ('retry' | 'skip' | 'stop')[];
};
export type RunStep = {
	id: string;
	seq: number;
	key: string;
	title: string;
	status: 'waiting' | 'running' | 'done' | 'failed';
	result?: string | null;
	costUsd: number;
	turns: number;
	seconds: number;
	attempts: number;
	commit?: string | null;
};
export type RunTask = {
	id: string;
	seq: number;
	title: string;
	lane?: string | null;
	status: 'pending' | 'running' | 'done' | 'failed';
	steps: RunStep[];
};
export type Run = {
	id: string;
	projectId: string;
	contractVersion: number;
	status: RunStatus;
	failureReason?: string | null;
	waitKind?: RunWaitKind | null;
	resumeCount: number;
	question?: RunQuestion | null;
	costUsd: number;
	costLimitUsd: number;
	lastSeq: number;
	startedAt: string;
	finishedAt?: string | null;
	tasks: RunTask[];
};
export type RunEvent = {
	seq: number;
	kind: 'run' | 'task' | 'step' | 'console';
	text: string;
	at: string;
};

export const startRun = (projectId: string) => json<Run>(`/api/projects/${projectId}/runs`, 'POST');
export const answerRunQuestion = (
	runId: string,
	questionId: string,
	choice: 'retry' | 'skip' | 'stop',
	hint: string
) => json<Run>(`/api/runs/${runId}/answer`, 'POST', { questionId, choice, hint });
export const stopRun = (runId: string) => json<Run>(`/api/runs/${runId}/stop`, 'POST');
export const resumeRun = (runId: string, costLimitUsd?: number) =>
	json<Run>(
		`/api/runs/${runId}/resume`,
		'POST',
		costLimitUsd === undefined ? {} : { costLimitUsd }
	);
export const getRun = (runId: string) => json<Run>(`/api/runs/${runId}`);
export const listRunEvents = (runId: string, after = 0) =>
	json<RunEvent[]>(`/api/runs/${runId}/events?after=${after}`);
export const runStreamUrl = (runId: string, after: number) =>
	`/api/runs/${runId}/stream?after=${after}`;
export type RunReport = {
	runId: string;
	status: RunStatus;
	startedAt: string;
	finishedAt?: string | null;
	seconds: number;
	costUsd: number;
	costLimitUsd: number;
	resumeCount: number;
	tasksDone: number;
	tasksTotal: number;
	stepsDone: number;
	stepsTotal: number;
	attempts: number;
	checkpoints: number;
	tasks: {
		title: string;
		status: string;
		steps: {
			title: string;
			status: string;
			attempts: number;
			costUsd: number;
			seconds: number;
			commit?: string | null;
		}[];
	}[];
	questions: {
		stepTitle: string;
		question: string;
		choice?: 'retry' | 'skip' | 'stop' | null;
		hint?: string | null;
		askedAt: string;
	}[];
};
export const getRunReport = (runId: string) => json<RunReport>(`/api/runs/${runId}/report`);
export const runZipUrl = (runId: string) => `/api/runs/${runId}/zip`;

/** The newest run of a project, or null when it has none yet. */
export async function getLatestRun(projectId: string): Promise<Run | null> {
	const response = await fetch(`/api/projects/${projectId}/runs/latest`);
	if (response.status === 404) return null;
	if (!response.ok) throw await failure(response);
	return (await response.json()) as Run;
}

export type StepDef = {
	key: string;
	title: string;
	gated: boolean;
	capUsd: number;
	prompt: string;
};
export type StepProfile = {
	id: string;
	name: string;
	description: string;
	isDefault: boolean;
	seeded: boolean;
	steps: StepDef[];
};
export type ProjectSettings = {
	stepProfileId: string;
	stepProfileName: string;
	usesDefaultProfile: boolean;
	costLimitUsd: number;
	defaultCostLimitUsd: number;
	usesDefaultLimit: boolean;
};

export const listStepProfiles = () => json<StepProfile[]>('/api/settings/step-profiles');
export const copyStepProfile = (fromId: string, name: string) =>
	json<StepProfile>('/api/settings/step-profiles', 'POST', { fromId, name });
export const updateStepProfile = (id: string, name: string, steps: StepDef[]) =>
	json<StepProfile>(`/api/settings/step-profiles/${id}`, 'PUT', { name, steps });
export async function deleteStepProfile(id: string): Promise<void> {
	const response = await fetch(`/api/settings/step-profiles/${id}`, { method: 'DELETE' });
	if (!response.ok) throw await failure(response);
}
export const getProjectSettings = (projectId: string) =>
	json<ProjectSettings>(`/api/projects/${projectId}/settings`);
export const updateProjectSettings = (
	projectId: string,
	change: { stepProfileId: string | null; costLimitUsd: number | null }
) => json<ProjectSettings>(`/api/projects/${projectId}/settings`, 'PUT', change);
