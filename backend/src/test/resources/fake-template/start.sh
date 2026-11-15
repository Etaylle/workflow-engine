#!/usr/bin/env bash
# A tiny fake app for the preview tests: prints an ASCII duck, then serves HTTP on APP_PORT.
set -euo pipefail

cat <<'DUCK'
    __
  <(o )___
   ( ._> /
    `---'
DUCK
echo "listening on port ${APP_PORT:?} with database ${APP_DB_URL:-none}"
exec python3 -m http.server "$APP_PORT" --bind 127.0.0.1
