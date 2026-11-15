import type { WorkspaceFile } from '#lib/api/client.js';

export type TreeNode = {
	name: string;
	path: string;
	dir: boolean;
	size: number;
	children: TreeNode[];
};

/** One visible line of the tree: the node and how deep it sits. */
export type TreeRow = { node: TreeNode; depth: number };

/** Builds the folder tree of a flat file list: folders first, then files, each sorted by name. */
export function buildTree(files: WorkspaceFile[]): TreeNode[] {
	const root: TreeNode = { name: '', path: '', dir: true, size: 0, children: [] };
	for (const file of files) {
		let parent = root;
		const parts = file.path.split('/');
		parts.forEach((name, index) => {
			const path = parts.slice(0, index + 1).join('/');
			const last = index === parts.length - 1;
			let node = parent.children.find((child) => child.path === path);
			if (!node) {
				node = { name, path, dir: !last, size: last ? file.size : 0, children: [] };
				parent.children.push(node);
			}
			parent = node;
		});
	}
	sortTree(root);
	return root.children;
}

function sortTree(node: TreeNode): void {
	node.children.sort((a, b) => Number(b.dir) - Number(a.dir) || a.name.localeCompare(b.name));
	node.children.forEach(sortTree);
}

/** The lines to show: a folder's children only while it is expanded. */
export function visibleRows(nodes: TreeNode[], expanded: readonly string[], depth = 0): TreeRow[] {
	return nodes.flatMap((node) => [
		{ node, depth },
		...(node.dir && expanded.includes(node.path)
			? visibleRows(node.children, expanded, depth + 1)
			: [])
	]);
}

/** The folders to open so that the file shows. */
export function parentsOf(path: string): string[] {
	const parts = path.split('/').slice(0, -1);
	return parts.map((_, index) => parts.slice(0, index + 1).join('/'));
}

/** A size for people: bytes up to 1 KB, then KB with one decimal. */
export const sizeLabel = (bytes: number): string =>
	bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KB`;
