#!/usr/bin/env bash
#
# Run the ClearFund test suite. Extra arguments are passed straight through
# to Maven.
#
# Usage:
#   scripts/run-tests.sh                       # all tests
#   scripts/run-tests.sh -Dtest=OrderStatusTest
#   scripts/run-tests.sh '-Dtest=!OrderLifecycleIntegrationTest'  # skip Docker test
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! docker info >/dev/null 2>&1; then
  echo "NOTE: Docker not available - the Testcontainers integration test will be skipped."
fi

echo "Running tests..."
mvn -B test "$@"
