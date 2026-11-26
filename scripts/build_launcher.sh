#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME ke path Android SDK Anda}"

if ! command -v gradle >/dev/null 2>&1; then
  echo "gradle tidak ditemukan. Pasang Gradle 8.7+ lalu coba lagi." >&2
  exit 1
fi

if [[ ! -d "$ANDROID_HOME/ndk/27.0.12077973" ]]; then
  echo "Peringatan: NDK 27.0.12077973 belum ada. AGP akan mencoba mengunduhnya." >&2
  echo "Pasang manual: sdkmanager \"ndk;27.0.12077973\" \"cmake;3.22.1\"" >&2
fi

echo "Membangun launcher Braid Android (termasuk libbraid_stub.so)..."
gradle -p "$ROOT/launcher" assembleDebug

APK="$ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
[[ -f "$APK" ]] || { echo "APK tidak ditemukan: $APK" >&2; exit 1; }
echo "Selesai: $APK"
