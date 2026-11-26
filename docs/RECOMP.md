# Alur recompile (ringkas)

Dokumen ini hanya menjelaskan alur secara konseptual. Repo ini **tidak** menyediakan file game atau skrip ripping disc.

## 1. Tentukan target

- **Original Xbox**: kode x86, toolchain seperti `XboxXBE-Recomp`. **Bukan target repo ini.**
- **Xbox 360 / XBLA**: kode PowerPC Xenon. Target **Braid Android** adalah yang ini.

Analisa kebutuhan lengkap: [BRAID-REQUIREMENTS.md](BRAID-REQUIREMENTS.md).

## 2. Siapkan salinan game yang sah

Anda harus memiliki salinan game secara legal. Jangan memasukkan file tersebut ke repo, dan jangan membagikannya. Gunakan tombol **Import Game** di launcher untuk memverifikasi identitas file secara lokal (hash + magic bytes).

## 3. Recompile ke C/C++

Gunakan recompiler upstream yang sesuai target (misal `XenonRecomp`/`ReXGlue` untuk Xenon). Hasilnya berupa source C/C++ yang kemudian dikompilasi dengan Android NDK untuk ABI `arm64-v8a`.

## 4. Lengkapi runtime

Kode hasil recompile belum cukup: dibutuhkan HLE XDK (file, input, audio, waktu), renderer, dan VFS asset. Lihat [checklist XDK](targets/braid-xdk-checklist.md) dan [INTEGRATION.md](INTEGRATION.md).

## 5. Pasang ke launcher

Taruh library hasil build Anda secara lokal di:

```text
launcher/app/src/main/jniLibs/arm64-v8a/libbraid.so
```

Direktori ini sengaja diabaikan oleh `.gitignore`. Repo menyediakan `libbraid_stub.so` sebagai fallback; keduanya tidak bentrok karena beda nama.

## 6. Build & tanda tangani

```sh
export ANDROID_HOME=/path/ke/android-sdk
./scripts/env.sh
./scripts/check_repo_clean.sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Output: `dist/braid-android-debug.apk`. Untuk distribusi ke perangkat sendiri, APK debug sudah cukup. Untuk rilis, tanda tangani dengan keystore sendiri (jangan commit keystore).

## Pengingat

Lihat [LEGAL.md](LEGAL.md). Jangan distribute game data atau APK hasil recompile.
