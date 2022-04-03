//
// Created by Willie Chang on 9/18/21.
//

/* Header for class com_atelieryl_wonderdroid_WonderSwan */

#ifndef WONDERDROID_X_COM_ATELIERYL_WONDERDROID_WONDERSWAN_H
#define WONDERDROID_X_COM_ATELIERYL_WONDERDROID_WONDERSWAN_H

#include <jni.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
* Class:     com_atelieryl_wonderdroid_WonderSwan
* Method:    load
* Signature: (Ljava/lang/String;Ljava/lang/String;)V
*/
JNIEXPORT jint JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_load
(JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    reset
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_reset
(JNIEnv *, jclass);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    exit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_exit
(JNIEnv *, jclass);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    _execute_frame
 * Signature: (ZLjava/nio/ShortBuffer;[S)I
 */
JNIEXPORT jint JNICALL Java_com_atelieryl_wonderdroid_WonderSwan__1execute_1frame
        (JNIEnv *, jclass, jboolean, jboolean, jobject, jshortArray);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    updatebuttons
 * Signature: (ZZZZZZZZZZZ)V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_updatebuttons
(JNIEnv *, jclass, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean, jboolean);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    savebackup
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_savebackup
(JNIEnv *, jclass, jstring);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    loadbackup
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_loadbackup
(JNIEnv *, jclass, jstring);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    savestate
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_savestate
(JNIEnv *, jclass, jstring);

/*
 * Class:     com_atelieryl_wonderdroid_WonderSwan
 * Method:    loadstate
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_loadstate
(JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libwonderswan",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libwonderswan",__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libwonderswan",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libwonderswan",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libwonderswan",__VA_ARGS__)

#endif //WONDERDROID_X_COM_ATELIERYL_WONDERDROID_WONDERSWAN_H
