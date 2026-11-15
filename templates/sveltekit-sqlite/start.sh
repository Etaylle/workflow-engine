#!/usr/bin/env bash
# Starts the app from a fresh checkout or unzip with one command: ./start.sh
# Environment (all optional): PORT (default 18080), APP_DB_FILE (default data/app.db; created and
# migrated at start). No database server is needed.
set -euo pipefail

cd "$(dirname "$0")"
export PORT="${PORT:-${APP_PORT:-18080}}"

command -v node >/dev/null || {
	echo "start.sh: Node.js 24 or newer is missing. Install it from https://nodejs.org" >&2
	exit 1
}
[ "$(node -p 'process.versions.node.split(".")[0]')" -ge 24 ] || {
	echo "start.sh: Node.js 24 or newer is needed (found $(node -v))." >&2
	exit 1
}

echo "start.sh: building the app (the first build downloads packages)"
npm install --no-audit --no-fund --loglevel=error
npm run --silent build

echo "start.sh: the app starts on http://localhost:$PORT (Ctrl+C stops it)"
exec node build
