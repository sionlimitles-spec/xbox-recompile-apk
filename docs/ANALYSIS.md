# Analisa lokal (Tahap 1)

Skrip `scripts/analyze_local.py` hanya membuat **inventarisasi lokal** dari file game yang Anda sediakan sendiri.

## Yang dilakukan

- Memastikan file berada **di luar folder repo**.
- Menghitung ukuran, SHA-256, dan 16 byte pertama.
- Menulis laporan JSON ke `analysis/`.

## Yang TIDAK dilakukan

- Tidak menyalin atau memindahkan file game ke repo.
- Tidak meng-upload apa pun.
- Tidak mendekripsi XEX, tidak melewati DRM, tidak membongkar disc.
- Tidak mengarahkan ke sumber ISO/ROM/XEX mana pun.

## Pakai

### Di PC (skrip Python)

```sh
python3 scripts/analyze_local.py --file /path/di/luar/repo/game.xex --label braid
```

Hasil: `analysis/braid.json` (folder `analysis/` sudah di-ignore git).

### Di HP (tombol Import di launcher)

Launcher punya tombol **Import Game** yang membaca metadata file langsung di perangkat (nama, ukuran, SHA-256, 16 byte pertama). File **tidak disimpan dan tidak diupload** — hanya dibaca sekali lalu hasilnya ditampilkan. Lihat [UI.md](UI.md).

Laporan ini berguna untuk:
- memverifikasi identitas salinan Anda,
- membandingkan dua salinan yang sama,
- menjadi catatan lokal sebelum analisa lanjutan dengan tool recompiler pilihan Anda.

Lihat [LEGAL.md](LEGAL.md) untuk batasan penggunaan.
