#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "Root proyek: $ROOT"

: "${ANDROID_HOME:?Set ANDROID_HOME ke path Android SDK Anda}"
[[ -d "$ANDROID_HOME" ]] || { echo "ANDROID_HOME tidak ditemukan: $ANDROID_HOME" >&2; exit 1; }

if command -v java >/dev/null 2>&1; then
  echo "java: $(java -version 2>&1 | head -n 1)"
else
  echo "Peringatan: java belum ada di PATH" >&2
fi

if command -v gradle >/dev/null 2>&1; then
  echo "gradle: $(gradle --version 2>/dev/null | grep '^Gradle' || true)"
else
  echo "Peringatan: gradle belum ada di PATH" >&2
fi

if [[ -d "$ANDROID_HOME/ndk/27.0.12077973" ]]; then
  echo "ndk: 27.0.12077973"
else
  echo "Peringatan: NDK 27.0.12077973 belum terpasang." >&2
fi

echo "Environment OK (launcher Braid Android + stub native)."
