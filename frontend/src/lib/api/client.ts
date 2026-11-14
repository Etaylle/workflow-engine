export type EngineStatus = { name: string; buildId: string; serverTime: string };
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

export async function checkAiConnection(apiKey: string): Promise<AiCheckResult> {
	const response = await fetch('/api/ai/check', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ apiKey })
	});
	if (!response.ok) throw await failure(response);
	return (await response.json()) as AiCheckResult;
}

export type Project = {
	id: string;
	name: string;
	wish: string;
	status: string;
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
export const updateProject = (projectId: string, change: { name?: string; wish?: string }) =>
	json<Project>(`/api/projects/${projectId}`, 'PATCH', change);
export async function deleteProject(projectId: string): Promise<void> {
	const response = await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
	if (!response.ok) throw await failure(response);
}
export const listProjects = () => json<Project[]>('/api/projects');
export const generateWorkflow = (projectId: string) =>
	json<Workflow>(`/api/projects/${projectId}/workflow`, 'POST');
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
