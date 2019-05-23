#ifndef __WSWAN_MEMORY_H
#define __WSWAN_MEMORY_H

#include <stdbool.h>
#include <stdint.h>

#ifndef __cplusplus

uint8_t wsRAM[65536];
uint8_t *wsCartROM;
uint32_t eeprom_size;
uint8_t wsEEPROM[2048];
uint8_t *wsSRAM;

#endif

extern uint8_t ButtonWhich, ButtonReadLatch;

extern uint32_t DMASource, DMADest;
extern uint16_t DMALength;
extern uint8_t DMAControl;

extern uint32_t SoundDMASource;
extern uint16_t SoundDMALength;
extern uint8_t SoundDMAControl;

extern uint8_t BankSelector[4];

extern uint8_t CommControl, CommData;

extern uint16_t WSButtonStatus;

uint8_t WSwan_readmem20(uint32_t);
void WSwan_writemem20(uint32_t address, uint8_t data);

void WSwan_MemoryInit(bool IsWSC, uint32_t ssize);


void WSwan_CheckSoundDMA(void);

void WSwan_MemoryReset(void);
void WSwan_writeport(uint32_t IOPort, uint8_t V);
uint8_t WSwan_readport(uint32_t number);

#endif
