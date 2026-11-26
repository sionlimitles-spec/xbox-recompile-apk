# Integrasi native library ke launcher (Tahap 3)

Stub launcher sudah mencoba memuat `libbraid.so` saat Activity dibuat. Jika library belum ada, aplikasi tetap tampil dan menunjukkan status "belum ada".

## Struktur lokal (tidak di-commit)

```text
launcher/app/src/main/jniLibs/arm64-v8a/libbraid.so
```

Folder `jniLibs/` sengaja masuk `.gitignore`. Taruh file hasil build Anda di sana **hanya di mesin sendiri**.

## Kontrak JNI minimal

Activity mendeklarasikan:

```java
private static native int nativeStart();
```

Simbol C/C++ yang diharapkan:

```c
JNIEXPORT jint JNICALL
Java_com_example_xboxrecompile_MainActivity_nativeStart(JNIEnv *env, jclass clazz);
```

Return `0` bila start berhasil, nonzero untuk error. Nama library yang dimuat: `braid` (dari `System.loadLibrary("braid")`).

## Alur yang diharapkan

1. `libbraid.so` berisi kode hasil recompile + runtime/HLE (lihat [braid-xdk-checklist.md](targets/braid-xdk-checklist.md)).
2. Launcher memuat library saat `onCreate`.
3. Entry point `nativeStart()` dijalankan setelah renderer/input/audio siap.
4. Activity menampilkan status gamepad dan status library.

## Yang belum ada

- Implementasi `nativeStart()` di repo ini (sengaja, karena bergantung pada hasil recompile Anda).
- Render surface Android (SurfaceView/GL surface) untuk menggambar frame game.
- Pemetaan event input Android/XInput ke state game.

Bagian tersebut menyusul setelah toolchain recompile Anda menghasilkan library yang bisa dijalankan.

## Pengingat

Lihat [LEGAL.md](LEGAL.md). Jangan commit library, asset, atau file game ke repo publik ini.
