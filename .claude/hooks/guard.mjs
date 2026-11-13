#!/usr/bin/env node
// Guardrail hook for Claude Code (agents.md AGT-081: a guardrail that matters is a hook, not a prompt).
//
// Usage (PreToolUse hook, reads the hook JSON from stdin):
//   node .claude/hooks/guard.mjs --mode=global        all sessions: secrets, disabled tests,
//                                                     destructive commands, protected guard files
//   node .claude/hooks/guard.mjs --role=<role>        one subagent: path guard of agents.md section 11
//
// A blocked call prints a PreToolUse "deny" decision as JSON on stdout and exits 0
// (this is more reliable than exit code 2 for some tools). Anything else: exit 0, no output.
// Blocked calls are appended to .claude/hooks/blocked.log (git-ignored).
// Self-test: node .claude/hooks/selftest.mjs

import fs from 'node:fs';
import path from 'node:path';

const args = Object.fromEntries(process.argv.slice(2).map((a) => {
  const [k, v] = a.replace(/^--/, '').split('=');
  return [k, v ?? true];
}));

// ---------- path rules per role (agents.md section 11, adapted to this repository) ----------
// Every role may write its story checkpoints (AGT-067) under ai-docs/stories/ and record its progress in todo.md.
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
};

// ---------- global rules ----------
const SECRET_PATTERNS = [
  [/sk-ant-[A-Za-z0-9_-]{20,}/, 'Anthropic API key'],
  [/\bsk-(?:proj-)?[A-Za-z0-9_-]{32,}/, 'OpenAI-style API key'],
  [/\bgh[pousr]_[A-Za-z0-9]{36,}/, 'GitHub token'],
  [/\bgithub_pat_[A-Za-z0-9_]{40,}/, 'GitHub fine-grained token'],
  [/\bAKIA[0-9A-Z]{16}\b/, 'AWS access key'],
  [/\bAIza[0-9A-Za-z_-]{35}\b/, 'Google API key'],
  [/-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----/, 'private key'],
];

