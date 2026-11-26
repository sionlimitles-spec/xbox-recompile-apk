#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME ke path Android SDK Anda}"

if ! command -v gradle >/dev/null 2>&1; then
  echo "gradle tidak ditemukan. Pasang Gradle 8.x lalu coba lagi." >&2
  exit 1
fi

echo "Membangun stub launcher..."
gradle -p "$ROOT/launcher" assembleDebug

APK="$ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
[[ -f "$APK" ]] || { echo "APK tidak ditemukan: $APK" >&2; exit 1; }
echo "Selesai: $APK"
