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

bool stateLock;

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
			LOGD("This is a header 0x01 ROM.");
			sram_size = 8 * 1024;
			break;
		case 0x02:
			LOGD("This is a header 0x02 ROM.");
			sram_size = 32 * 1024;
			break;
		case 0x03:
			LOGD("This is a header 0x03 ROM.");
			sram_size = 16 * 65536;
			break;
		case 0x04:
			LOGD("This is a header 0x04 ROM.");
			sram_size = 32 * 65536;
			break; // Dicing Knight!

		case 0x10:
			LOGD("This is a header 0x10 ROM.");
			eeprom_size = 128;
			break;
		case 0x20:
			LOGD("This is a header 0x20 ROM.");
			eeprom_size = 2 * 1024;
			break;
		case 0x50:
			LOGD("This is a header 0x50 ROM.");
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

	if (stateLock) {
		return 0;
	}

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

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_loadbackup(JNIEnv *env, jclass obj,
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

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_savebackup(JNIEnv *env, jclass obj,
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

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_loadstate(JNIEnv *env, jclass obj,
		jstring filename) {

	LOGD("loading state data");

	char temp[256];
	const jbyte *str;
	str = (*env)->GetStringUTFChars(env, filename, NULL);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
	}

	FILE* file = fopen(str, "r");
	if (file != NULL) {
		LOGD("Loading state");
		stateLock = true;
		uint8_t toLoad[65536 + sizeof(unsigned) * 14 + sram_size + 1 + 1312 + 28 + 547 + 8224 + 132104 + 1048576 + sizeof(bool) + 11 + 326 + 123];
		fread(toLoad, sizeof(uint8_t), 65536 + sizeof(unsigned) * 14 + sram_size + 1 + 1312 + 28 + 547 + 8224 + 132104 + 1048576 + sizeof(bool) + 11 + 326 + 123, file);
		memcpy(wsRAM, toLoad, 65536);
		for (int i = 0; i < 14; i++) {
			unsigned *reg = malloc(sizeof(unsigned));
			memcpy(reg, toLoad + 65536 + sizeof(unsigned) * i, sizeof(unsigned));
			v30mz_set_reg(i + 1, *reg);
		}
		memcpy(wsSRAM, toLoad + 65536 + sizeof(unsigned) * 14, sram_size);
		memcpy(&wsLine, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size, sizeof(uint8_t)); // DIFF 18038
		memcpy(wsMonoPal, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1, 256);
		memcpy(wsColors, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1 + 256, 32);
		memcpy(wsCols, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1 + 256 + 32, 1024);
		memcpy(&ButtonWhich, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1313, 1);
		memcpy(&ButtonReadLatch, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1314, 1);
		memcpy(&DMASource, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1315, 4);
		memcpy(&DMADest, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1319, 4);
		memcpy(&DMALength, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1323, 2);
		memcpy(&DMAControl, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1325, 1);
		memcpy(&SoundDMASource, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1326, 4);
		memcpy(&SoundDMALength, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1330, 2);
		memcpy(&SoundDMAControl, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1332, 1);
		memcpy(BankSelector, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1333, 4);
		memcpy(&CommControl, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1337, 1);
		memcpy(&CommData, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1338, 1);
		memcpy(&WSButtonStatus, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1339, 2);
		memcpy(SpriteTable, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1341, 512);
		memcpy(&SpriteCountCache, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1853, 4);
		memcpy(&DispControl, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1857, 1);
		memcpy(&BGColor, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1858, 1);
		memcpy(&LineCompare, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1859, 1); // DIFF 1877B
		memcpy(&SPRBase, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1860, 1);
		memcpy(&SpriteStart, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1861, 1);
		memcpy(&SpriteCount, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1862, 1);
		memcpy(&FGBGLoc, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1863, 1);
		memcpy(&FGx0, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1864, 1);
		memcpy(&FGy0, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1865, 1);
		memcpy(&FGx1, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1866, 1);
		memcpy(&FGy1, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1867, 1);
		memcpy(&SPRx0, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1868, 1);
		memcpy(&SPRy0, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1869, 1);
		memcpy(&SPRx1, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1870, 1);
		memcpy(&SPRy1, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1871, 1);
		memcpy(&BGXScroll, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1872, 1); // DIFF 18788
		memcpy(&BGYScroll, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1873, 1);
		memcpy(&FGXScroll, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1874, 1); // DIFF 1878A
		memcpy(&FGYScroll, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1875, 1);
		memcpy(&LCDControl, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1876, 1);
		memcpy(&LCDIcons, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1877, 1);
		memcpy(&BTimerControl, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1878, 1);
		memcpy(&HBTimerPeriod, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1879, 2);
		memcpy(&VBTimerPeriod, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1881, 2);
		memcpy(&HBCounter, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1883, 2);
		memcpy(&VBCounter, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1885, 2);
		memcpy(&VideoMode, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1887, 1);
		memcpy(ColorMapG, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1888, 32);
		memcpy(ColorMap, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1920, 8192);
		memcpy(wsTCache, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 10112, 32768);
		memcpy(wsTCacheFlipped, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 42880, 32768);
		memcpy(wsTileRow, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 75648, 8);
		memcpy(wsTCacheUpdate, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 75656, 512);
		memcpy(wsTCache2, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 76168, 32768);
		memcpy(wsTCacheFlipped2, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 108936, 32768);
		memcpy(wsTCacheUpdate2, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 141704, 512);
		memcpy(tiles, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 142216, 1048576);
		memcpy(&IStatus, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1190792, 1);
		memcpy(&IEnable, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1190793, 1);
		memcpy(&IVectorBase, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1190794, 1);
		memcpy(&IOn_Cache, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + 1190795, sizeof(bool));
		memcpy(&IOn_Which, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190795, 4);
		memcpy(&IVector_Cache, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190799, 4);
		memcpy(&v30mz_ICount, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190803, 4);
		memcpy(&v30mz_timestamp, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190807, 4);
		memcpy(&I, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190811, 56);
		memcpy(&InHLT, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190867, 1);
		memcpy(&prefix_base, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190868, 4);
		memcpy(&seg_prefix, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190872, 1);
		memcpy(parity_table, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190873, 256);
		memcpy(period, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191129, 8);
		memcpy(volume, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191137, 4);
		memcpy(&voice_volume, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191141, 1);
		memcpy(&sweep_step, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191142, 1);
		memcpy(&sweep_value, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191143, 1);
		memcpy(&noise_control, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191144, 1);
		memcpy(&control, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191145, 1);
		memcpy(&output_control, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191146, 1);
		memcpy(&sweep_8192_divider, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191147, 4);
		memcpy(&sweep_counter, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191151, 1);
		memcpy(&SampleRAMPos, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191152, 1);
		memcpy(sample_cache, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191153, 32);
		memcpy(&last_v_val, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191185, 4);
		memcpy(&HyperVoice, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191189, 1);
		memcpy(&last_hv_val, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191190, 4);
		memcpy(period_counter, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191194, 16);
		memcpy(last_val, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191210, 32);
		memcpy(sample_pos, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191242, 4);
		memcpy(&nreg, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191246, 2);
		memcpy(&last_ts, toLoad + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191248, 4);
		wsSetVideo(wsVMode, TRUE);
		/*wsLine = 145;
		WSwan_Interrupt(WSINT_VBLANK);
		while (wsExecuteLine(NULL, FALSE) != 0) {}*/
		stateLock = false;
		fclose(file);
		LOGD("Load complete");
	}

}

JNIEXPORT void JNICALL Java_com_atelieryl_wonderdroid_WonderSwan_savestate(JNIEnv *env, jclass obj,
		jstring filename) {

	LOGD("storing state data");

	char temp[256];
	const jbyte *str;
	str = (*env)->GetStringUTFChars(env, filename, NULL);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
	}

	FILE* file = fopen(str, "w");
	if (file != NULL) {
		LOGD("Writing state");
		stateLock = true;
		uint8_t toWrite[65536 + sizeof(unsigned) * 14 + sram_size + 1 + 1312 + 28 + 547 + 8224 + 132104 + 1048576 + sizeof(bool) + 11 + 326 + 123];
		memcpy(toWrite, wsRAM, 65536);
		for (int i = 0; i < 14; i++) {
			unsigned reg = v30mz_get_reg(i + 1);
			memcpy(toWrite + 65536 + sizeof(unsigned) * i, &reg, sizeof(unsigned));
		}
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14, wsSRAM, sram_size);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size, &wsLine, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1, wsMonoPal, 256);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1 + 256, wsColors, 32);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1 + 256 + 32, wsCols, 1024);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1313, &ButtonWhich, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1314, &ButtonReadLatch, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1315, &DMASource, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1319, &DMADest, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1323, &DMALength, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1325, &DMAControl, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1326, &SoundDMASource, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1330, &SoundDMALength, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1332, &SoundDMAControl, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1333, BankSelector, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1337, &CommControl, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1338, &CommData, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1339, &WSButtonStatus, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1341, SpriteTable, 512);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1853, &SpriteCountCache, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1857, &DispControl, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1858, &BGColor, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1859, &LineCompare, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1860, &SPRBase, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1861, &SpriteStart, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1862, &SpriteCount, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1863, &FGBGLoc, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1864, &FGx0, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1865, &FGy0, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1866, &FGx1, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1867, &FGy1, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1868, &SPRx0, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1869, &SPRy0, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1870, &SPRx1, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1871, &SPRy1, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1872, &BGXScroll, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1873, &BGYScroll, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1874, &FGXScroll, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1875, &FGYScroll, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1876, &LCDControl, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1877, &LCDIcons, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1878, &BTimerControl, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1879, &HBTimerPeriod, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1881, &VBTimerPeriod, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1883, &HBCounter, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1885, &VBCounter, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1887, &VideoMode, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1888, ColorMapG, 32);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1920, ColorMap, 8192);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 10112, wsTCache, 32768);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 42880, wsTCacheFlipped, 32768);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 75648, wsTileRow, 8);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 75656, wsTCacheUpdate, 512);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 76168, wsTCache2, 32768);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 108936, wsTCacheFlipped2, 32768);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 141704, wsTCacheUpdate2, 512);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 142216, tiles, 1048576);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1190792, &IStatus, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1190793, &IEnable, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1190794, &IVectorBase, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + 1190795, &IOn_Cache, sizeof(bool));
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190795, &IOn_Which, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190799, &IVector_Cache, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190803, &v30mz_ICount, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190807, &v30mz_timestamp, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190811, &I, 56);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190867, &InHLT, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190868, &prefix_base, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190872, &seg_prefix, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1190873, parity_table, 256);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191129, period, 8);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191137, volume, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191141, &voice_volume, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191142, &sweep_step, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191143, &sweep_value, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191144, &noise_control, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191145, &control, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191146, &output_control, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191147, &sweep_8192_divider, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191151, &sweep_counter, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191152, &SampleRAMPos, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191153, sample_cache, 32);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191185, &last_v_val, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191189, &HyperVoice, 1);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191190, &last_hv_val, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191194, period_counter, 16);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191210, last_val, 32);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191242, sample_pos, 4);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191246, &nreg, 2);
		memcpy(toWrite + 65536 + sizeof(unsigned) * 14 + sram_size + sizeof(bool) + 1191248, &last_ts, 4);
		fwrite(toWrite, sizeof(uint8_t), 65536 + sizeof(unsigned) * 14 + sram_size + 1 + 1312 + 28 + 547 + 8224 + 132104 + 1048576 + sizeof(bool) + 11 + 326 + 123, file);
		stateLock = false;
		fclose(file);
		LOGD("Save state version 3");
	}

}
