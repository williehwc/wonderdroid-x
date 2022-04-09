//
// Created by Willie Chang on 9/18/21.
//

#include "mednafen/mednafen.h"
#include "mednafen/settings-driver.h"
#include "mednafen/state-driver.h"
#include "mednafen/mednafen-driver.h"

#include "com_atelieryl_wonderdroid_WonderSwan.h"

using namespace Mednafen;

const int GAME_INFO_ARRAY_SIZE = 7;
const int FRAME_INFO_ARRAY_SIZE = 3;
const int SOUND_BUF_SIZE = 4096;

bool _initialized = false;
bool _runGame = false;

static MDFNGI *_game;
static MDFN_Surface *_surf;

double _sampleRate = 24000;
uint32_t *_inputBuffer[13];

extern "C" {

    JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_exit(JNIEnv *env, jclass obj) {
        LOGD("Called exit!");
        _runGame = false;
        MDFNI_CloseGame();
        delete _surf;
    }

    JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_reset(JNIEnv *env, jclass obj) {
        LOGD("Called reset!");
        MDFNI_Reset();
    }

    JNIEXPORT jshortArray JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan_load(JNIEnv *env, jclass obj, jstring rom_path,
                                                   jstring dir_path) {
        LOGD("Called load!");

        // Initialize Mednafen
        if (!_initialized) {
            MDFNI_InitializeModules();
            std::vector<MDFNSetting> settings;
            MDFNI_Initialize(env->GetStringUTFChars(dir_path, NULL), settings);
            MDFNI_SetSetting("filesys.path_sav", env->GetStringUTFChars(dir_path, NULL));
            _initialized = true;
        }

        // Load ROM
        _game = MDFNI_LoadGame(NULL, &::Mednafen::NVFS, env->GetStringUTFChars(rom_path, NULL));
        if (!_game) return NULL;

        // Set up input
        for (unsigned i = 0; i < 13; i++)
            _inputBuffer[i] = (uint32_t *) calloc(9, sizeof(uint32_t));
        _game->SetInput(0, "gamepad", (uint8_t *)_inputBuffer[0]);

        // Set up surface -- might move to execute_frame
        MDFN_PixelFormat pix_fmt(MDFN_COLORSPACE_RGB, 0, 8, 16, 24);
        _surf = new MDFN_Surface(NULL, _game->fb_width, _game->fb_height, _game->fb_width, pix_fmt);

        _runGame = true;

        jshort gameInfo[GAME_INFO_ARRAY_SIZE];
        gameInfo[0] = _game->fps;
        gameInfo[1] = _game->nominal_width;
        gameInfo[2] = _game->nominal_height;
        gameInfo[3] = _game->fb_width;
        gameInfo[4] = _game->fb_height;
        gameInfo[5] = _game->soundchan;
        gameInfo[6] = _game->rotated;

        jshortArray gameInfoArray = env->NewShortArray(GAME_INFO_ARRAY_SIZE);
        env->SetShortArrayRegion(gameInfoArray, 0, GAME_INFO_ARRAY_SIZE, gameInfo);
        return gameInfoArray;
    }

    JNIEXPORT jshortArray JNICALL
    Java_com_atelieryl_wonderdroid_WonderSwan__1execute_1frame(JNIEnv *env, jclass obj, jboolean skip,
                                                               jboolean audio, jobject framebuffer,
                                                               jshortArray audiobuffer) {
        LOGD("Called execute frame!");

        if (!_runGame) return 0;

        // Mednafen emulate
        static int16_t sound_buf[SOUND_BUF_SIZE];
        int32 rects[_game->fb_height];

        memset(rects, 0, _game->fb_height*sizeof(int32));
        rects[0] = ~0;
        EmulateSpecStruct spec;
        spec.surface = _surf;
        spec.SoundRate = _sampleRate;
        spec.SoundBuf = sound_buf;
        spec.LineWidths = rects;
        spec.SoundBufMaxSize = sizeof(sound_buf) / 2;
        spec.SoundVolume = 1.0;
        spec.soundmultiplier = 1.0;

        MDFNI_Emulate(&spec);

        // Copy framebuffer
        uint32_t* fb = (uint32_t*) env->GetDirectBufferAddress(framebuffer);
        memcpy(fb, _surf->pixels, _game->fb_width * _game->fb_height * sizeof(uint32_t));

        // Audio
        env->SetShortArrayRegion(audiobuffer, 0, spec.SoundBufSize * _game->soundchan, sound_buf);

        jshort frameInfo[FRAME_INFO_ARRAY_SIZE];
        frameInfo[0] = spec.SoundBufSize;
        frameInfo[1] = spec.DisplayRect.x;
        frameInfo[2] = spec.DisplayRect.y;

        jshortArray frameInfoArray = env->NewShortArray(FRAME_INFO_ARRAY_SIZE);
        env->SetShortArrayRegion(frameInfoArray, 0, FRAME_INFO_ARRAY_SIZE, frameInfo);
        return frameInfoArray;
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