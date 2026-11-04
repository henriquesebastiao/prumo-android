#!/usr/bin/env bash
# Copies the server's contract (openapi/openapi.json) to the test fixtures of :core:network, where
# ContractTest (and the features' contract tests) check the hand-written DTOs against it.
# Usage: scripts/update-openapi.sh [path of the prumo repository, default ~/git/prumo]
set -euo pipefail
server="${1:-$HOME/git/prumo}"
here="$(cd "$(dirname "$0")/.." && pwd)"
source_file="$server/openapi/openapi.json"
target="$here/core/network/src/testFixtures/resources/openapi.json"
if [[ ! -f "$source_file" ]]; then
  echo "No $source_file: pass the path of the prumo repository." >&2
  exit 1
fi
cp "$source_file" "$target"
echo "Copied $source_file to ${target#"$here"/}. Run ./gradlew :core:network:test."
