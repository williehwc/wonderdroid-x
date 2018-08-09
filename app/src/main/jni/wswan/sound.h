#ifndef __WSWAN_SOUND_H
#define __WSWAN_SOUND_H

#include <stdbool.h>

int16_t WSwan_SoundFlush(int16_t *buffer);
void WSwan_SoundClear();
void WSwan_SoundInit();

void WSwan_Sound(int rate);

void WSwan_SoundWrite(uint32_t, uint8_t);
uint8_t WSwan_SoundRead(uint32_t);
//void WSwan_SoundInit(void);

void WSwan_SoundReset(void);

#ifdef __cplusplus
	extern "C" uint8_t wsRAM[65536];
#endif

#ifndef __cplusplus
	void wswan_soundinit(void);
	uint8_t wswan_soundread(uint32_t A);
	void wswan_soundwrite(uint32_t A, uint8_t V);
	int16_t wswan_soundflush(int16_t *buffer);
	void wswan_soundreset(void);
	void wswan_soundcheckramwrite(uint32_t A);
	void wswan_soundclear();
#endif

void WSwan_SoundCheckRAMWrite(uint32_t A);

#endif
