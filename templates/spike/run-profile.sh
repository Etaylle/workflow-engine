#!/usr/bin/env bash
# M3 build spike: builds the task "Order a meal" of the canteen contract with Claude Code headless,
# one invocation per development step, a git commit in the workspace after each green step.
#
# Usage: templates/spike/run-profile.sh <short|full>
# Needs: claude, jq, psql, npm, Java 21 toolchain, PostgreSQL 17 on localhost:5432 (role engine).
# Run outside any sandbox that blocks sockets. Writes only to $SPIKE_ROOT (default $TMPDIR).
# Results: $SPIKE_ROOT/spike-<profile>-logs/results.tsv (one row per invocation) and one JSON per call.
set -euo pipefail

PROFILE="${1:?usage: run-profile.sh <short|full>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
TEMPLATE="$HERE/../java-svelte-postgres"
ROOT="${SPIKE_ROOT:-${TMPDIR:-/tmp}}"
WORKSPACE="$ROOT/spike-$PROFILE"
LOGS="$ROOT/spike-$PROFILE-logs"
STEPS_FILE="$HERE/steps-$PROFILE.txt"
MODEL="${SPIKE_MODEL:-sonnet}"
STEP_TIMEOUT="${SPIKE_STEP_TIMEOUT:-1200}"
CHECK_TIMEOUT="${SPIKE_CHECK_TIMEOUT:-600}"
FIX_CAP="1.5"

case "$PROFILE" in
short) TOTAL_BUDGET="${SPIKE_BUDGET:-8}" ;;
full) TOTAL_BUDGET="${SPIKE_BUDGET:-20}" ;;
*)
	echo "profile must be short or full" >&2
	exit 2
	;;
esac

[ -f "$STEPS_FILE" ] || {
	echo "missing $STEPS_FILE" >&2
	exit 2
}
[ ! -e "$WORKSPACE" ] || {
	echo "workspace $WORKSPACE exists; remove it first" >&2
	exit 2
}

DB="spike_canteen"
[ "$PROFILE" = "full" ] && DB="spike_canteen_full"
export APP_DB_URL="jdbc:postgresql://localhost:5432/$DB"
export APP_DB_USER="engine"
export APP_PORT="18080"

PREAMBLE='You are one step of a task build run by an orchestrator, working alone in the current folder (the workspace). Read CLAUDE.md and CONTRACT.md first. Do only the step below, nothing from other steps. Earlier steps left their documents in docs/. Do not run git commit or any other git command that changes the repository; the orchestrator commits. Use ./check.sh as the only way to build and test everything; it prints CHECK PASSED when green. Finish with one line saying what you did.'

ALLOWED='Read,Edit,Write,Glob,Grep,Bash(./check.sh:*),Bash(./gradlew:*),Bash(npm:*),Bash(cd:*),Bash(git status:*),Bash(git diff:*),Bash(curl:*),Bash(ls:*),Bash(cat:*),Bash(mkdir:*),Bash(sleep:*),Bash(kill:*)'

mkdir -p "$LOGS"
psql -h localhost -U engine -d postgres -q -c "DROP DATABASE IF EXISTS $DB" -c "CREATE DATABASE $DB"
mkdir -p "$WORKSPACE"
cp -R "$TEMPLATE/." "$WORKSPACE/"
cp "$HERE/contract-canteen.md" "$WORKSPACE/CONTRACT.md"
mkdir -p "$WORKSPACE/docs"
cd "$WORKSPACE"
git init -q
git config user.name "spike"
git config user.email "spike@localhost"
git add -A
git commit -q -m "Template and contract"

RESULTS="$LOGS/results.tsv"
printf 'step\tattempt\twall_s\tcost_usd\tturns\tsubtype\tgate\n' >"$RESULTS"
SPENT="0"
RUN_START="$(date +%s)"

with_timeout() {
	local seconds="$1"
	shift
	perl -e 'alarm shift; exec @ARGV' "$seconds" "$@"
}

