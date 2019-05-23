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

extern uint16_t period[4];
extern uint8_t volume[4]; // left volume in upper 4 bits, right in lower 4 bits
extern uint8_t voice_volume;

extern uint8_t sweep_step, sweep_value;
extern uint8_t noise_control;
extern uint8_t control;
extern uint8_t output_control;

extern int32_t sweep_8192_divider;
extern uint8_t sweep_counter;
extern uint8_t SampleRAMPos;

extern int32_t sample_cache[4][2];

extern int32_t last_v_val;

extern uint8_t HyperVoice;
extern int32_t last_hv_val;

extern int32_t period_counter[4];
extern int32_t last_val[4][2];
extern uint8_t sample_pos[4];
extern uint16_t nreg;
extern uint32_t last_ts;

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
