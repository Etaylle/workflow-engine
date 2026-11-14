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
