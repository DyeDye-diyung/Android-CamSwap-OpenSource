#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

// Forwards the camera's buffer transform to an EGL-produced output, so the app sees the
// same orientation metadata as when the camera writes to its Surface directly.
extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_zensu357_camswap_SwitchablePreview_nativeSetBuffersTransform(
        JNIEnv* env, jclass /*clazz*/, jobject surface, jint transform) {
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (window == nullptr) return JNI_FALSE;
    int32_t result = ANativeWindow_setBuffersTransform(window, transform);
    ANativeWindow_release(window);
    return result == 0 ? JNI_TRUE : JNI_FALSE;
}
