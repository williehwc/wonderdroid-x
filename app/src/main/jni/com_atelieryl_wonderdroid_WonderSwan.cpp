//
// Created by Willie Chang on 9/18/21.
//

#include "com_atelieryl_wonderdroid_WonderSwan.h"

extern "C" {

    JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_reset(JNIEnv *env, jclass obj) {
        LOGD("Called reset!");
    }

    JNIEXPORT void JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_load(JNIEnv *env, jclass obj, jstring filename,
                                                   jboolean iswsc, jstring name, jint year, jint month,
                                                   jint day, jint blood, jint sex) {
        LOGD("Called load!");
    }

    JNIEXPORT jint JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan__1execute_1frame(JNIEnv *env, jclass obj, jboolean skip,
                                                               jboolean audio, jobject framebuffer,
                                                               jshortArray audiobuffer) {
        LOGD("Called execute frame!");
    }

    JNIEXPORT void JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_updatebuttons(JNIEnv *env, jclass obj, jboolean y1,
                                                            jboolean y2, jboolean y3, jboolean y4,
                                                            jboolean x1, jboolean x2, jboolean x3,
                                                            jboolean x4, jboolean a, jboolean b,
                                                            jboolean start) {}

    JNIEXPORT void JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_loadbackup(JNIEnv *env, jclass obj, jstring filename) {}

    JNIEXPORT void JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_savebackup(JNIEnv *env, jclass obj, jstring filename) {}

    JNIEXPORT void JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_loadstate(JNIEnv *env, jclass obj, jstring filename) {}

    JNIEXPORT void JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_savestate(JNIEnv *env, jclass obj, jstring filename) {}

}