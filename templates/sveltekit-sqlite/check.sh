#!/usr/bin/env bash
# Installs, type-checks, tests and builds the app. Needs Node.js 24 or newer.
set -euo pipefail

cd "$(dirname "$0")"

npm install --no-audit --no-fund --loglevel=error
npm run --silent check
npm test --silent
npm run --silent build
echo "CHECK PASSED"
