/** Client for the builders of a run: what each one still lacks and how Claude Code signs in. */
import { ApiError, type Problem } from './client.js';

export type BuilderId = 'claude_code' | 'gemini_cli';
export type ClaudeAuthMode = 'anthropic_key' | 'machine_sign_in';

export type MissingItem = { code: string; message: string; instructionKey: string | null };

export type Builder = {
	id: BuilderId;
	ready: boolean;
	installed: boolean;
	version: string;
	signedIn: string | null;
	keySet: boolean;
	authMode: ClaudeAuthMode | null;
	model: string;
	missing: MissingItem[];
};

export type Builders = { demo: boolean; builders: Builder[] };

type RawBuilder = Partial<Omit<Builder, 'missing'>> & {
	missing?: (Partial<MissingItem> & { code: string })[];
};

/** Reads the answer tolerantly: unknown builders are dropped, absent lists are empty. */
export function normaliseBuilders(raw: { demo?: boolean; builders?: RawBuilder[] }): Builders {
	const builders: Builder[] = [];
	for (const entry of raw.builders ?? []) {
		if (entry.id !== 'claude_code' && entry.id !== 'gemini_cli') continue;
		builders.push({
			id: entry.id,
			ready: entry.ready === true,
			installed: entry.installed === true,
			version: entry.version ?? '',
			signedIn: entry.signedIn ?? null,
			keySet: entry.keySet === true,
			authMode: entry.authMode ?? null,
			model: entry.model ?? '',
			missing: (entry.missing ?? []).map((item) => ({
				code: item.code,
				message: item.message ?? item.code,
				instructionKey: item.instructionKey ?? null
			}))
		});
	}
	return { demo: raw.demo === true, builders };
}

async function problemOf(response: Response): Promise<ApiError> {
	let problem: Problem;
	try {
		problem = (await response.json()) as Problem;
	} catch {
		problem = { title: response.statusText, status: response.status };
	}
	return new ApiError(problem);
}

/** What each builder lacks; `refresh` makes the engine look again (installation, sign-in). */
export async function getBuilders(refresh = false): Promise<Builders | null> {
	try {
		const response = await fetch(refresh ? '/api/builders?refresh=true' : '/api/builders');
		if (!response.ok) return null;
		return normaliseBuilders((await response.json()) as Parameters<typeof normaliseBuilders>[0]);
	} catch {
		return null;
	}
}

export async function setClaudeAuth(mode: ClaudeAuthMode): Promise<void> {
	const response = await fetch('/api/builders/claude_code/auth', {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ mode })
	});
	if (!response.ok) throw await problemOf(response);
}
