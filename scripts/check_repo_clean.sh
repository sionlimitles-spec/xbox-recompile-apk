#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Memeriksa isi repo terhadap file game / binary yang tidak boleh di-commit..."

mapfile -t banned < <(cd "$ROOT" && git ls-files | grep -Ei '\.(iso|xbe|xex|bin|rom|apk|aab|so|zip|7z|rar|tar|gz|wav|mp3|ogg|flac)$' || true)

if ((${#banned[@]} > 0)); then
  echo "ERROR: file terlarang ikut ter-commit:" >&2
  printf '  - %s\n' "${banned[@]}" >&2
  exit 1
fi

too_big=0
while IFS= read -r file; do
  [[ -f "$ROOT/$file" ]] || continue
  size="$(stat -c%s "$ROOT/$file" 2>/dev/null || echo 0)"
  if ((size > 52428800)); then
    echo "ERROR: file terlalu besar untuk repo (>$((52428800 / 1048576)) MB): $file" >&2
    too_big=1
  fi
done < <(cd "$ROOT" && git ls-files)

if ((too_big != 0)); then
  exit 1
fi

echo "OK — tidak ada data game atau binary hasil build di repo."
