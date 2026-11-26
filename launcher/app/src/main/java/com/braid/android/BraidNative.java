package com.braid.android;

/**
 * Jembatan ke runtime native Braid.
 *
 * Urutan pemuatan:
 *   1. libbraid.so      — hasil recompile Anda (lokal, di jniLibs/, tidak di-commit)
 *   2. libbraid_stub.so — stub bawaan repo, supaya launcher tetap bisa diuji
 *
 * Simbol JNI yang harus diekspor oleh libbraid.so hasil recompile:
 *   Java_com_braid_android_BraidNative_nativeStart
 *   Java_com_braid_android_BraidNative_nativeVersion
 *   Java_com_braid_android_BraidNative_nativeLastError
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

    /** Return 0 bila runtime berhasil start; nonzero untuk error. */
    public static int start() {
        return nativeStart();
    }

    public static String lastError() {
        try {
            return nativeLastError();
        } catch (Throwable error) {
            return String.valueOf(error);
        }
    }

    private static native int nativeStart();

    private static native String nativeVersion();

    private static native String nativeLastError();
}
