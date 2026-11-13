#!/usr/bin/env node
// Checks that file references in the project documents resolve to real files.
// Usage: node .claude/skills/docs-check/check-refs.mjs [--all]
//   default: reports broken references (exit 1 if any)
//   --all:   also lists references to future code folders that do not exist yet
//
// A reference is a Markdown link target `](path)` or a backticked path `` `path` ``
// that ends in a known file extension or a slash. Placeholders (<x>, *, …, $X, NN) are skipped.
// Resolution order: relative to the file, to the repository root, to ai-docs/,
// then, for a bare file name (no slash), any document with that name (doc index).

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
const showAll = process.argv.includes('--all');

// Folders of the product code. They are created story by story, so a reference to them is not an error yet.
const FUTURE = /^(?:backend|frontend|api|ci|infrastructure|runner|templates|e2e|ai-docs\/(?:tasks|plans|spikes|design|adr|stories)|\.github|src|docs)\//;
const SKIP_DIRS = new Set(['.git', 'node_modules', '.idea', 'build', 'dist', '.gradle', '.svelte-kit', 'skeleton']);
const EXT = /[\w-]\.(?:md|png|bpmn|docx|html|json|sql|java|ts|mjs|ya?ml|kts|svelte)$/;
// Only paths under these roots are documentation paths; other paths with a slash are examples or product code.
const DOC_ROOT = /^(?:\.\.\/|ai-docs\/|\.claude\/|ui-mockup\/|bpmn\/|audit\/|images\/|word\/|stories\/|skeleton\/|CLAUDE\.md|agents\.md)/;
// Documents that are planned but not written yet (architecture.md document plan, dormant phase, runbooks of
// infrastructure.md, files of a generated project) and names that only look like files (design tokens).
const PLANNED = new Set(['security.md', 'observability.md', 'DECISIONS.md', 'process.md', 'first-deploy.md', 'recover-host.md',
  'restore-database.md', 'rollback.md', 'size.control.md', 'backlog.md']);

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (SKIP_DIRS.has(e.name)) continue;
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out); else out.push(p);
  }
  return out;
}

const all = walk(root);
const docs = all.filter((f) => f.endsWith('.md'));
const byName = new Map();
for (const f of all) { const b = path.basename(f); if (!byName.has(b)) byName.set(b, []); byName.get(b).push(f); }

function candidates(line) {
  const out = [];
  for (const m of line.matchAll(/\]\(([^)\s#]+)(?:#[^)]*)?\)/g)) out.push(m[1]);
  for (const m of line.matchAll(/`([^`\s]+)`/g)) out.push(m[1]);
  return out.filter((r) => !/^(?:https?:|mailto:)/.test(r) && !/[<>*…${}|]|NN|\.\.\.|^-/.test(r) && (EXT.test(r) || (r.endsWith('/') && r.includes('/'))));
}

function exists(p) { try { fs.statSync(p); return true; } catch { return false; } }

let broken = 0; let future = 0;
for (const f of docs) {
  const relFile = path.relative(root, f);
  const lines = fs.readFileSync(f, 'utf8').split('\n');
  let inFence = false;
  lines.forEach((line, i) => {
    if (/^\s*```/.test(line)) { inFence = !inFence; return; }
    if (inFence) return;
    for (const ref of candidates(line)) {
      const clean = ref.replace(/^\.\//, '');
      const tries = [path.join(path.dirname(f), clean), path.join(root, clean), path.join(root, 'ai-docs', clean)];
      if (tries.some(exists)) continue;
      if (clean.startsWith('/') || /ee\.ss|<|\bnn\b/i.test(clean)) continue;
      if (!clean.includes('/')) {
        if (byName.has(clean) || PLANNED.has(clean)) continue;
        if (!clean.endsWith('.md')) { future++; if (showAll) console.log(`future  ${relFile}:${i + 1}  ${ref}`); continue; }
      } else if (!DOC_ROOT.test(clean)) { future++; if (showAll) console.log(`example ${relFile}:${i + 1}  ${ref}`); continue; }
      const fromRoot = path.relative(root, path.join(path.dirname(f), clean)).split(path.sep).join('/');
      if (FUTURE.test(clean) || FUTURE.test(fromRoot)) { future++; if (showAll) console.log(`future  ${relFile}:${i + 1}  ${ref}`); continue; }
      broken++;
      console.log(`BROKEN  ${relFile}:${i + 1}  ${ref}`);
    }
  });
}
console.log(`\n${docs.length} documents checked; ${broken} broken reference(s); ${future} reference(s) to product code or examples not checked${showAll ? '' : ' (--all lists them)'}.`);
process.exit(broken ? 1 : 0);
