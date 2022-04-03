//
// Created by Willie Chang on 9/18/21.
//

#include "mednafen/mednafen.h"
#include "mednafen/settings-driver.h"
#include "mednafen/state-driver.h"
#include "mednafen/mednafen-driver.h"

#include "com_atelieryl_wonderdroid_WonderSwan.h"

using namespace Mednafen;

bool initialized = false;
bool run_game = false;

static MDFNGI *game;
static MDFN_Surface *surf;

double _sampleRate = 48000;
uint32_t *_inputBuffer[13];

extern "C" {

    JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_exit(JNIEnv *env, jclass obj) {
        LOGD("Called exit!");
        run_game = false;
        MDFNI_CloseGame();
        delete surf;
    }

    JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_reset(JNIEnv *env, jclass obj) {
        LOGD("Called reset!");
        MDFNI_Reset();
    }

    JNIEXPORT jint JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_load(JNIEnv *env, jclass obj, jstring rom_path,
                                                   jstring dir_path) {
        LOGD("Called load!");

        // Initialize Mednafen
        if (!initialized) {
            MDFNI_InitializeModules();
            std::vector<MDFNSetting> settings;
            MDFNI_Initialize(env->GetStringUTFChars(dir_path, NULL), settings);
            MDFNI_SetSetting("filesys.path_sav", env->GetStringUTFChars(dir_path, NULL));
            initialized = true;
        }

        // Load ROM
        game = MDFNI_LoadGame(NULL, &::Mednafen::NVFS, env->GetStringUTFChars(rom_path, NULL));
        if (!game) return 1;

        // Set up input
        for (unsigned i = 0; i < 13; i++)
            _inputBuffer[i] = (uint32_t *) calloc(9, sizeof(uint32_t));
        game->SetInput(0, "gamepad", (uint8_t *)_inputBuffer[0]);

        // Set up surface -- might move to execute_frame
        MDFN_PixelFormat pix_fmt(MDFN_COLORSPACE_RGB, 0, 8, 16, 24);
        surf = new MDFN_Surface(NULL, game->fb_width, game->fb_height, game->fb_width, pix_fmt);

        run_game = true;
        return 0;
    }

    JNIEXPORT jint JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan__1execute_1frame(JNIEnv *env, jclass obj, jboolean skip,
                                                               jboolean audio, jobject framebuffer,
                                                               jshortArray audiobuffer) {
        LOGD("Called execute frame!");

        if (!run_game) return 0;

        // Mednafen emulate
        static int16_t sound_buf[0x10000];
        int32 rects[game->fb_height];

        memset(rects, 0, game->fb_height*sizeof(int32));
        rects[0] = ~0;
        EmulateSpecStruct spec;
        spec.surface = surf;
        spec.SoundRate = _sampleRate;
        spec.SoundBuf = sound_buf;
        spec.LineWidths = rects;
        spec.SoundBufMaxSize = sizeof(sound_buf) / 2;
        spec.SoundVolume = 1.0;
        spec.soundmultiplier = 1.0;

        MDFNI_Emulate(&spec);

        // Copy framebuffer
        uint32_t* fb = (uint32_t*) env->GetDirectBufferAddress(framebuffer);
        memcpy(fb, surf->pixels, game->fb_width * game->fb_height * sizeof(uint32_t));

        // Audio


        return 0;
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