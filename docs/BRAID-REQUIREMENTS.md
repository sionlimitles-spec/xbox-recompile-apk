# Analisa kebutuhan: Braid → Android ("Braid Android")

> Analisa teknis tingkat tinggi. **Tidak** berisi cara ripping disc, decrypt XEX, bypass DRM, tautan unduhan, atau data game. Semua langkah yang menyentuh file game dilakukan pengguna di perangkatnya sendiri atas salinan sah.

## 1. Kesimpulan singkat

Braid yang dimaksud adalah rilis **Xbox 360 / XBLA (2008)** — arsitektur **PowerPC Xenon**, GPU **Xenos**, bukan Xbox generasi pertama (x86). Karena itu `XboxXBE-Recomp` bukan jalur yang tepat.

Repo ini **bukan** emulator dan **bukan** recompiler. Repo ini adalah **launcher + kerangka integrasi**. Supaya Braid benar-benar jalan di HP, masih dibutuhkan pipeline besar: XEX → C++ → runtime/HLE → renderer → audio → input → NDK → APK. Estimasi realistis: **minggu–bulan**, bukan hitungan jam.

## 2. Keadaan repo sekarang

| Komponen | Status | Catatan |
|---|---|---|
| UI launcher + gamepad | ✅ ada | `MainActivity`, layout scrollable, semua tombol ada listener |
| Import metadata lokal | ✅ ada | SAF + fallback, SHA-256, magic bytes, progres |
| JNI contract + stub | ✅ ada | `BraidNative` + `GameActivity`, `libbraid_stub.so` |
| Build Android/NDK | ✅ ada | AGP 8.6, NDK 27, CMake, `arm64-v8a` |
| CI + repo guard | ✅ ada | `.github/workflows/build.yml`, `check_repo_clean.sh` |
| Recompiler output | ❌ belum | Bergantung pada salinan sah + tool upstream |
| HLE XDK | ❌ belum | Lihat [checklist](targets/braid-xdk-checklist.md) |
| Renderer / audio / input runtime | ❌ belum | Lihat bagian 4 |
| Data game | ❌ tidak ada | Sengaja, dan tidak boleh di-commit |

## 3. Peta komponen yang dibutuhkan

| # | Komponen | Kenapa wajib | Status | Langkah berikutnya |
|---|---|---|---|---|
| 1 | Salinan sah + identifikasi format | Tanpa ini tidak ada yang boleh direcompile | User | Impor via tombol **Import Game**, simpan hash lokal |
| 2 | Ekstraksi XEX dari container XBLA | XBLA biasanya berupa paket `CON`/`STFS`/`LIVE`, bukan `.xex` mentah | User, tool lokal | Verifikasi magic dari hasil import |
| 3 | Static recompiler PowerPC → C++ | CPU Xenon bukan ARM; kode harus diterjemahkan | ❌ | Pakai `XenonRecomp` / `ReXGlue` upstream |
| 4 | Runtime / HLE XDK | Game memanggil API Xbox 360, bukan Linux/Android | ❌ | Lihat bagain 4.3 |
| 5 | Renderer D3D9/Xenos → Vulkan/GLES | Braid engine 2D custom di atas D3D9 | ❌ | Mulai dari renderer null, lalu sprite |
| 6 | Audio + XMA + reverse playback | Mekanik waktu Braid memutar audio terbalik | ❌ | Decoder lokal + sink Android |
| 7 | VFS / asset | Level, tekstur, musik, teks ada di paket game | ❌ | Mapping path ke file pengguna |
| 8 | Timing deterministik | Rewind & time-manipulation sensitif terhadap timestep | ❌ | Fixed timestep, state buffer |
| 9 | Input gamepad + fallback touch | Platformer presisi butuh analog + deadzone | ✅ kerangka | `GameActivity` membersihkan tombol/axis; tinggal map ke XInput HLE |
| 10 | Android platform layer | JNI, Surface lifecycle, audio, SAF, performa | ✅ kerangka | Implementasi native sesuai [INTEGRATION.md](INTEGRATION.md) |
| 11 | Build & packaging | NDK, ABI, signing, tanpa asset di repo | ✅ stub | Tambahkan sumber recompile saat ada |
| 12 | QA per world | Rewind mudah rusak karena float/timing | ❌ | Matriks uji bagain 4.11 |

