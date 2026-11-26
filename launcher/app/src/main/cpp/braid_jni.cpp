#include <jni.h>

#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOG_TAG "BraidStub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {

constexpr const char* kVersion = "0.2.0-stub";
constexpr const char* kStubError =
        "Runtime stub: libbraid.so hasil recompile belum ditanamkan. "
        "Lihat docs/BRAID-REQUIREMENTS.md.";

ANativeWindow* g_window = nullptr;
bool g_logged_frame = false;
bool g_logged_audio = false;

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_braid_android_BraidNative_nativeVersion(JNIEnv* env, jclass) {
    return env->NewStringUTF(kVersion);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_braid_android_BraidNative_nativeLastError(JNIEnv* env, jclass) {
    return env->NewStringUTF(kStubError);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_braid_android_BraidNative_nativeStart(JNIEnv*, jclass) {
    // Stub sengaja tidak memulai game. Tombol Play di launcher sudah
    // menolak masuk ke GameActivity saat library masih stub.
    LOGI("nativeStart() dipanggil pada stub; tidak ada runtime game.");
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeStop(JNIEnv*, jclass) {
    LOGI("nativeStop() stub.");
}

// ===== Render =====

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeSurfaceCreated(JNIEnv* env, jclass, jobject surface) {
    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
    g_window = ANativeWindow_fromSurface(env, surface);
    LOGI("Surface dibuat: %p", g_window);
}

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeSurfaceChanged(JNIEnv*, jclass, jint width, jint height) {
    if (g_window != nullptr) {
        ANativeWindow_setBuffersGeometry(g_window, width, height, WINDOW_FORMAT_RGBA_8888);
    }
    LOGI("Surface diubah: %dx%d", width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeSurfaceDestroyed(JNIEnv*, jclass) {
    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
    LOGI("Surface dihancurkan.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeFrame(JNIEnv*, jclass, jlong) {
    if (!g_logged_frame) {
        LOGI("Loop frame stub berjalan (tidak ada frame digambar).");
        g_logged_frame = true;
    }
}

// ===== Input =====

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeSetButton(JNIEnv*, jclass, jint button, jboolean down) {
    (void)button;
    (void)down;
}

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeSetAxis(JNIEnv*, jclass, jint axis, jfloat value) {
    (void)axis;
    (void)value;
}

// ===== Audio =====

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeAudioInit(JNIEnv*, jclass) {
    if (!g_logged_audio) {
        LOGI("Audio stub: tidak ada backend yang diinisialisasi.");
        g_logged_audio = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_braid_android_BraidNative_nativeAudioShutdown(JNIEnv*, jclass) {
    // Stub: tidak ada yang dimatikan.
}
