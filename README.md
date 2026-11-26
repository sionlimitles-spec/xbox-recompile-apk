# xbox-recompile-apk

Eksperimen **native recompile** judul Xbox (XBE/XEX) menjadi aplikasi Android, mengikuti pendekatan proyek seperti [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) dan [LibertyRecompAndroid](https://github.com/vaduur/LibertyRecompAndroid).

> ⚠️ **Batasan hukum**: repo ini **tidak berisi** ROM/ISO/XBE/XEX, asset game, binary hasil recompile, ataupun APK game berhak cipta. Repo hanya berisi kode launcher, skrip build, dan dokumentasi. Gunakan hanya salinan game yang sah Anda miliki, dan jangan distribusikan hasil recompile-nya. Lihat [docs/LEGAL.md](docs/LEGAL.md).

## Isi repo

- `launcher/` — stub aplikasi Android (Java) untuk mendeteksi gamepad Xbox via Bluetooth/OTG. Ini "cangkang" tempat kode native hasil recompile nanti dipasang.
- `scripts/` — pengecekan environment, build launcher, dan packaging APK.
- `docs/` — setup tooling, alur recompile, catatan hukum, dan catatan target ([Braid](docs/targets/braid.md)).
- `configs/example.toml` — contoh konfigurasi (placeholder, tanpa path game sungguhan).
- `.github/workflows/build.yml` — CI untuk membangun **stub launcher** (tanpa data game).

## Yang TIDAK ada di sini

- File game, disc image, atau asset dari judul mana pun.
- Skrip untuk membongkar/ripping disc atau melewati DRM.
- APK siap pakai dari game komersial.

## Mulai cepat

```sh
export ANDROID_HOME=/path/ke/android-sdk
./scripts/env.sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Hasilnya: `dist/xbox-recompile-launcher-debug.apk` — masih berupa stub, belum memuat game.

## Alur recompile (ringkas)

Lihat [docs/RECOMP.md](docs/RECOMP.md). Secara garis besar:

```text
salinan game yang sah (lokal)
        ↓
XBE/XEX → kode C/C++ (recompiler upstream)
        ↓
kompilasi ke arm64-v8a (NDK)
        ↓
launcher Android memuat native library
        ↓
APK untuk pemakaian sendiri
```

## Status

Scaffold. Target yang direncanakan: **Braid** (Xbox 360 / XBLA) — lihat [docs/targets/braid.md](docs/targets/braid.md). Belum ada file game atau kode hasil recompile yang ditanamkan di repo.

## Lisensi

Kode di repo ini: [MIT](LICENSE). Game yang Anda pakai tetap tunduk pada lisensi/perjanjian masing-masing.
