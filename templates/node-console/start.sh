#!/usr/bin/env bash
# Runs the console program from a fresh checkout or unzip: ./start.sh
# The output appears in the terminal; the exit code of the program is the exit code of this script.
set -euo pipefail

cd "$(dirname "$0")"

command -v node >/dev/null || {
	echo "start.sh: Node.js 24 or newer is missing. Install it from https://nodejs.org" >&2
	exit 1
}
[ "$(node -p 'process.versions.node.split(".")[0]')" -ge 24 ] || {
	echo "start.sh: Node.js 24 or newer is needed (found $(node -v))." >&2
	exit 1
}

if [ ! -d node_modules ]; then
	npm install --no-audit --no-fund --loglevel=error >&2
fi

exec node src/main.ts "$@"
