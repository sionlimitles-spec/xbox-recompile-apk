#!/usr/bin/env python3
"""Inventarisasi lokal untuk file game yang disediakan pengguna.

Skrip ini HANYA membaca file, menghashingnya, dan menulis laporan ke analysis/.
Tidak menyalin, memindahkan, meng-upload, mendekripsi, atau membongkar DRM.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parent.parent
ANALYSIS_DIR = REPO_ROOT / "analysis"


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def first_bytes(path: Path, count: int = 16) -> str:
    with path.open("rb") as handle:
        return handle.read(count).hex()


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Buat laporan lokal untuk file game milik Anda sendiri."
    )
    parser.add_argument(
        "--file",
        required=True,
        help="Path ke file game di mesin Anda, di luar folder repo ini.",
    )
    parser.add_argument(
        "--label",
        default=None,
        help="Nama laporan (default: nama file tanpa ekstensi).",
    )
    args = parser.parse_args()

    source = Path(args.file).expanduser().resolve()

    if not source.is_file():
        print(f"ERROR: bukan file regular: {source}", file=sys.stderr)
        return 1

    try:
        source.relative_to(REPO_ROOT)
    except ValueError:
        pass
    else:
        print(
            "ERROR: file tidak boleh berada di dalam repo. "
            "Simpan di luar folder xbox-recompile-apk.",
            file=sys.stderr,
        )
        return 1

    ANALYSIS_DIR.mkdir(exist_ok=True)
    label = args.label or source.stem

    report = {
        "label": label,
        "source_path": str(source),
        "size_bytes": source.stat().st_size,
        "sha256": sha256_of(source),
        "first_bytes_hex": first_bytes(source),
        "note": "Hanya inventarisasi lokal; tidak ada dekripsi, ripping, atau upload.",
    }

    output = ANALYSIS_DIR / f"{label}.json"
    output.write_text(json.dumps(report, indent=2), encoding="utf-8")

    print(json.dumps(report, indent=2))
    print(f"\nLaporan tersimpan: {output}")
    print("Peringatan: gunakan hanya salinan game yang sah Anda miliki.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
