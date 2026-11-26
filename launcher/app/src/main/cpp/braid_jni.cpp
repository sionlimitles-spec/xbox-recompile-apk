#include <jni.h>
#include <android/log.h>

#define LOG_TAG "BraidStub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {
constexpr const char* kVersion = "0.2.0-stub";
constexpr const char* kStubError =
        "Runtime stub: libbraid.so hasil recompile belum ditanamkan. "
        "Lihat docs/BRAID-REQUIREMENTS.md.";
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_braid_android_BraidNative_nativeVersion(JNIEnv* env, jclass) {
    return env->NewStringUTF(kVersion);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_braid_android_BraidNative_nativeStart(JNIEnv*, jclass) {
    // Stub sengaja tidak memulai apa pun. Ganti libbraid_stub.so dengan hasil
    // recompile nyata (libbraid.so) untuk mengaktifkan tombol Play.
    LOGI("nativeStart() dipanggil pada stub; tidak ada runtime game.");
    return -1;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_braid_android_BraidNative_nativeLastError(JNIEnv* env, jclass) {
    return env->NewStringUTF(kStubError);
}
