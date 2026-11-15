#!/usr/bin/env bash
# Starts the app from a fresh checkout or unzip with one command: ./start.sh
# It checks what is needed, creates the database when it is missing, builds the app and starts it.
# Environment (all optional): APP_PORT (default 18080); APP_DB_URL, APP_DB_USER, APP_DB_PASSWORD
# (default: PostgreSQL on localhost:5432, database "app", your user; the database is created when
# missing). When APP_DB_URL is set the database must already exist.
set -euo pipefail

cd "$(dirname "$0")"
export APP_PORT="${APP_PORT:-18080}"

fail() {
	echo "start.sh: $*" >&2
	exit 1
}

java_major() {
	"$1" -version 2>&1 | awk -F'"' '/version/ {split($2, v, "."); print v[1]; exit}'
}

find_java() {
	local candidate home
	for candidate in "${JAVA_HOME:+$JAVA_HOME/bin/java}" "$(command -v java || true)"; do
		[ -n "$candidate" ] && [ -x "$candidate" ] || continue
		[ "$(java_major "$candidate")" -ge 21 ] 2>/dev/null && {
			echo "$candidate"
			return 0
		}
	done
	if [ -x /usr/libexec/java_home ] && home="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
		echo "$home/bin/java"
		return 0
	fi
	return 1
}

JAVA="$(find_java)" || fail "Java 21 or newer is missing. Install a JDK 21 (https://adoptium.net) and run ./start.sh again."
command -v node >/dev/null || fail "Node.js 20 or newer is missing. Install it from https://nodejs.org and run ./start.sh again."
command -v npm >/dev/null || fail "npm is missing. It comes with Node.js (https://nodejs.org)."

if [ -z "${APP_DB_URL:-}" ]; then
	db_name="${APP_DB_NAME:-app}"
	export APP_DB_URL="jdbc:postgresql://localhost:5432/$db_name"
	export APP_DB_USER="${APP_DB_USER:-$(whoami)}"
	command -v psql >/dev/null || fail "PostgreSQL tools (psql) are missing. Install PostgreSQL 15 or newer (macOS: brew install postgresql@17)."
	pg_isready -q -h localhost -p 5432 || fail "PostgreSQL is not running on localhost:5432. Start it (macOS: brew services start postgresql@17) and run ./start.sh again."
	exists="$(PGPASSWORD="${APP_DB_PASSWORD:-}" psql -h localhost -U "$APP_DB_USER" -d postgres -qtA -c "select 1 from pg_database where datname='$db_name'")" ||
		fail "Could not connect to PostgreSQL as user $APP_DB_USER. Set APP_DB_USER and APP_DB_PASSWORD."
	if [ -z "$exists" ]; then
		echo "start.sh: creating the database $db_name"
		PGPASSWORD="${APP_DB_PASSWORD:-}" psql -h localhost -U "$APP_DB_USER" -d postgres -q -c "create database $db_name" ||
			fail "Could not create the database $db_name."
	fi
fi

echo "start.sh: building the app (the first build downloads packages and can take minutes)"
(cd backend && ./gradlew --console=plain --no-daemon bootJar)

jar="$(find backend/build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)"
[ -n "$jar" ] || fail "no jar was built in backend/build/libs"

echo "start.sh: the app starts on http://localhost:$APP_PORT (Ctrl+C stops it)"
exec "$JAVA" -jar "$jar" --server.port="$APP_PORT"
