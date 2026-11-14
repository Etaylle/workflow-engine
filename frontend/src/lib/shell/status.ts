import type { MessageKey } from '#lib/i18n/i18n.js';

const KNOWN = [
	'draft',
	'questions_ready',
	'answered',
	'requirements_ready',
	'workflow_drafted',
	'confirmed'
];

export const statusKey = (status: string): MessageKey =>
	KNOWN.includes(status) ? (`status.${status}` as MessageKey) : 'status.other';

/** Colour of the status pill: waiting for the client, finished, or just under way. */
export function statusTone(status: string): 'wait' | 'done' | 'active' | '' {
	if (status === 'questions_ready') return 'wait';
	if (status === 'confirmed') return 'done';
	if (status === 'draft') return '';
	return 'active';
}
