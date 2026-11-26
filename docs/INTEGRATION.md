# Integrasi native library ke Braid Android

Launcher memuat library native saat `Activity` dibuat. Urutannya sengaja dibuat **fallback** supaya aplikasi tetap bisa diuji tanpa runtime game.

## Urutan pemuatan (`BraidNative.load()`)

1. `libbraid.so` — **hasil recompile Anda**, ditaruh lokal di:
   ```text
   launcher/app/src/main/jniLibs/arm64-v8a/libbraid.so
   ```
   Folder `jniLibs/` sudah di-ignore git.
2. `libbraid_stub.so` — stub bawaan repo, dibangun dari `launcher/app/src/main/cpp/`.
3. Kalau keduanya gagal dimuat, launcher tetap tampil dan statusnya "BELUM SIAP".

Memisahkan nama `libbraid.so` dan `libbraid_stub.so` mencegah bentrok saat Anda menaruh library hasil recompile (stub tidak menimpa library Anda).

## Kontrak JNI

Deklarasi di Java (`com.braid.android.BraidNative`):

```java
private static native int nativeStart();
private static native String nativeVersion();
private static native String nativeLastError();
```

Simbol yang harus diekspor oleh `libbraid.so` Anda:

```c
JNIEXPORT jint JNICALL
Java_com_braid_android_BraidNative_nativeStart(JNIEnv *env, jclass clazz);

JNIEXPORT jstring JNICALL
Java_com_braid_android_BraidNative_nativeVersion(JNIEnv *env, jclass clazz);

JNIEXPORT jstring JNICALL
Java_com_braid_android_BraidNative_nativeLastError(JNIEnv *env, jclass clazz);
```

Kontrak:

| Fungsi | Return | Arti |
|---|---|---|
| `nativeStart` | `0` | runtime berhasil dimulai |
| `nativeStart` | nonzero | gagal; detail dari `nativeLastError()` |
| `nativeVersion` | string | versi runtime, misalnya `0.3.0-braid` |
| `nativeLastError` | string | pesan error terakhir |

Package sebelumnya `com.example.xboxrecompile` sudah **tidak dipakai**. Kalau Anda punya library lama, export ulang symbol di atas atau tambahkan wrapper dengan nama baru.

## Perilaku stub

`libbraid_stub.so` hanya mencatat log dan mengembalikan `-1` dari `nativeStart()`. Tombol **Play** akan menampilkan dialog penjelasan, bukan mengklaim game berjalan. Ini disengaja agar status aplikasi jujur.

## Build

```sh
export ANDROID_HOME=/path/ke/android-sdk
./scripts/env.sh
./scripts/check_repo_clean.sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Output: `dist/braid-android-debug.apk`.

Gradle akan membangun `libbraid_stub.so` via CMake (`src/main/cpp/CMakeLists.txt`) untuk ABI `arm64-v8a` dengan NDK 27.0.12077973.

## Alur integrasi runtime nyata

1. Hasilkan C++ dari XEX milik Anda dengan recompiler upstream.
2. Tambahkan runtime/HLE, renderer, audio, dan input — lihat [BRAID-REQUIREMENTS.md](BRAID-REQUIREMENTS.md).
3. Build dengan NDK menjadi `libbraid.so` (`arm64-v8a`).
4. Taruh di `jniLibs/arm64-v8a/` (lokal, jangan di-commit).
5. Pastikan tiga symbol JNI di atas diekspor.
6. Jalankan APK, buka tombol **Diagnostik** (ikon kanan atas) untuk memastikan library terbaca sebagai "siap", bukan "stub".

## Yang masih belum ada

- Implementasi `nativeStart()` nyata (sengaja bergantung pada hasil recompile Anda).
- `SurfaceView` + renderer Vulkan/GLES.
- Mapping event input Android ke state XInput.
- Backend audio AAudio/OpenSL ES.

## Pengingat

Lihat [LEGAL.md](LEGAL.md). Jangan commit library, asset, atau file game ke repo publik ini.