## 4. Detail komponen

### 4.1 Sumber sah & identifikasi

Format yang mungkin terlihat saat tombol Import Game dipakai:

- `XEX2` / `XEX1` — executable Xbox 360.
- `CON `, `STFS`, `PIRS`, `LIVE` — container/paket XBLA (sering berisi XEX + asset + title update).
- `XBEH` — Xbox original; **bukan target repo ini**.
- ZIP/RAR/7z — arsip, perlu diekstrak dulu oleh pengguna.

Launcher **hanya membaca** metadata: nama, ukuran, SHA-256, 16 byte pertama, dan progres. Tidak menyimpan, tidak mengupload, tidak mendekripsi.

### 4.2 Recompile PowerPC → C++

Kode Xenon bersifat big-endian dan punya register/set instruksi yang berbeda dari ARM. Pendekatan umum proyek recompile:

1. Parse header XEX dan daftar fungsi/import.
2. Terjemahkan instruksi ke C++ portabel (IR → emit).
3. Hasilkan proyek CMake + runtime stub untuk di-build di host PC terlebih dahulu.
4. Verifikasi: logika berjalan dengan renderer null sebelum ke Android.

Tool rujukan: `XenonRecomp` dan `ReXGlue` (hedge-dev). Pilihan tool lain boleh, selama hasilnya C/C++ yang bisa dikompilasi NDK.

> Catatan: ini bukan sekadar "jalankan tool lalu jadi APK". HLE dan renderer biasanya porsi terberat.

### 4.3 Runtime / HLE XDK

Area yang perlu diimplementasikan atau di-stub (prioritas dari [checklist](targets/braid-xdk-checklist.md)):

| Area | Dampak bila belum ada |
|---|---|
| Startup, heap, thread | Game crash sebelum menu |
| File system (`XFile`) | Asset tidak terbaca |
| Input (`XInput`) | Tidak bisa bergerak |
| Audio (`XAudio2`, XMA) | Diam / timing audio rusak |
| Render (`D3D9`) | Tidak ada gambar |
| Waktu/thread tick | Rewind tidak deterministik |
| Save/profile (`XUser`, `XContent`) | Progres tidak tersimpan |
| Font/teks | Narasi world tidak muncul |
| Content/update | Versi XEX berbeda → import berbeda |
| Video, achievement, jaringan | Bisa di-stub untuk pemakaian lokal |

Strategi yang disarankan: **stub dulu yang tidak blocking**, lalu implementasi bertahap berdasarkan log crash nyata.

### 4.4 Renderer

Braid memakai engine 2D custom di atas Direct3D 9 Xbox 360. Kebutuhan minimal:

- upload tekstur + sampling (format swizzle Xenon perlu konversi),
- sprite batch + alpha blend,
- render target untuk efek visual (blur/ghost/echo),
- koordinat & skala ke layar HP (lanskap, aspect ratio berbeda),
- sinkronisasi frame dengan timestep game.

Target backend: Vulkan atau OpenGL ES 3.x via NDK. Mulai dengan **null renderer** supaya logika game bisa divalidasi lebih dulu.

### 4.5 Audio

- Decode XMA/ADPCM ke PCM menggunakan decoder lokal.
- Backend output Android: AAudio atau OpenSL ES (minSdk 21 → OpenSL fallback).
- **Reverse playback** benar-benar dipakai Braid untuk efek manipulasi waktu — jangan dianggap optional.
- Clock audio harus selaras dengan tick game; kalau tidak, rewind dan musik bisa meleset.

### 4.6 VFS & asset

- Petakan path Xbox (case-insensitive, separator backslash) ke filesystem Android.
- Asset berada di paket game milik pengguna; jangan ditanam di APK/repo.
- Perhatikan data big-endian pada struktur game.

### 4.7 Timing & determinisme (paling kritis)

Mekanik Braid menyimpan riwayat state untuk rewind, time shadow, dan waktu yang mengikuti posisi. Risiko:

- floating point host vs Xenon bisa beda,
- timestep tidak tetap → puzzle jadi tidak konsisten,
- state buffer bisa membengkak di HP.

Mitigasi: fixed timestep, simpan state ringkas per tick, uji tiap world dengan replay deterministik.

### 4.8 Input

