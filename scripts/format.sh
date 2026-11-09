#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
exec npx --yes prettier@3.9.10 --check README.md .github/workflows/ci.yml .github/workflows/release.yml
