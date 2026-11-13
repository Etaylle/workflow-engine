#!/usr/bin/env node
// One-off update of the guard's path rules (D-04, 13 November 2026). Run by a human from the repository root:
//   node .claude/skills/project-learnings/apply-guard-paths.mjs && node .claude/hooks/selftest.mjs
// Delete this file after it was applied.
// It changes only COMMON, ROLE_PATHS (guard.mjs) and the document paths of the self-test cases (selftest.mjs).
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const guardFile = path.join(root, '.claude', 'hooks', 'guard.mjs');
const testFile = path.join(root, '.claude', 'hooks', 'selftest.mjs');
if (!fs.existsSync(guardFile)) { console.error('Run this from the workflow-engine root.'); process.exit(1); }

let g = fs.readFileSync(guardFile, 'utf8');
const newRules = `// Every role may write its story checkpoints (AGT-067) under ai-docs/stories/ and record its progress in todo.md.
// Document locations: product documents in ai-docs/, rule documents in .claude/skills/<area>/, agents.md at the root.
const COMMON = ['ai-docs/stories/**', 'todo.md'];
export const ROLE_PATHS = {
  'lead': ['CLAUDE.md', 'README.md', 'agents.md', 'ai-docs/README.md', 'kickoff-prompt.md', 'ai-docs/plan.md', 'ai-docs/goals/**', 'ai-docs/epics/**', '.claude/skills/team/**', '.claude/skills/workflow/**', 'ai-docs/tasks/**', 'ai-docs/plans/**', 'ai-docs/backlog.md'],
  'architect': ['.claude/skills/architecture/**', 'ai-docs/spikes/**', 'spikes/**', 'ai-docs/engine-architecture.md', 'ai-docs/adr/**', 'ai-docs/bpmn/**', 'ai-docs/software-elements.md', 'ai-docs/skeleton/**', '.claude/skills/backend/**', '.claude/skills/frontend/**', '.claude/skills/api/**', '.claude/skills/db/**', 'api/**'],
  'backend-developer': ['backend/**', 'api/**', 'templates/backend-*/**', 'ai-docs/stories/**'],
  'frontend-developer': ['frontend/**', 'templates/frontend-*/**'],
  'tester': ['backend/**/src/test/**', 'frontend/**/*.test.ts', 'frontend/**/*.spec.ts', 'frontend/tests/**', 'frontend/src/**/__tests__/**', 'e2e/**', '.claude/skills/tests/**'],
  'reviewer': [],
  'devops': ['ci/**', 'infrastructure/**', 'runner/**', '.github/**', 'templates/base/**', 'templates/manifest.json', 'templates/README.md', 'compose*.yaml', 'compose*.yml', 'docker-compose*.yml', '**/Dockerfile', '**/.dockerignore', '.gitignore', '.env.example', '.claude/skills/ci-cd/**', '.claude/skills/infrastructure/**'],
  'ux-ui-designer': ['.claude/skills/ux-ui/**', 'ai-docs/design/**', 'ai-docs/ui-mockup/**', 'frontend/src/lib/design/**', 'frontend/src/lib/tokens/**'],
  'auditor': ['ai-docs/audit/**', '.claude/skills/audit/**'],
};`;
const re = /\/\/ Every role may write its story checkpoints[^\n]*\n(?:\/\/[^\n]*\n)?const COMMON = \[[^\]]*\];\nexport const ROLE_PATHS = \{[\s\S]*?\n\};/;
if (!re.test(g)) { console.error('guard.mjs: the ROLE_PATHS block was not found (already updated?). Nothing changed.'); process.exit(1); }
g = g.replace(re, newRules);

let t = fs.readFileSync(testFile, 'utf8');
const moves = [
  ["'ai-docs/tests.md'", "'.claude/skills/tests/tests.md'"],
  ["W('ai-docs/workflow.md', 'x'), 'allow', 'lead writes workflow.md'", "W('.claude/skills/workflow/workflow.md', 'x'), 'allow', 'lead writes workflow.md'"],
  ["W('ai-docs/architecture.md', 'x')", "W('.claude/skills/architecture/architecture.md', 'x')"],
  ["W('ai-docs/workflow.md', 'x'), 'block', 'auditor writes another document'", "W('.claude/skills/workflow/workflow.md', 'x'), 'block', 'auditor writes another document'"],
  ["W('ai-docs/ux-ui.md', 'x')", "W('.claude/skills/ux-ui/ux-ui.md', 'x')"],
];
for (const [a, b] of moves) {
  if (!t.includes(a)) { console.error('selftest.mjs: case not found: ' + a); process.exit(1); }
  t = t.replace(a, b);
}
const extra = `  ['--role=tester', W('todo.md', '- done'), 'allow', 'every role updates todo.md'],
  ['--role=lead', W('ai-docs/epics/E-01.md', 'x'), 'allow', 'lead writes an epic'],
  ['--role=architect', W('ai-docs/spikes/US-00.S1.md', 'x'), 'allow', 'architect writes the spike'],
  ['--role=frontend-developer', W('ai-docs/epics/E-01.md', 'x'), 'block', 'frontend writes an epic'],
];`;
t = t.replace(/\n\];\n\nlet failed/, '\n' + extra + '\n\nlet failed');

fs.writeFileSync(guardFile, g);
fs.writeFileSync(testFile, t);
console.log('guard.mjs and selftest.mjs updated. Now run: node .claude/hooks/selftest.mjs');
