package com.braid.android;

import android.view.Surface;

/**
 * Jembatan ke runtime native Braid.
 *
 * Urutan pemuatan:
 *   1. libbraid.so      — hasil recompile Anda (lokal, di jniLibs/, tidak di-commit)
 *   2. libbraid_stub.so — stub bawaan repo, supaya launcher tetap bisa diuji
 *
 * Kontrak JNI lengkap ada di docs/INTEGRATION.md.
 */
public final class BraidNative {

    private static boolean attempted = false;
    private static boolean loaded = false;
    private static boolean stub = false;
    private static String version = "?";
    private static String loadError = "";

    private BraidNative() {
    }

    public static synchronized boolean load() {
        if (attempted) {
            return loaded;
        }
        attempted = true;

        try {
            System.loadLibrary("braid");
            loaded = true;
            stub = false;
        } catch (UnsatisfiedLinkError realMissing) {
            try {
                System.loadLibrary("braid_stub");
                loaded = true;
                stub = true;
            } catch (UnsatisfiedLinkError stubMissing) {
                loaded = false;
                stub = false;
                loadError = stubMissing.getMessage();
            }
        }

        if (loaded) {
            try {
                version = nativeVersion();
            } catch (Throwable error) {
                version = "?";
            }
        }
        return loaded;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isStub() {
        return stub;
    }

    public static String version() {
        return version;
    }

    public static String loadError() {
        return loadError;
    }

    // ===== Siklus runtime =====

    /** Return 0 bila runtime berhasil start; nonzero untuk error. */
    public static int start() {
        return loaded ? nativeStart() : -1;
    }

    public static void stop() {
        if (loaded) {
            nativeStop();
        }
    }

    public static String lastError() {
        try {
            return nativeLastError();
        } catch (Throwable error) {
            return String.valueOf(error);
        }
    }

    // ===== Render =====

    public static void surfaceCreated(Surface surface) {
        if (loaded) {
            nativeSurfaceCreated(surface);
        }
    }

    public static void surfaceChanged(int width, int height) {
        if (loaded) {
            nativeSurfaceChanged(width, height);
        }
    }

    public static void surfaceDestroyed() {
        if (loaded) {
            nativeSurfaceDestroyed();
        }
    }

    /** Dipanggil loop 60 fps oleh GameActivity. */
    public static void frame(long nowMs) {
        if (loaded) {
            nativeFrame(nowMs);
        }
    }

    // ===== Input =====

    public static void setButton(int button, boolean down) {
        if (loaded) {
            nativeSetButton(button, down);
        }
    }

    public static void setAxis(int axis, float value) {
        if (loaded) {
            nativeSetAxis(axis, value);
        }
    }

    // ===== Audio =====

    public static void audioInit() {
        if (loaded) {
            nativeAudioInit();
        }
    }

    public static void audioShutdown() {
        if (loaded) {
            nativeAudioShutdown();
        }
    }

    private static native int nativeStart();

    private static native void nativeStop();

    private static native String nativeVersion();

    private static native String nativeLastError();

    private static native void nativeSurfaceCreated(Surface surface);

    private static native void nativeSurfaceChanged(int width, int height);

    private static native void nativeSurfaceDestroyed();

    private static native void nativeFrame(long nowMs);

    private static native void nativeSetButton(int button, boolean down);

    private static native void nativeSetAxis(int axis, float value);

    private static native void nativeAudioInit();

    private static native void nativeAudioShutdown();
}
