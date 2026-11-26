# Braid Android

Launcher Android **khusus target Braid** (Xbox 360 / XBLA) sebagai titik integrasi eksperimen native recompile. Fokusnya: memeriksa perangkat (gamepad), mengimpor metadata file game secara lokal, dan memuat runtime native hasil recompile milik sendiri.

> ⚠️ **Batasan hukum**: repo ini **tidak berisi** ROM/ISO/XBE/XEX, asset game, binary hasil recompile, atau APK game berhak cipta. Isinya hanya launcher, skrip build, dan dokumentasi. Gunakan hanya salinan game yang sah Anda miliki, untuk pemakaian sendiri, dan jangan distribusikan hasil recompile-nya. Lihat [docs/LEGAL.md](docs/LEGAL.md).

## Fitur launcher

- Deteksi gamepad Bluetooth/OTG via `InputManager`.
- Tombol **Import Game** — file picker sistem, hanya membaca metadata di perangkat:
  - nama, ukuran, SHA-256, 16 byte pertama,
  - deteksi magic (`XEX`, `CON`/`STFS`, `XBE`, ZIP/RAR/7z, ELF),
  - progres saat hashing file besar, fallback picker bila DocumentsUI tidak ada.
- Tombol, kartu koleksi, dan ikon semuanya benar-benar merespons (ripple + Toast/dialog).
- Tombol **Play** memanggil JNI `BraidNative.start()` bila runtime asli tersedia; saat masih stub, diberi penjelasan yang jelas.
- `libbraid_stub.so` bawaan supaya launcher bisa diuji tanpa runtime game.
- Diagnostik: ABI, versi native, daftar gamepad, status library.

## Isi repo

- `launcher/` — aplikasi Android (Java, tanpa AndroidX) + JNI stub C++.
- `scripts/` — pengecekan environment, kebersihan repo, analisa lokal, build, packaging.
- `docs/` — setup, alur recompile, **[analisa kebutuhan Braid](docs/BRAID-REQUIREMENTS.md)**, hukum, dan checklist target.
- `configs/braid.toml` — konfigurasi placeholder (tanpa path game sungguhan).
- `.github/workflows/build.yml` — CI build APK stub + guard terhadap file game.

## Yang TIDAK ada di sini

- File game, disc image, atau asset dari judul mana pun.
- Tautan/sumber unduhan ISO/ROM/XEX, atau skrip ripping/decrypt/DRM bypass.
- APK siap pakai dari game komersial.

## Mulai cepat

```sh
export ANDROID_HOME=/path/ke/android-sdk
./scripts/env.sh
./scripts/check_repo_clean.sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Hasil: `dist/braid-android-debug.apk` — masih **stub**, belum memuat game.

CI otomatis membangun stub yang sama (JDK 17 + Android SDK 35 + NDK 27 + CMake 3.22).

## Alur recompile (ringkas)

```text
salinan Braid yang sah (lokal, milik Anda)
        ↓
XEX/XBLA → kode C/C++ (recompiler upstream)
        ↓
HLE XDK + renderer + audio + input
        ↓
kompilasi arm64-v8a (NDK) → libbraid.so
        ↓
Braid Android memuat library via BraidNative
        ↓
APK untuk pemakaian sendiri
```

Detail lengkap + kesenjangan yang masih harus dikerjakan: [docs/BRAID-REQUIREMENTS.md](docs/BRAID-REQUIREMENTS.md).

## Status

**Scaffold + stub fungsional.** Target: **Braid (Xbox 360 / XBLA)**.

- ✅ UI interaktif & import metadata lokal
- ✅ JNI stub + kontrak library
- ✅ CI build & guard repo
- ❌ Belum ada recompiler output, HLE XDK, renderer, audio, atau data game

Integrasi native: [docs/INTEGRATION.md](docs/INTEGRATION.md). Target: [docs/targets/braid.md](docs/targets/braid.md).

## Lisensi

Kode di repo ini: [MIT](LICENSE). Game yang Anda pakai tetap tunduk pada lisensi/perjanjian masing-masing.
