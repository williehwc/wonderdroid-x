#include "com_atelieryl_wonderdroid_WonderSwan.h"

#include <stdio.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <math.h>

#include <stdbool.h>
#include <stdint.h>
#include "wswan.h"

#include "gfx.h"
#include "memory.h"
#include "start.h"
#include "sound.h"
#include "v30mz.h"
#include "rtc.h"
#include "eeprom.h"

#include "log.h"

uint32_t rom_size;
int wsc = 0; /*color/mono*/
uint16_t WSButtonStatus;
uint32_t sram_size;
uint32_t eeprom_size;

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_reset(JNIEnv * env, jclass obj) {
	LOGD("v30mz_reset()");
	v30mz_reset();
	LOGD("WSwan_MemoryReset()");
	WSwan_MemoryReset();
	LOGD("WSwan_GfxReset()");
	WSwan_GfxReset();
	LOGD("WSwan_SoundReset()");
	wswan_soundreset();
	LOGD("WSwan_InterruptReset()");
	WSwan_InterruptReset();
	LOGD("WSwan_RTCReset()");
	WSwan_RTCReset();
	LOGD("WSwan_EEPROMReset()");
	WSwan_EEPROMReset();

	wsMakeTiles();
	wsSetVideo(wsVMode, TRUE);

	for (int u0 = 0; u0 < 0xc9; u0++)
		WSwan_writeport(u0, startio[u0]);

	v30mz_set_reg(NEC_SS, 0);
	v30mz_set_reg(NEC_SP, 0x2000);
}

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_load(JNIEnv * env, jclass obj, jstring filename,
		jboolean iswsc, jstring name, jint year, jint month, jint day, jint blood, jint sex) {

	if (iswsc) {
		LOGD("Emulating a WonderSwan Color");
		wsc = 1;
		wsVMode = 0x7;

	}
	else {
		wsVMode = 0x0;
	}

	// char convertedfilename[] = filename;
	char temp[512];
	const jbyte *str;
	str = (*env)->GetStringUTFChars(env, filename, NULL);
	if (str == NULL) {
		//return NULL; /* OutOfMemoryError already thrown */
		//break;
	}

	snprintf(temp, sizeof(temp), "Loading %s", str);
	LOGD(temp);
	FILE* file = fopen(str, "r");
	if (file != NULL) {
		LOGD("The file loaded!!!");
	}

	struct stat st;
	stat(str, &st);
	rom_size = st.st_size;

	//rom_size = uppow2(fp->size);
	// rom_size = 4 * 1024 * 1024;
	//sprintf(tempstring, "size of rom: %d", rom_size);
	// LOGD(tempstring);
	if (wsCartROM != NULL) {
		free(wsCartROM);
	}
	wsCartROM = (uint8_t *) calloc(1, rom_size);

	fread(wsCartROM, sizeof(uint8_t), rom_size, file);

	fclose(file);

	uint8_t header[10];
	memcpy(header, wsCartROM + rom_size - 10, 10);

	sram_size = 0;
	eeprom_size = 0;

	switch (header[5]) {
		case 0x01:
			sram_size = 8 * 1024;
			break;
		case 0x02:
			sram_size = 32 * 1024;
			break;
		case 0x03:
			sram_size = 16 * 65536;
			break;
		case 0x04:
			sram_size = 32 * 65536;
			break; // Dicing Knight!

		case 0x10:
			eeprom_size = 128;
			break;
		case 0x20:
			eeprom_size = 2 * 1024;
			break;
		case 0x50:
			eeprom_size = 1024;
			break;
	}

	// sprintf(tempstring, "SRAM size is 0x%x", sram_size);
	// LOGD(tempstring);

	// sprintf(tempstring, "EEPROM size is 0x%x", eeprom_size);
	//  LOGD(tempstring);

	if (header[6] & 0x1) {
//LOGD("Game orientation is vertical");
	}
	else {
//LOGD("Game orientation is horizontal");
	}

	v30mz_init(WSwan_readmem20, WSwan_writemem20, WSwan_readport, WSwan_writeport);
	// sprintf(tempstring, "WSwan_MemoryInit(%d, %d)", wsc, sram_size);
	// LOGD(tempstring);
	WSwan_MemoryInit(wsc, sram_size); // EEPROM and SRAM are loaded in this func.

	const jbyte *namestr;
	str = (*env)->GetStringUTFChars(env, name, NULL);
	if (str == NULL) {
		//return NULL; /* OutOfMemoryError already thrown */
		//break;
	}
	WSwan_EEPROMInit("", (uint16_t) year, (uint8_t) month, (uint8_t) day, (uint8_t) sex, (uint8_t) blood);

// LOGD("WSwan_GfxInit()");
	WSwan_GfxInit();

// LOGD("WSwan_SoundInit()");
	wswan_soundinit();

// LOGD("wsMakeTiles()");
	wsMakeTiles();

//LOGD("reset()");
//reset();
}

