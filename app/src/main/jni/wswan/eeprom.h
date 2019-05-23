#ifndef __WSWAN_EEPROM_H
#define __WSWAN_EEPROM_H

uint8_t WSwan_EEPROMRead(uint32_t A);
void WSwan_EEPROMWrite(uint32_t A, uint8_t V);

void WSwan_EEPROMReset(void);
void WSwan_EEPROMInit(const char *Name, const uint16_t BYear, const uint8_t BMonth, const uint8_t BDay,
		const uint8_t Sex, const uint8_t Blood);

#endif
