/**
 * Client for how the builder of a run is authorised. The backend contract is planned, not merged:
 * GET /api/execution/prerequisites, PUT /api/execution/claude-auth. Every name lives in this one
 * file; a 404 means the engine has no such endpoint and the screens keep today's behaviour.
 */
import { ApiError, type Problem } from './client.js';

export type BuilderId = 'claude_code' | 'gemini_cli';
export type ClaudeAuthMode = 'anthropic_key' | 'machine_sign_in';

/** One builder: ready, or the list of what is missing (codes) and a detail line. */
export type BuilderReadiness = {
	id: BuilderId;
	ready: boolean;
	missing: string[];
	details: string | null;
};

export type Prerequisites = {
	builders: BuilderReadiness[];
	claudeAuth: ClaudeAuthMode;
};

type RawBuilder = Partial<Omit<BuilderReadiness, 'missing'>> & { missing?: string[] };
type RawPrerequisites = { builders?: RawBuilder[]; claudeAuth?: ClaudeAuthMode };

/** Reads the answer tolerantly: unknown builders are dropped, absent lists are empty. */
export function normalisePrerequisites(raw: RawPrerequisites): Prerequisites {
	const builders: BuilderReadiness[] = [];
	for (const entry of raw.builders ?? []) {
		if (entry.id !== 'claude_code' && entry.id !== 'gemini_cli') continue;
		builders.push({
			id: entry.id,
			ready: entry.ready === true,
			missing: entry.missing ?? [],
			details: entry.details ?? null
		});
	}
	return { builders, claudeAuth: raw.claudeAuth ?? 'anthropic_key' };
}

/** The prerequisites of the builders, or null when the engine has no such endpoint (or fails). */
export async function getPrerequisites(): Promise<Prerequisites | null> {
	try {
		const response = await fetch('/api/execution/prerequisites');
		if (!response.ok) return null;
		return normalisePrerequisites((await response.json()) as RawPrerequisites);
	} catch {
		return null;
	}
}

export async function setClaudeAuth(mode: ClaudeAuthMode): Promise<void> {
	const response = await fetch('/api/execution/claude-auth', {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ mode })
	});
	if (!response.ok) {
		let problem: Problem;
		try {
			problem = (await response.json()) as Problem;
		} catch {
			problem = { title: response.statusText, status: response.status };
		}
		throw new ApiError(problem);
	}
}
