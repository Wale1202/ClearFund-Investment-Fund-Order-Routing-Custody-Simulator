#!/usr/bin/env bash
#
# Start the full ClearFund stack (PostgreSQL + backend) via Docker Compose.
# Flyway applies the V1-V3 migrations on backend startup.
#
# Usage:
#   scripts/run-local.sh            # build and start in the foreground
#   scripts/run-local.sh --detach   # start in the background
#
set -euo pipefail

# Resolve the project root regardless of where the script is called from.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker does not appear to be running. Start Docker and retry." >&2
  exit 1
fi

DETACH=""
if [[ "${1:-}" == "--detach" || "${1:-}" == "-d" ]]; then
  DETACH="--detach"
fi

echo "Starting ClearFund (backend on http://localhost:8080)..."
docker compose up --build ${DETACH}
