# Target: Braid

> ⚠️ Ini adalah catatan perencanaan, **bukan** prosedur untuk membongkar atau menyalin game. Tidak ada file game di repo ini.

## Identitas

- **Judul:** Braid
- **Developer:** Number None (Jonathan Blow); Anniversary Edition oleh Thekla, Inc.
- **Rilis awal:** Agustus 2008, Xbox Live Arcade (Xbox 360)
- **Port lain:** Windows (2009), Mac OS X (2009), PlayStation 3 (2009), Linux (2010)
- **Anniversary Edition:** Mei 2024 — Windows, macOS, Linux, PS4/PS5, Switch, Xbox One/Series, **Android & iOS** (dirilis via Netflix, lalu di-delist 14 Juli 2025)

## Kenapa menarik secara teknis

- Game 2D dengan **engine custom** dan mekanik manipulasi waktu: rewind, objek kebal waktu, waktu mengikuti posisi horizontal, bayangan hasil rewind, dan efek pelambatan waktu.
- Visual painterly oleh David Hellman dan musik berlisensi dari artis Magnatune — artinya **asset dan audio juga dilindungi hak cipta**, bukan hanya kode.
- Ukuran dan kompleksitas tergolong "indie standar": panduan porting console → native memperkirakan **beberapa minggu sampai beberapa bulan** per judul, bukan tinggal jalankan tool.

## Platform yang realistis

| Rilis | Arsitektur | Toolchain yang relevan |
|---|---|---|
| Xbox 360 (XBLA) | PowerPC Xenon | `XenonRecomp` / `ReXGlue`, lalu NDK untuk `arm64-v8a` |
| PC (2009) | x86 | pendekatan recompile x86 → C/C++ |
| PS3 (2009) | PowerPC64 | `ps3recomp` (butuh ELF yang didekripsi secara sah) |

Catatan: **tidak ada rilis Braid untuk Xbox generasi pertama**, jadi `XboxXBE-Recomp` bukan jalur yang tepat. Kalau memang Xbox, berarti Xbox 360.

## Status di repo ini

- Target **direncanakan**, bukan sudah didukung.
- `configs/braid.toml` hanya berisi placeholder.
- Tidak ada XEX/XBE/ISO, asset, atau APK game di sini.
- Stub launcher di `launcher/` baru sebatas deteksi gamepad.

## Alternatif yang lebih bersih secara hukum

Kalau tujuan Anda adalah **punya APK puzzle-platformer bergaya Braid di Android**, jalur paling aman:

1. Bangun game **original** dengan mekanik rewind waktu memakai Godot 4 / libGDX.
2. Pakai **nama, seni, level, musik, dan teks Anda sendiri** — jangan menyalin ekspresi khas Braid.
3. Repo ini tetap bisa dipakai sebagai kerangka launcher + gamepad.

Mekanik "mundurkan waktu" sebagai ide umum tidak eksklusif; yang dilindungi adalah implementasi, aset, level, dan narasi spesifiknya.

## Rujukan

- https://en.wikipedia.org/wiki/Braid_(video_game)
- https://github.com/hedge-dev/XenonRecomp
- https://github.com/sp00nznet/ps3recomp/blob/master/docs/GAME_PORTING_GUIDE.md
