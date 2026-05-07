#!/usr/bin/env bash
# Format Kotlin sources across option1/ and option2/ with ktlint.
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v ktlint >/dev/null 2>&1; then
  echo "ktlint not found. Install with: brew install ktlint" >&2
  exit 1
fi
ktlint --format \
  'option1/src/**/*.kt' \
  'option1/src/**/*.kts' \
  'option2/src/**/*.kt' \
  'option2/src/**/*.kts' \
  '**/*.gradle.kts' \
  '!**/build/**'