// Patterns that disable or weaken tests (AGT-081, CLAUDE.md section 3). Only an INCREASE is blocked,
// so existing occurrences (for example in documentation) do not block unrelated edits.
const TEST_DISABLE_PATTERNS = [
  [/@Disabled\b/, 'JUnit @Disabled'],
  [/@Ignore\b/, 'JUnit 4 @Ignore'],
  [/\b(?:it|test|describe)\.(?:skip|only|todo)\s*\(/, 'Vitest/Jest/Playwright skip or only'],
  [/\b(?:xit|xdescribe|xtest)\s*\(/, 'x-prefixed disabled test'],
  [/\btest\.fixme\s*\(/, 'Playwright fixme'],
  [/@pytest\.mark\.(?:skip|xfail)\b/, 'pytest skip/xfail'],
  [/\[(?:Fact|Theory)\s*\(\s*Skip\s*=/, 'xUnit Skip'],
  [/^\s*@(?:ignore|wip|skip)\b/m, 'Gherkin @ignore/@wip/@skip tag'],
  [/ignoreFailures\s*=\s*true/, 'Gradle ignoreFailures'],
  [/testFailureIgnore/, 'Maven testFailureIgnore'],
  [/continue-on-error:\s*true/, 'CI continue-on-error'],
  [/failOnViolation\s*=\s*false|failOnError\s*=\s*false/, 'check failure switched off'],
];

const DESTRUCTIVE_BASH = [
  [/\brm\s+-[a-zA-Z]*r[a-zA-Z]*f?[a-zA-Z]*\s+(?:\/|~|\$HOME|\.\.?)(?:\s|$|\/\s|\/\*)/, 'recursive delete of a root, home or project folder'],
  [/\bgit\s+push\b[^\n;&|]*(?:--force\b|--force-with-lease\b|\s-f\b)/, 'force push'],
  [/\bgit\s+push\b[^\n;&|]*\b(?:origin\s+)?(?:HEAD:)?(?:main|master)\b/, 'push to main (CLAUDE.md section 3: never push to main)'],
  [/\bgit\s+reset\s+--hard\b/, 'git reset --hard'],
  [/\bgit\s+clean\s+-[a-zA-Z]*f/, 'git clean -f'],
  [/\bgit\s+(?:commit|push|merge)\b[^\n]*--no-verify\b/, '--no-verify skips the checks'],
  [/\bgit\s+branch\s+-D\b/, 'force branch delete'],
  [/\bdocker\s+(?:volume\s+(?:rm|prune)|system\s+prune|compose\b[^\n]*\bdown\b[^\n]*(?:-v\b|--volumes\b))/, 'Docker volume removal (use ci/run env-down for the local environment)'],
  [/\b(?:DROP\s+(?:DATABASE|SCHEMA)|TRUNCATE\s+TABLE)\b/i, 'database drop or truncate'],
  [/\b(?:curl|wget)\b[^\n|]*\|\s*(?:sudo\s+)?(?:ba|z)?sh\b/, 'piping a download into a shell'],
  [/\bsudo\b/, 'sudo'],
  [/-DskipTests\b|-Dmaven\.test\.skip|--skip-tests\b/, 'skipping tests'],
  [/\bchmod\s+-R\s+777\b/, 'chmod -R 777'],
];

const SECRET_FILE = /(?:^|\/)(?:\.env(?:\.(?!example$)[^/]+)?|[^/]+\.pem|[^/]+\.key|id_(?:rsa|ed25519|ecdsa)|credentials\.json|\.pypirc)$/;
const READ_SECRET_BASH = /(?:^|[\s;&|(])(?:cat|less|more|head|tail|bat|grep|rg|awk|sed|cp|scp|source|\.)\s+(?:[^\n;&|]*[\s/'"=])?\.env(?!\.example)(?![\w-])/;
const GUARD_FILES = /(?:^|[\/\s'"=])\.claude\/(?:settings(?:\.local)?\.json|hooks\/)/;

// ---------- helpers ----------
function globToRegex(glob) {
  let re = '';
  for (let i = 0; i < glob.length; i++) {
    const c = glob[i];
    if (c === '*') {
      if (glob[i + 1] === '*') { re += '.*'; i++; if (glob[i + 1] === '/') i++; } else re += '[^/]*';
    } else if ('.+?^${}()|[]\\'.includes(c)) re += '\\' + c;
    else re += c;
  }
  return new RegExp('^' + re + '$');
}
export function matchesAny(rel, globs) { return globs.some((g) => globToRegex(g).test(rel)); }

function count(re, text) {
  if (!text) return 0;
  const g = new RegExp(re.source, re.flags.includes('g') ? re.flags : re.flags + 'g');
  return (text.match(g) || []).length;
}

function rel(p, root) {
  if (!p) return '';
  const abs = path.isAbsolute(p) ? p : path.join(root, p);
  return path.relative(root, abs).split(path.sep).join('/');
}

// Collect the text that a write tool adds and the text it removes.
function writeTexts(tool, input, root) {
  if (tool === 'Write') {
    let old = '';
    try { old = fs.readFileSync(path.isAbsolute(input.file_path) ? input.file_path : path.join(root, input.file_path), 'utf8'); } catch { /* new file */ }
    return { added: input.content || '', removed: old };
  }
  if (tool === 'Edit') return { added: input.new_string || '', removed: input.old_string || '' };
  if (tool === 'MultiEdit') return {
    added: (input.edits || []).map((e) => e.new_string || '').join('\n'),
    removed: (input.edits || []).map((e) => e.old_string || '').join('\n'),
  };
  if (tool === 'NotebookEdit') return { added: input.new_source || '', removed: '' };
  return { added: '', removed: '' };
}

// Best-effort list of paths that a shell command writes to (redirections and common file commands).
export function bashWriteTargets(cmd) {
  const out = [];
  for (const m of cmd.matchAll(/(?:^|[^<>&0-9])>{1,2}\s*([^\s;&|<>]+)/g)) out.push(m[1]);
  for (const m of cmd.matchAll(/\btee\s+(?:-a\s+)?([^\s;&|]+)/g)) out.push(m[1]);
  for (const m of cmd.matchAll(/\b(?:rm|touch|mkdir|rmdir|unlink|truncate)\s+((?:-[^\s]+\s+)*)([^;&|\n]+)/g)) out.push(...m[2].trim().split(/\s+/));
  for (const m of cmd.matchAll(/\b(?:mv|cp|ln|install)\s+((?:-[^\s]+\s+)*)([^;&|\n]+)/g)) { const a = m[2].trim().split(/\s+/); if (a.length) out.push(a[a.length - 1]); }
  for (const m of cmd.matchAll(/\bsed\s+(?:-[^\s]*i[^\s]*)\s+(?:'[^']*'|"[^"]*"|\S+)\s+([^;&|\n]+)/g)) out.push(...m[1].trim().split(/\s+/));
  for (const m of cmd.matchAll(/\bgit\s+(?:checkout|restore)\s+(?:--\s+)?([^;&|\n]+)/g)) out.push(...m[1].trim().split(/\s+/).filter((t) => !t.startsWith('-')));
  return out.map((t) => t.replace(/^['"]|['"]$/g, '')).filter((t) => t && t !== '/dev/null' && !t.startsWith('$') && !/^&?\d$/.test(t));
}

// ---------- the checks ----------
export function check(input, opts, root) {
  const tool = input.tool_name || '';
  const ti = input.tool_input || {};
  const isWrite = ['Write', 'Edit', 'MultiEdit', 'NotebookEdit'].includes(tool);
  const file = rel(ti.file_path || ti.notebook_path, root);

  if (opts.mode === 'global') {
    if (isWrite) {
      if (GUARD_FILES.test(file)) return `Writing ${file} is not allowed for agents: the guardrails are changed by a human only (AGT-081).`;
      if (SECRET_FILE.test(file)) return `Writing the secret file ${file} is not allowed (CLAUDE.md section 3: no secrets in the repository). Use .env.example with placeholders.`;
      const { added, removed } = writeTexts(tool, ti, root);
      for (const [re, what] of SECRET_PATTERNS) if (re.test(added)) return `The content contains what looks like a ${what}. Secrets never go into files (CLAUDE.md section 3, QR-SEC-01). Use an environment variable.`;
      for (const [re, what] of TEST_DISABLE_PATTERNS) {
        if (count(re, added) > count(re, removed)) return `The change adds "${what}". Never disable or weaken a test, a check or a gate to get green (AGT-081). Report the failure instead.`;
      }
    }
    if (tool === 'Bash') {
      const cmd = ti.command || '';
      if (/^\s*node\s+\.claude\/hooks\/selftest\.mjs\s*$/.test(cmd)) return null;
      if (GUARD_FILES.test(cmd) && /(?:>|\btee\b|\brm\b|\bmv\b|\bcp\b|\bsed\s+-i|\bchmod\b|\btruncate\b|\bln\b)/.test(cmd)) return 'Changing the guardrail files (.claude/settings.json, .claude/hooks/) from the shell is not allowed for agents (AGT-081).';
      for (const [re, what] of DESTRUCTIVE_BASH) if (re.test(cmd)) return `Blocked: ${what}. Destructive or check-skipping commands need a human (CLAUDE.md section 3). Ask the human or use the ci/run command.`;
      if (READ_SECRET_BASH.test(cmd)) return 'Reading .env files is not allowed: they hold secrets (CLAUDE.md section 3). Use .env.example.';
      for (const [re, what] of SECRET_PATTERNS) if (re.test(cmd)) return `The command contains what looks like a ${what}. Never put secrets on the command line.`;
    }
    return null;
  }

  if (opts.role) {
    const allowed = ROLE_PATHS[opts.role];
    if (!allowed) return `Unknown role "${opts.role}" in the guard hook.`;
    const globs = [...allowed, ...COMMON];
    if (isWrite) {
      if (file.startsWith('..')) return `${opts.role} may not write outside the repository (${file}).`;
      if (!matchesAny(file, globs)) return `${opts.role} may not write ${file}. Allowed: ${globs.join(', ') || 'nothing'} (agents.md section 11, AGT-070). Propose the change in your report instead.`;
    }
    if (tool === 'Bash') {
      for (const t of bashWriteTargets(ti.command || '')) {
        const r = rel(t, root);
        if (r.startsWith('..') || path.isAbsolute(r)) {
          if (/^\/tmp\//.test(t) || r.startsWith('../tmp/')) continue;
          return `${opts.role} may not write outside the repository from the shell (${t}).`;
        }
        if (/^(?:build|dist|out|target|node_modules|\.gradle|\.svelte-kit|coverage|reports|test-results|playwright-report)(?:\/|$)/.test(r) || /\/(?:build|dist|target|node_modules|\.gradle|\.svelte-kit|coverage)(?:\/|$)/.test(r)) continue;
        if (!matchesAny(r, globs)) return `${opts.role} may not write ${r} from the shell. Allowed: ${globs.join(', ') || 'nothing'} (agents.md section 11).`;
      }
    }
  }
  return null;
}

function logBlock(root, reason, input, opts) {
  try {
    const line = JSON.stringify({ at: new Date().toISOString(), guard: opts.mode || opts.role, tool: input.tool_name, reason }) + '\n';
    fs.appendFileSync(path.join(root, '.claude', 'hooks', 'blocked.log'), line);
  } catch { /* logging must never break the hook */ }
}

// ---------- main (only when run as a script) ----------
const isMain = import.meta.url === `file://${process.argv[1]}` || process.argv[1]?.endsWith('guard.mjs');
if (isMain) {
  let raw = '';
  process.stdin.setEncoding('utf8');
  process.stdin.on('data', (c) => { raw += c; });
  process.stdin.on('end', () => {
    let input;
    try { input = JSON.parse(raw || '{}'); } catch {
      // Fail closed: an unreadable hook input blocks the call.
      process.stdout.write(JSON.stringify({ hookSpecificOutput: { hookEventName: 'PreToolUse', permissionDecision: 'deny', permissionDecisionReason: 'Guard hook could not read its input.' } }));
      process.exit(0);
    }
    const root = process.env.CLAUDE_PROJECT_DIR || input.cwd || process.cwd();
    const reason = check(input, args, root);
    if (reason) {
      logBlock(root, reason, input, args);
      process.stdout.write(JSON.stringify({ hookSpecificOutput: { hookEventName: 'PreToolUse', permissionDecision: 'deny', permissionDecisionReason: reason } }));
    }
    process.exit(0);
  });
}