# Runs one claude invocation; sets LAST_COST. Args: key attempt cap prompt
invoke() {
	local key="$1" attempt="$2" cap="$3" prompt="$4"
	local out="$LOGS/$key-$attempt.json" start end
	start="$(date +%s)"
	with_timeout "$STEP_TIMEOUT" claude -p "$PREAMBLE

$prompt" \
		--model "$MODEL" --output-format json --permission-mode acceptEdits \
		--max-budget-usd "$cap" --no-session-persistence \
		--setting-sources project,local --strict-mcp-config --disable-slash-commands \
		--tools "Read,Edit,Write,Glob,Grep,Bash" --allowedTools "$ALLOWED" \
		>"$out" 2>"$LOGS/$key-$attempt.err" </dev/null || true
	end="$(date +%s)"
	LAST_COST="$(jq -r '.total_cost_usd // 0' "$out" 2>/dev/null || echo 0)"
	local turns subtype
	turns="$(jq -r '.num_turns // 0' "$out" 2>/dev/null || echo 0)"
	subtype="$(jq -r '.subtype // "no_json"' "$out" 2>/dev/null || echo no_json)"
	if grep -q "HARD STOP" "$out" "$LOGS/$key-$attempt.err" 2>/dev/null; then
		echo "HARD STOP reported; aborting" >&2
		exit 3
	fi
	SPENT="$(awk -v a="$SPENT" -v b="$LAST_COST" 'BEGIN { printf "%.4f", a + b }')"
	LAST_ROW="$key\t$attempt\t$((end - start))\t$LAST_COST\t$turns\t$subtype"
}

record() {
	printf "%b\t%s\n" "$LAST_ROW" "$1" >>"$RESULTS"
}

remaining() {
	awk -v t="$TOTAL_BUDGET" -v s="$SPENT" 'BEGIN { printf "%.2f", t - s }'
}

cap_for() {
	awk -v c="$1" -v r="$(remaining)" 'BEGIN { printf "%.2f", (c < r ? c : r) }'
}

budget_left() {
	awk -v r="$(remaining)" 'BEGIN { exit !(r >= 0.30) }'
}

run_check() {
	local log="$LOGS/$1-check-$2.log"
	if with_timeout "$CHECK_TIMEOUT" ./check.sh >"$log" 2>&1 </dev/null; then
		return 0
	fi
	return 1
}

commit_step() {
	git add -A
	git commit -q --allow-empty -m "Step $1 green"
}

FAILED=""
KEY=""
GATE=""
CAP=""
PROMPT=""

run_step() {
	local attempt=1
	budget_left || {
		FAILED="budget exhausted before $KEY"
		return
	}
	invoke "$KEY" "$attempt" "$(cap_for "$CAP")" "$PROMPT"
	if [ "$GATE" = "none" ]; then
		record "n/a"
		commit_step "$KEY"
		return
	fi
	while true; do
		if run_check "$KEY" "$attempt"; then
			record "green"
			commit_step "$KEY"
			return
		fi
		record "red"
		if [ "$attempt" -ge 3 ] || ! budget_left; then
			FAILED="$KEY red after attempt $attempt (spent $SPENT)"
			return
		fi
		attempt=$((attempt + 1))
		local tail_log
		tail_log="$(tail -60 "$LOGS/$KEY-check-$((attempt - 1)).log")"
		invoke "$KEY" "$attempt" "$(cap_for "$FIX_CAP")" "The last ./check.sh run after the step failed. Fix the cause without weakening any test, then make ./check.sh print CHECK PASSED. Output tail:
$tail_log"
	done
}

while IFS= read -r line; do
	if [[ "$line" == "=== "* ]]; then
		if [ -n "$KEY" ] && [ -z "$FAILED" ]; then run_step; fi
		IFS='|' read -r KEY GATE CAP <<<"${line#=== }"
		KEY="$(echo "$KEY" | xargs)"
		GATE="$(echo "$GATE" | xargs)"
		CAP="$(echo "$CAP" | xargs)"
		PROMPT=""
	else
		PROMPT="$PROMPT$line
"
	fi
done <"$STEPS_FILE"
if [ -n "$KEY" ] && [ -z "$FAILED" ]; then run_step; fi

TOTAL_S="$(($(date +%s) - RUN_START))"
if run_check final 0; then FINAL="GREEN"; else FINAL="RED"; fi
{
	echo "profile=$PROFILE final=$FINAL total_cost_usd=$SPENT total_wall_s=$TOTAL_S budget_usd=$TOTAL_BUDGET"
	echo "failed=${FAILED:-none}"
} | tee "$LOGS/summary.txt"
