#!/usr/bin/env bash
# A tiny fake console program for the preview tests: prints an ASCII duck and ends.
set -euo pipefail

cat <<'DUCK'
    __
  <(o )___
   ( ._> /
    `---'
DUCK
exit "${FAKE_EXIT:-0}"
