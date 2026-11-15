import { describe, expect, it } from 'vitest';
import { buildTree, parentsOf, sizeLabel, visibleRows } from './files.js';

const files = [
	{ path: 'README.md', size: 10 },
	{ path: 'backend/src/Duck.java', size: 20 },
	{ path: 'backend/build.gradle.kts', size: 30 },
	{ path: 'a.txt', size: 1 }
];

describe('file tree', () => {
	it('puts folders first and sorts by name', () => {
		const tree = buildTree(files);
		expect(tree.map((node) => node.name)).toEqual(['backend', 'a.txt', 'README.md']);
		expect(tree[0]?.children.map((node) => node.name)).toEqual(['src', 'build.gradle.kts']);
	});

	it('shows the children of expanded folders only', () => {
		const tree = buildTree(files);
		expect(visibleRows(tree, []).map((row) => row.node.path)).toEqual([
			'backend',
			'a.txt',
			'README.md'
		]);
		const rows = visibleRows(tree, ['backend', 'backend/src']);
		expect(rows.map((row) => [row.node.path, row.depth])).toContainEqual([
			'backend/src/Duck.java',
			2
		]);
	});

	it('lists the folders to open for a file', () => {
		expect(parentsOf('backend/src/Duck.java')).toEqual(['backend', 'backend/src']);
		expect(parentsOf('README.md')).toEqual([]);
	});

	it('labels sizes', () => {
		expect(sizeLabel(512)).toBe('512 B');
		expect(sizeLabel(2048)).toBe('2.0 KB');
	});
});
