#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
GD="$ROOT/.gradle-local/gradle-8.9"
ZIP="$ROOT/.gradle-local/gradle-8.9-bin.zip"
if [ ! -x "$GD/bin/gradle" ]; then
  mkdir -p "$ROOT/.gradle-local"
  echo "[SayIt] Downloading Gradle 8.9 once..."
  curl -L "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -o "$ZIP"
  unzip -o "$ZIP" -d "$ROOT/.gradle-local" >/dev/null
fi
exec "$GD/bin/gradle" "$@"
