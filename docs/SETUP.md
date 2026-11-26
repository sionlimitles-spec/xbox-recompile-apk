# Setup environment

Disarankan memakai host Linux/macOS untuk build Android. Termux cukup untuk skrip ringan, tetapi build Gradle/NDK lebih nyaman di desktop.

## Prasyarat

- JDK 17
- Android SDK: platform 35 + build-tools 35.0.0
- Android NDK **27.0.12077973** + CMake **3.22.1** (dipakai stub native)
- Gradle **8.7+**
- Python 3 (untuk skrip utilitas)
- `git`

Pasang NDK/CMake lewat sdkmanager:

```sh
sdkmanager "ndk;27.0.12077973" "cmake;3.22.1"
```

## Variabel environment

```sh
export ANDROID_HOME=/path/ke/android-sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

## Cek environment & repo

```sh
./scripts/env.sh
./scripts/check_repo_clean.sh
```

`check_repo_clean.sh` memastikan tidak ada file game / binary hasil build yang ikut ter-commit.

## Build Braid Android

```sh
./scripts/build_launcher.sh
./scripts/package_apk.sh
```

Output: `dist/braid-android-debug.apk`.

## Di Termux

```sh
pkg install git python clang cmake ninja openjdk-17 gradle
export ANDROID_HOME=/path/ke/android-sdk
```

Pastikan `gradle` dan `java` terlihat di `PATH` sebelum menjalankan skrip build. NDK tetap harus tersedia di `$ANDROID_HOME/ndk/27.0.12077973`; kalau belum ada, AGP akan mencoba mengunduhnya saat build pertama.
