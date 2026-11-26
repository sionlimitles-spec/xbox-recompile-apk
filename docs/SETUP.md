# Setup environment

Disarankan memakai host Linux/macOS untuk build Android. Termux cukup untuk menjalankan skrip ringan, tetapi build Gradle/NDK lebih nyaman di desktop.

## Prasyarat

- JDK 17
- Android SDK: platform 35 + build-tools 35.0.0
- Android NDK (opsional, dipakai saat menyusun kode native hasil recompile)
- Gradle 8.x
- Python 3 (untuk skrip utilitas)
- `git`

## Variabel environment

```sh
export ANDROID_HOME=/path/ke/android-sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

## Cek environment

```sh
./scripts/env.sh
```

## Build stub launcher

```sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Output: `dist/xbox-recompile-launcher-debug.apk`.

## Di Termux

```sh
pkg install git python clang cmake ninja openjdk-17 gradle
export ANDROID_HOME=/path/ke/android-sdk
```

Pastikan `gradle` dan `java` terlihat di `PATH` sebelum menjalankan skrip build.