- Sudah ada: `GameActivity` mengirim state tombol (`A/B/X/Y/LB/RB/DPAD/START/TOUCH`) dan axis X/Y ternormalisasi lewat JNI.
- Masih harus di native: mapping state itu ke `XInputGetState`, deadzone analog & trigger, dan fallback kontrol sentuh yang benar-benar didesain ulang (bukan sekadar tombol virtual).

### 4.9 Android platform layer

| Item | Catatan |
|---|---|
| NDK + ABI | `arm64-v8a`, NDK 27, CMake 3.22 (sudah di CI) |
| JNI | `BraidNative` (start/stop/frame/surface/input/audio), symbol `Java_com_braid_android_BraidNative_*` |
| Library | `libbraid.so` hasil recompile di `jniLibs/arm64-v8a/` (di-ignore git); fallback `libbraid_stub.so` |
| Surface | `GameActivity` + `SurfaceView` + `ANativeWindow`, callback lengkap |
| Audio | AAudio / OpenSL ES |
| Storage | SAF untuk memilih file milik pengguna |
| Lifecycle | onPause/onResume: hentikan tick & audio, jangan boros baterai |
| Termal | Batasi frame rate & resolution di HP |

### 4.10 Build & packaging

- `libbraid.so` hasil recompile **jangan** di-commit (sudah masuk `.gitignore`).
- `scripts/check_repo_clean.sh` memblokir file game/binary terlarang di CI.
- Signing dengan keystore sendiri; APK debug cukup untuk pemakaian pribadi.

### 4.11 QA minimal sebelum disebut "jalan"

- Boot sampai menu tanpa renderer nyata (renderer null).
- Satu world penuh dengan sprite & audio.
- Rewind berulang di world dengan time-shadow.
- Gamepad Bluetooth + OTG, landscape 360–720dp tinggi.
- 30 menit main: tidak crash, tidak overheat,Performa stabil.

## 5. Milestone & kriteria penerimaan

| Milestone | Kriteria |
|---|---|
| M0 ✅ | APK stub build di CI, tombol merespons, import metadata jalan, layar game + loop frame + input tersedia |
| M1 | Source C++ hasil recompile build di host PC |
| M2 | Boot dengan renderer null, log tidak error fatal |
| M3 | HLE file/input/waktu → menu Braid tampil |
| M4 | Renderer Vulkan/GES → sprite & level tampil |
| M5 | Audio + reverse playback sinkron |
| M6 | `libbraid.so` jalan di HP via `GameActivity` + `BraidNative.start()` |
| M7 | Rewind deterministik + uji performa lintas world |

## 6. Risiko

| Risiko | Mitigasi |
|---|---|
| Legal/EULA | Hanya salinan sah, pemakaian sendiri, tidak distribusi |
| Impor XDK banyak & belum ter-cover | Stub bertahap + log crash sebagai panduan |
| Renderer Xenos kompleks | Null renderer dulu, lalu inkremental |
| Rewind tidak deterministik | Fixed timestep + replay test tiap world |
| Performa HP menengah | Batasi resolusi/frame, profil sejak awal |

## 7. Langkah berikutnya di repo ini

1. Ganti `libbraid_stub.so` dengan `libbraid.so` hasil recompile Anda (lokal).
2. Implementasikan semua symbol JNI di [INTEGRATION.md](INTEGRATION.md) di library Anda.
3. Isi renderer di `nativeSurfaceCreated/Changed/Frame` saat HLE dasar sudah boot.
4. Isi `analysis/` (di-ignore git) dengan hasil import tiap salinan sah untuk perbandingan versi.

## 8. Yang tidak disediakan repo ini

- Sumber ISO/XEX/ROM, tautan unduhan, atau nama situs penyedia game.
- Skrip ripping disc, decrypt XEX, atau bypass DRM/lisensi.
- File asset, audio, teks, atau kode dari Braid.

Lihat [LEGAL.md](LEGAL.md). Kalau Anda tidak memiliki salinan sah, repo ini tidak bisa dipakai untuk target Braid.

## 9. Rujukan

- https://en.wikipedia.org/wiki/Braid_(video_game)
- https://github.com/hedge-dev/XenonRecomp
- https://github.com/hedge-dev/ReXGlue
- https://github.com/sp00nznet/ps3recomp/blob/master/docs/GAME_PORTING_GUIDE.md
