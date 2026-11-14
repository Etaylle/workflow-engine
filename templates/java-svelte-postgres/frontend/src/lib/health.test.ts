import { expect, test } from 'vitest';
import { healthText } from './health';

test('healthText names the app and its state', () => {
	expect(healthText({ status: 'UP', app: 'app' })).toBe('app is up');
	expect(healthText({ status: 'DOWN', app: 'app' })).toBe('app is down');
});
