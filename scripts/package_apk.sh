#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
DST_DIR="$ROOT/dist"
DST="$DST_DIR/braid-android-debug.apk"

[[ -f "$SRC" ]] || { echo "Jalankan scripts/build_launcher.sh dulu." >&2; exit 1; }

mkdir -p "$DST_DIR"
cp "$SRC" "$DST"
echo "APK Braid Android tersalin ke: $DST"
echo "Ingat: ini masih stub; tidak ada data game di dalamnya."