JNIEXPORT jint JNICALL Java_com_atelieryl_wonderdroid_WonderSwan__1execute_1frame(JNIEnv *env, jclass obj,
		jboolean skip, jboolean audio, jobject framebuffer, jshortArray audiobuffer) {

	// execute the active frame cycles
	uint16_t* fb = (uint16_t*) (*env)->GetDirectBufferAddress(env, framebuffer);
	while (wsExecuteLine(fb, skip) < 144) {};

	// grab the audio if we want it
	jint samples = 0;
	if(audio){
		int16_t* ab = (int16_t*) (*env)->GetShortArrayElements(env, audiobuffer, NULL);
		samples = (jint) wswan_soundflush(ab);
		(*env)->ReleaseShortArrayElements(env, audiobuffer, ab, 0);
	}
	else {
		wswan_soundclear();
	}

	// execute the vblank section
	while (wsExecuteLine(NULL, FALSE) != 0) {}

	// return the number of new audio samples
	return samples;
}

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_updatebuttons(JNIEnv *env, jclass obj,
		jboolean y1, jboolean y2, jboolean y3, jboolean y4, jboolean x1, jboolean x2, jboolean x3, jboolean x4,
		jboolean a, jboolean b, jboolean start) {

	uint16_t newbuttons = 0x0000;

	if (start)
		newbuttons |= 0x100;
	if (a)
		newbuttons |= 0x200;
	if (b)
		newbuttons |= 0x400;
	if (x1)
		newbuttons |= 0x1;
	if (x2)
		newbuttons |= 0x2;
	if (x3)
		newbuttons |= 0x4;
	if (x4)
		newbuttons |= 0x8;
	if (y1)
		newbuttons |= 0x10;
	if (y2)
		newbuttons |= 0x20;
	if (y3)
		newbuttons |= 0x40;
	if (y4)
		newbuttons |= 0x80;

	WSButtonStatus = newbuttons;
}

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_execute_1vblank(JNIEnv *env, jclass obj) {
	//while(wsExecuteLine(NULL, FALSE) != 0){}
}

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_loadbackupdata(JNIEnv *env, jclass obj,
		jstring filename) {

	LOGD("loading backup data");

	char temp[256];
	const jbyte *str;
	str = (*env)->GetStringUTFChars(env, filename, NULL);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
	}

	snprintf(temp, sizeof(temp), "eeprom size %d, sram size %d", eeprom_size, sram_size);

	FILE* file = fopen(str, "r");
	if (file != NULL) {
		if (sram_size) {
			LOGD("Loading SRAM");
			fread(wsSRAM, sizeof(uint8_t), sram_size, file);
		}

		else if (eeprom_size) {
			LOGD("Loading eeprom");
			fread(wsEEPROM, sizeof(uint8_t), eeprom_size, file);
		}

		fclose(file);
	}

}

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_storebackupdata(JNIEnv *env, jclass obj,
		jstring filename) {
	LOGD("storing backup data");

	char temp[256];
	const jbyte *str;
	str = (*env)->GetStringUTFChars(env, filename, NULL);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
		//break;
	}

	snprintf(temp, sizeof(temp), "eeprom size %d, sram size %d", eeprom_size, sram_size);

	FILE* file = fopen(str, "w");
	if (file != NULL) {
		if (sram_size) {
			LOGD("Writing SRAM");
			fwrite(wsSRAM, sizeof(uint8_t), sram_size, file);
		}
		else if (eeprom_size) {
			LOGD("Writing eeprom");
			fwrite(wsEEPROM, sizeof(uint8_t), eeprom_size, file);
		}
		fclose(file);
	}

}
