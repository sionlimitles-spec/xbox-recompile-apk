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

## Peta toolchain (tingkat tinggi)

```text
XBLA XEX (milik pengguna, sah)
        ↓  analisa statis, bukan ripping/decrypting otomatis
XenonRecomp / ReXGlue → C++ portabel
        ↓  HLE untuk impor XDK (file, input, audio, render, save)
kode native + virtual filesystem asset
        ↓  Android NDK, ABI arm64-v8a
libbraid.so → launcher Android (stub di repo ini) → APK
```

Yang perlu disiapkan/dipelajari (perkiraan, harus diverifikasi dari biner nyata):

- **Impor XDK**: `XFile`/`XFileSystem`, `XInput`, `XAudio2`/audio stream, `XUser`/profile, `XContent`/title update, font dan text, video/movie bila ada.
- **Render**: Braid memakai engine 2D custom di atas Direct3D 9 Xbox 360. Renderer host perlu menerjemahkan tekstur, sprite, blend, dan efek blur/partikel ke Vulkan/OpenGL ES.
- **Manajemen memori XEX**: heap, alignment, dan tata letak data big-endian PowerPC perlu emulator VM yang konsisten.
- **Determinisme waktu**: mekanik rewind menyimpan riwayat state. Perbedaan kecil pada timestep atau floating point bisa merusak puzzle.
- **Audio**: trek Magnatune diputar juga dalam bentuk terbalik untuk efek world tertentu — backend audio harus mendukung playback mundur/effects.
- **Kontrol**: platformer presisi lebih cocok dengan gamepad; kontrol sentuh sebagai fallback perlu desain ulang, bukan sekadar tombol virtual.

Titik integrasi di repo ini: `launcher/app/src/main/jniLibs/arm64-v8a/` (di-ignore git) untuk native library hasil build, lalu Activity di `launcher/` yang memanggil entry point game.

## Milestone yang disarankan

1. Analisa impor/XDK dan daftar fungsi yang perlu HLE.
2. Build C++ hasil recompile untuk host PC, jalankan dengan renderer null.
3. HLE input + audio + file, sampai menu utama tampil.
4. Renderer Vulkan/OpenGL ES dasar, lalu sprite/efek Braid.
5. Port ke Android NDK, uji gamepad Bluetooth/OTG.
6. Test savestate/rewind tiap world dan performa di perangkat low-end.

Estimasi realistis: **minggu–bulan**, tergantung seberapa banyak impor XDK yang belum ter-cover.

## Yang TIDAK disediakan repo ini

- Sumber ISO/XEX/ROM, tautan unduhan, atau nama situs penyedia game.
- Skrip ripping disc, decrypt XEX, atau bypass DRM/lisensi.
- File asset, audio, atau teks dari Braid.

Lihat [LEGAL.md](../LEGAL.md). Kalau Anda tidak memiliki salinan sah, repo ini tidak bisa dipakai untuk target Braid.

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
