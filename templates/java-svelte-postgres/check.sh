#!/usr/bin/env bash
# Builds and tests everything: frontend (types, tests, build), then backend (compile, tests).
# Needs Node 20+, a Java 21 toolchain and the PostgreSQL named by APP_DB_URL (default: local spike_canteen).
set -euo pipefail

cd "$(dirname "$0")"

(cd frontend && npm ci --no-audit --no-fund && npm run check && npm test && npm run build)
(cd backend && ./gradlew --no-daemon build)
echo "CHECK PASSED"
