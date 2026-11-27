#!/usr/bin/env bash
#
# Checks that the committed OpenAPI snapshots still describe the running server.
#
# doc/oas.json and doc/oas-client.json are the source of the generated TypeScript client, but nothing used
# to compare them against the code: they drifted silently until someone noticed a field or a route was
# missing from the client. This script starts a server built from the current sources, asks it for both
# specifications and reports any difference.
#
# Usage: doc/check-api-spec.sh [--port PORT]
#
# Exit codes: 0 = snapshots are current, 1 = drift detected, 2 = the check could not run.

set -euo pipefail

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly OAS="$REPO_ROOT/doc/oas.json"
readonly CLIENT_OAS="$REPO_ROOT/doc/oas-client.json"
readonly SERVER="$REPO_ROOT/backend/build/install/dres-dist/bin/backend"

PORT=8080
while [ $# -gt 0 ]; do
  case "$1" in
    --port) PORT="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

# Work directory for the server instance this script starts. It is deliberately placed inside the repository
# rather than the system temp directory so the CI job can upload the server log with a relative path; the
# trap below removes it, and `.gitignore` covers `build/`.
WORK="$REPO_ROOT/build/api-spec-check"
rm -rf "$WORK"
mkdir -p "$WORK"

# The server reads its command loop from stdin; a FIFO keeps it from exiting on EOF and lets the shutdown
# command reach it.
FIFO="$WORK/stdin"
mkfifo "$FIFO"
cleanup() {
  if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "quit" > "$FIFO" 2>/dev/null || true
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      kill -0 "$SERVER_PID" 2>/dev/null || break
      sleep 0.5
    done
    kill -9 "$SERVER_PID" 2>/dev/null || true
  fi
  exec 3>&- 2>/dev/null || true
  rm -rf "$WORK"
}
trap cleanup EXIT

if [ ! -x "$SERVER" ]; then
  echo "The distribution is not installed. Running :backend:installDist..."
  (cd "$REPO_ROOT" && ./gradlew :backend:installDist --console=plain --quiet) || {
    echo "FAILED: could not build the distribution." >&2
    exit 2
  }
fi

cat > "$WORK/config.json" <<JSON
{
  "httpPort": $PORT,
  "dataPath": "$WORK/data",
  "eventsLocation": "$WORK/events",
  "auditLocation": "$WORK/audit",
  "externalMediaLocation": "$WORK/external",
  "presetsLocation": "$WORK/type-presets"
}
JSON

echo "Starting a server from the current sources on port $PORT..."
"$SERVER" "$WORK/config.json" < "$FIFO" > "$WORK/server.log" 2>&1 &
SERVER_PID=$!
# Holding the write end open keeps the FIFO from signalling EOF.
exec 3> "$FIFO"

STATUS=""
for _ in $(seq 1 120); do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "FAILED: the server exited during start-up. Log:" >&2
    tail -40 "$WORK/server.log" >&2
    exit 2
  fi
  STATUS="$(curl -fsS -o /dev/null -w '%{http_code}' "http://localhost:$PORT/api/v2/status/info" 2>/dev/null || true)"
  if [ "$STATUS" = "200" ]; then
    break
  fi
  sleep 1
done

if [ "$STATUS" != "200" ]; then
  echo "FAILED: the server did not become ready on port $PORT. Log:" >&2
  tail -40 "$WORK/server.log" >&2
  exit 2
fi

curl -fsS "http://localhost:$PORT/openapi.json" -o "$WORK/oas.json" || { echo "FAILED: no openapi.json." >&2; exit 2; }
curl -fsS "http://localhost:$PORT/clientapi.json" -o "$WORK/oas-client.json" || { echo "FAILED: no clientapi.json." >&2; exit 2; }

# The specification is generated from Javalin's handler map, so the ORDER of sibling operations and of
# schema properties follows hash iteration order and differs between JVM runs -- `post` and `patch` on the
# same path swap places, and a byte comparison reports that as drift. Comparing the parsed documents with
# sorted keys keeps the check about content; only a genuine addition, removal or change of a field fails it.
canonicalise() {
  python3 -c '
import json, sys

try:
    with open(sys.argv[1]) as handle:
        document = json.load(handle)
except (OSError, ValueError) as error:
    print(f"cannot read {sys.argv[1]}: {error}", file=sys.stderr)
    sys.exit(2)

json.dump(document, sys.stdout, indent=2, sort_keys=True)
print()
' "$1"
}

status=0
for pair in "oas.json:$OAS" "oas-client.json:$CLIENT_OAS"; do
  name="${pair%%:*}"
  committed="${pair#*:}"
  if ! canonicalise "$WORK/$name" > "$WORK/$name.canonical" 2>/dev/null; then
    echo "FAILED: could not parse the generated $name (python3 is required)." >&2
    exit 2
  fi
  if ! canonicalise "$committed" > "$WORK/$name.committed" 2>/dev/null; then
    echo "FAILED: could not parse the committed $committed (python3 is required)." >&2
    exit 2
  fi
  if ! diff -q "$WORK/$name.canonical" "$WORK/$name.committed" > /dev/null; then
    echo
    echo "DRIFT: $name differs from the running server. Differences ('<' committed, '>' current):"
    diff -u "$WORK/$name.committed" "$WORK/$name.canonical" | sed -n '1,120p'
    status=1
  fi
done

if [ "$status" -eq 0 ]; then
  echo "The committed specifications match the running server."
else
  echo
  echo "Run ./gradlew regenerateApi against a running server and commit the result."
fi
exit "$status"
