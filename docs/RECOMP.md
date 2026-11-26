# Alur recompile (ringkas)

Dokumen ini hanya menjelaskan alur secara konseptual. Repo ini **tidak** menyediakan file game atau skrip ripping disc.

## 1. Tentukan target

- **Original Xbox**: kode x86, biasanya lewat toolchain seperti `XboxXBE-Recomp`.
- **Xbox 360**: kode PowerPC, biasanya lewat proyek decomp/recomp spesifik judul.

Setiap judul punya tingkat kesulitan berbeda. Judul dengan ketergantungan SDK/XDK yang berat lebih sulit dipindahkan.

## 2. Siapkan salinan game yang sah

Anda harus memiliki salinan game secara legal. Jangan memasukkan file tersebut ke repo, dan jangan membagikannya.

## 3. Recompile ke C/C++

Gunakan recompiler upstream yang sesuai target. Hasilnya berupa source C/C++ yang kemudian dikompilasi dengan Android NDK untuk ABI `arm64-v8a`.

## 4. Pasang ke launcher

Stub launcher di `launcher/` menyediakan Activity dasar dan deteksi gamepad. Kode native hasil recompile dapat ditempatkan secara lokal di:

```text
launcher/app/src/main/jniLibs/arm64-v8a/
```

Direktori ini sengaja diabaikan oleh `.gitignore` agar tidak ikut ter-commit.

## 5. Build & tanda tangani

```sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Untuk distribusi ke perangkat Anda sendiri, APK debug sudah cukup. Untuk rilis, tanda tangani dengan keystore Anda sendiri (jangan commit keystore).

## Pengingat

Lihat [LEGAL.md](LEGAL.md). Jangan distribute game data atau APK hasil recompile.
