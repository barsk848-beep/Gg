#include <jni.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/uio.h>
#include <fcntl.h>
#include <cstdint>
#include <cstring>
#include <vector>

namespace {
    bool aimEnabled = false;
    bool espEnabled = false;
    bool noRecoilEnabled = false;

    float aimFov = 90.0f;
    float aimSmooth = 5.0f;
    float recoilMultiplier = 0.0f;

    bool hasRoot() {
        return access("/system/xbin/su", F_OK) == 0 ||
               access("/system/bin/su", F_OK) == 0 ||
               access("/sbin/su", F_OK) == 0;
    }

    // Generic remote memory write helper. Requires root + a valid target pid.
    // Standoff 2 offsets are intentionally left as named constants to fill
    // after dumping the current build with a memory scanner.
    bool writeRemoteMemory(pid_t pid, std::uintptr_t address, const void* data, size_t size) {
        struct iovec local { .iov_base = const_cast<void*>(data), .iov_len = size };
        struct iovec remote { .iov_base = reinterpret_cast<void*>(address), .iov_len = size };
        ssize_t written = process_vm_writev(pid, &local, 1, &remote, 1, 0);
        return written == static_cast<ssize_t>(size);
    }

    // Placeholder offsets for the current Standoff 2 build.
    // Dump these with Cheat Engine / GameGuardian and update here.
    constexpr std::uintptr_t OFF_RECOIL_STRUCT = 0x0;   // TODO: fill
    constexpr std::uintptr_t OFF_AIM_TARGET = 0x0;      // TODO: fill
    constexpr std::uintptr_t OFF_ESP_VISIBILITY = 0x0;  // TODO: fill
}

extern "C" JNIEXPORT void JNICALL
Java_com_ollie_standoff2cheat_CheatNative_setFeature(JNIEnv*, jclass, jint feature, jboolean enabled) {
    switch (feature) {
        case 0: aimEnabled = enabled == JNI_TRUE; break;
        case 1: espEnabled = enabled == JNI_TRUE; break;
        case 2: noRecoilEnabled = enabled == JNI_TRUE; break;
        default: break;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_ollie_standoff2cheat_CheatNative_setFloat(JNIEnv*, jclass, jint feature, jfloat value) {
    switch (feature) {
        case 10: aimFov = value; break;
        case 11: aimSmooth = value; break;
        case 12: recoilMultiplier = value; break;
        default: break;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ollie_standoff2cheat_CheatNative_isRooted(JNIEnv*, jclass) {
    return hasRoot() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ollie_standoff2cheat_CheatNative_patchGame(JNIEnv*, jclass, jboolean enabled) {
    if (!hasRoot()) return JNI_FALSE;

    // Find Standoff 2 pid. In production, cache this and refresh on process death.
    // Kept as a documented hook point; actual pid enumeration is done via /proc.
    // The real patching calls would look like:
    //   writeRemoteMemory(pid, OFF_RECOIL_STRUCT, &recoilMultiplier, sizeof(float));
    // Fill offsets first, then enable the writes below.
    (void)enabled;
    (void)writeRemoteMemory;
    return JNI_TRUE;
}
