# Integrasi runtime native ke Braid Android

Launcher sekarang punya **layar game penuh** (`GameActivity`) dengan `SurfaceView`, loop frame 60 fps, input gamepad/sentuh, dan siklus audio. Semua itu memanggil JNI di `BraidNative`. Yang belum ada cuma **implementasi native-nya** — itulah yang harus diisi dari hasil recompile Anda.

## Urutan pemuatan library

`BraidNative.load()`:

1. `libbraid.so` — hasil recompile Anda, lokal di `launcher/app/src/main/jniLibs/arm64-v8a/libbraid.so` (di-ignore git).
2. `libbraid_stub.so` — stub bawaan repo (`launcher/app/src/main/cpp/`), fallback supaya launcher tetap bisa diuji.
3. Kalau keduanya gagal, status "BELUM SIAP".

Nama beda supaya stub tidak menimpa library Anda.

## Alur saat tombol Play

```text
MainActivity.onPlay()
   ├─ library belum ada / masih stub → dialog penjelasan, tidak masuk game
   └─ libbraid.so nyata → GameActivity
          ├─ BraidNative.start()            (0 = sukses)
          ├─ surfaceCreated → nativeSurfaceCreated(Surface)
          ├─ surfaceChanged → nativeSurfaceChanged(w, h)
          ├─ loop 60 fps  → nativeFrame(nowMs)
          ├─ input        → nativeSetButton / nativeSetAxis
          ├─ onResume     → nativeAudioInit
          ├─ onPause      → nativeAudioShutdown
          └─ onDestroy    → nativeStop + nativeSurfaceDestroyed
```

## Kontrak JNI

Semua fungsi memakai prefix `Java_com_braid_android_BraidNative_` + nama method di bawah.

| Method Java | Tanda tangan C | Catatan |
|---|---|---|
| `nativeStart` | `jint (JNIEnv*, jclass)` | `0` = sukses; nonzero = error |
| `nativeStop` | `void (JNIEnv*, jclass)` | hentikan runtime |
| `nativeVersion` | `jstring (JNIEnv*, jclass)` | versi runtime |
| `nativeLastError` | `jstring (JNIEnv*, jclass)` | pesan error terakhir |
| `nativeSurfaceCreated` | `void (JNIEnv*, jclass, jobject surface)` | ambil `ANativeWindow` via `ANativeWindow_fromSurface` |
| `nativeSurfaceChanged` | `void (JNIEnv*, jclass, jint w, jint h)` | `ANativeWindow_setBuffersGeometry` |
| `nativeSurfaceDestroyed` | `void (JNIEnv*, jclass)` | `ANativeWindow_release` |
| `nativeFrame` | `void (JNIEnv*, jclass, jlong nowMs)` | gambar satu frame ke surface |
| `nativeSetButton` | `void (JNIEnv*, jclass, jint button, jboolean down)` | state tombol |
| `nativeSetAxis` | `void (JNIEnv*, jclass, jint axis, jfloat value)` | axis `-1..1` |
| `nativeAudioInit` | `void (JNIEnv*, jclass)` | siapkan backend audio |
| `nativeAudioShutdown` | `void (JNIEnv*, jclass)` | matikan audio |

### Indeks tombol (`GameActivity`)

`A=0, B=1, X=2, Y=3, LB=4, RB=5, UP=6, DOWN=7, LEFT=8, RIGHT=9, START=10, TOUCH=11`

Tombol fisik dipetakan dari `KEYCODE_BUTTON_*` / DPAD. Sentuh layar mengisi `axis 0` (X) dan `axis 1` (Y) ternormalisasi, plus `TOUCH` ditekan. Mapping ini sengaja menyerupai `XInputGetState` supaya HLE tidak perlu tahu soal Android.

### Audio

`nativeAudioInit`/`nativeAudioShutdown` adalah tempat memasang AAudio (API 26+) atau OpenSL ES (API 21+). Decode XMA dan **reverse playback** tetap menjadi tanggung jawab runtime native — lihat [BRAID-REQUIREMENTS.md](BRAID-REQUIREMENTS.md).

## Perilaku stub

`libbraid_stub.so` mengimplementasikan semua symbol di atas sebagai no-op; `nativeStart()` mengembalikan `-1`. Karena itu `GameActivity` **tidak pernah terbuka** saat library masih stub — launcher terus terang memberi tahu pengguna.

## Build

```sh
export ANDROID_HOME=/path/ke/android-sdk
./scripts/env.sh
./scripts/check_repo_clean.sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

CMake (`src/main/cpp/CMakeLists.txt`) membangun `libbraid_stub.so` untuk `arm64-v8a`, menautkan `log` dan `android` (dibutuhkan `ANativeWindow`).

## Memasang library hasil recompile

1. Bangun `libbraid.so` (`arm64-v8a`) dari source hasil recompiler + HLE + renderer.
2. Export semua symbol JNI di atas (boleh tambah `extern "C"` di file JNI Anda).
3. Taruh di `launcher/app/src/main/jniLibs/arm64-v8a/libbraid.so` **(lokal, jangan di-commit)**.
4. Build & install. Buka **Diagnostik** di launcher — status native harus terbaca "siap", bukan "stub".
5. Tekan **Play**; kalau `nativeStart()` mengembalikan error, pesan dari `nativeLastError()` muncul.

## Yang masih harus ditulis di sisi native

- Implementasi `nativeStart`/`nativeStop` + state game.
- Renderer Vulkan/GLES ke `ANativeWindow`.
- Map `nativeSetButton`/`nativeSetAxis` ke state XInput HLE.
- Backend audio + XMA + reverse playback.
- VFS asset dari salinan sah pengguna.

Sisi Java sudah siap; tidak perlu diubah kecuali Anda menambah kontrol sentuh khusus.

## Pengingat

Lihat [LEGAL.md](LEGAL.md). Jangan commit library, asset, atau file game ke repo publik ini.
