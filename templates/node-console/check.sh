#!/usr/bin/env bash
# Installs, type-checks and tests the program. Needs Node.js 24 or newer.
set -euo pipefail

cd "$(dirname "$0")"

npm install --no-audit --no-fund --loglevel=error
npm run --silent typecheck
npm test --silent
echo "CHECK PASSED"
