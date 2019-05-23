/* Cygne
 *
 * Copyright notice for this file:
 *  Copyright (C) 2002 Dox dox@space.pl
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <string.h>

#include "wswan.h"
#include "gfx.h"
#include "memory.h"
#include "sound.h"
#include "eeprom.h"
#include "rtc.h"
#include "v30mz.h"

#include <time.h>
#include <math.h>

#include "log.h"

uint32_t wsRAMSize;
//uint8_t wsRAM[65536];
uint8_t *wsSRAM = NULL;

uint8_t *wsCartROM;
uint32_t sram_size;
uint32_t eeprom_size;

uint8_t ButtonWhich, ButtonReadLatch;

uint32_t DMASource, DMADest;
uint16_t DMALength;
uint8_t DMAControl;

uint32_t SoundDMASource;
uint16_t SoundDMALength;
uint8_t SoundDMAControl;

uint8_t BankSelector[4];

uint8_t CommControl, CommData;

extern uint16_t WSButtonStatus;

inline void WSwan_writemem20(uint32_t A, uint8_t V) {
	uint32_t offset, bank;

	offset = A & 0xffff;
	bank = (A >> 16) & 0xF;

	if (!bank) /*RAM*/
	{
		wswan_soundcheckramwrite(offset);
		wsRAM[offset] = V;

		WSWan_TCacheInvalidByAddr(offset);

		if (offset >= 0xfe00) /*WSC palettes*/
			WSwan_GfxWSCPaletteRAMWrite(offset, V);
	}
	else if (bank == 1) /* SRAM */
	{
		if (sram_size) {
			wsSRAM[(offset | (BankSelector[1] << 16)) & (sram_size - 1)] = V;
		}
	}
}

inline uint8_t WSwan_readmem20(uint32_t A) {

	uint32_t offset, bank;

	offset = A & 0xFFFF;
	bank = (A >> 16) & 0xF;

	uint8_t byte = 0;

	switch (bank) {
		case 0:
			byte = wsRAM[offset];
			break;
		case 1:
			if (sram_size) {
				byte = wsSRAM[(offset | (BankSelector[1] << 16)) & (sram_size - 1)];

			}
			;
			break;
		case 2:
		case 3:
			byte = wsCartROM[offset + ((BankSelector[bank] & ((rom_size >> 16) - 1)) << 16)];
			break;

		default: {
			uint8_t bank_num = ((BankSelector[0] & 0xF) << 4) | (bank & 0xf);
			bank_num &= (rom_size >> 16) - 1;
			byte = (wsCartROM[(bank_num << 16) | offset]);
		}
			break;
	}
	return byte;
}

static uint8_t ReadCartByte(uint32_t address) {
	return 0;
}

static void ws_CheckDMA(void) {
	if (DMAControl & 0x80) {
		while (DMALength) {
			WSwan_writemem20(DMADest, WSwan_readmem20(DMASource));

			DMASource++; // = ((DMASource + 1) & 0xFFFF) | (DMASource & 0xFF0000);
			//if(!(DMASource & 0xFFFF)) puts("Warning: DMA source bank crossed.");
			DMADest = ((DMADest + 1) & 0xFFFF) | (DMADest & 0xFF0000);
			DMALength--;
		}
	}
	DMAControl &= ~0x80;
}

void WSwan_CheckSoundDMA(void) {
	if (SoundDMAControl & 0x80) {
		if (SoundDMALength) {
			uint8_t zebyte = WSwan_readmem20(SoundDMASource);

			if (SoundDMAControl & 0x08)
				zebyte ^= 0x80;

			if (SoundDMAControl & 0x10)
				wswan_soundwrite(0x95, zebyte); // Pick a port, any port?!
			else
				wswan_soundwrite(0x89, zebyte);

			SoundDMASource++; // = ((SoundDMASource + 1) & 0xFFFF) | (SoundDMASource & 0xFF0000);
			//if(!(SoundDMASource & 0xFFFF)) puts("Warning:  Sound DMA source bank crossed.");
			SoundDMALength--;
		}
		if (!SoundDMALength)
			SoundDMAControl &= ~0x80;
	}
}

uint8_t WSwan_readport(uint32_t number) {
	//char tempstring[256];
	//sprintf(tempstring,"WSwan_readport(%d)", number);
	//LOGD(tempstring);

	number &= 0xFF;

	if (number >= 0x80 && number <= 0x9F)
		return (wswan_soundread(number));
	else if (number <= 0x3F || (number >= 0xA0 && number <= 0xAF) || (number == 0x60))
		return (WSwan_GfxRead(number));
	else if ((number >= 0xBA && number <= 0xBE) || (number >= 0xC4 && number <= 0xC8))
		return (WSwan_EEPROMRead(number));
	else if (number >= 0xCA && number <= 0xCB)
		return (WSwan_RTCRead(number));
	else
		switch (number) {
			//default: printf("Read: %04x\n", number); break;
			case 0x40:
				return (DMASource >> 0);
			case 0x41:
				return (DMASource >> 8);
			case 0x42:
				return (DMASource >> 16);

			case 0x43:
				return (DMADest >> 16);
			case 0x44:
				return (DMADest >> 0);
			case 0x45:
				return (DMADest >> 8);

			case 0x46:
				return (DMALength >> 0);
			case 0x47:
				return (DMALength >> 8);

			case 0x48:
				return (DMAControl);

			case 0xB0:
			case 0xB2:
			case 0xB6:
				return (WSwan_InterruptRead(number));

			case 0xC0:
				return (BankSelector[0] | 0x20);
			case 0xC1:
				return (BankSelector[1]);
			case 0xC2:
				return (BankSelector[2]);
			case 0xC3:
				return (BankSelector[3]);

			case 0x4a:
				return (SoundDMASource >> 0);
			case 0x4b:
				return (SoundDMASource >> 8);
			case 0x4c:
				return (SoundDMASource >> 16);
			case 0x4e:
				return (SoundDMALength >> 0);
			case 0x4f:
				return (SoundDMALength >> 8);
			case 0x52:
				return (SoundDMAControl);

			case 0xB1:
				return (CommData);

			case 0xb3: {
				uint8_t ret = CommControl & 0xf0;

				if (CommControl & 0x80)
					ret |= 0x4; // Send complete

				return (ret);
			}
			case 0xb5: {
				uint8_t ret = (ButtonWhich << 4) | ButtonReadLatch;
				return (ret);
			}
		}

	if (number >= 0xC8)
		return (0xD1);

	return (0);
}

void WSwan_writeport(uint32_t IOPort, uint8_t V) {
//	char tempstring[256];
	//sprintf(tempstring,"WSwan_writeport(%d, 0x%x)", IOPort, V);

	//LOGD(tempstring);

	IOPort &= 0xFF;

	if (IOPort >= 0x80 && IOPort <= 0x9F) {
		wswan_soundwrite(IOPort, V);
	}
	else if ((IOPort >= 0x00 && IOPort <= 0x3F) || (IOPort >= 0xA0 && IOPort <= 0xAF) || (IOPort == 0x60)) {
		WSwan_GfxWrite(IOPort, V);
	}
	else if ((IOPort >= 0xBA && IOPort <= 0xBE) || (IOPort >= 0xC4 && IOPort <= 0xC8))
		WSwan_EEPROMWrite(IOPort, V);
	else if (IOPort >= 0xCA && IOPort <= 0xCB)
		WSwan_RTCWrite(IOPort, V);
	else
		switch (IOPort) {
			//default: printf("%04x %02x\n", IOPort, V); break;

			case 0x40:
				DMASource &= 0xFFFF00;
				DMASource |= (V << 0);
				break;
			case 0x41:
				DMASource &= 0xFF00FF;
				DMASource |= (V << 8);
				break;
			case 0x42:
				DMASource &= 0x00FFFF;
				DMASource |= ((V & 0x0F) << 16);
				break;

			case 0x43:
				DMADest &= 0x00FFFF;
				DMADest |= ((V & 0x0F) << 16);
				break;
			case 0x44:
				DMADest &= 0xFFFF00;
				DMADest |= (V << 0);
				break;
			case 0x45:
				DMADest &= 0xFF00FF;
				DMADest |= (V << 8);
				break;

			case 0x46:
				DMALength &= 0xFF00;
				DMALength |= (V << 0);
				break;
			case 0x47:
				DMALength &= 0x00FF;
				DMALength |= (V << 8);
				break;

			case 0x48:
				DMAControl = V;
				//if(V&0x80)
				// printf("DMA%02x: %08x %08x %08x\n", V, DMASource, DMADest, DMALength);
				ws_CheckDMA();
				break;

			case 0x4a:
				SoundDMASource &= 0xFFFF00;
				SoundDMASource |= (V << 0);
				break;
			case 0x4b:
				SoundDMASource &= 0xFF00FF;
				SoundDMASource |= (V << 8);
				break;
			case 0x4c:
				SoundDMASource &= 0x00FFFF;
				SoundDMASource |= (V << 16);
				break;
				//case 0x4d: break; // Unused?
			case 0x4e:
				SoundDMALength &= 0xFF00;
				SoundDMALength |= (V << 0);
				break;
			case 0x4f:
				SoundDMALength &= 0x00FF;
				SoundDMALength |= (V << 8);
				break;
				//case 0x50: break; // Unused?
				//case 0x51: break; // Unused?
			case 0x52:
				SoundDMAControl = V;
				//if(V & 0x80) printf("Sound DMA: %02x, %08x %08x\n", V, SoundDMASource, SoundDMALength);
				break;

			case 0xB0:
			case 0xB2:
			case 0xB6:
				WSwan_InterruptWrite(IOPort, V);
				break;

			case 0xB1:
				CommData = V;
				break;
			case 0xB3:
				CommControl = V & 0xF0;
				break;

			case 0xb5:
				ButtonWhich = V >> 4;
				ButtonReadLatch = 0;

				if (ButtonWhich & 0x4) { /*buttons*/

					ButtonReadLatch |= ((WSButtonStatus >> 8) << 1) & 0xF;
				}
				if (ButtonWhich & 0x2) { /* H/X cursors */

					ButtonReadLatch |= WSButtonStatus & 0xF;
				}
				if (ButtonWhich & 0x1) { /* V/Y cursors */

					ButtonReadLatch |= (WSButtonStatus >> 4) & 0xF;
				}

				break;

			case 0xC0:
				BankSelector[0] = V & 0xF;
				break;
			case 0xC1:
				BankSelector[1] = V;
				break;
			case 0xC2:
				BankSelector[2] = V;
				break;
			case 0xC3:
				BankSelector[3] = V;
				break;
		}
}



void WSwan_MemoryInit(bool IsWSC, uint32_t ssize) {
	wsRAMSize = 65536;
	sram_size = ssize;

	//uint16_t byear = MDFN_GetSettingUI("wswan.byear");
	//uint8_t bmonth = MDFN_GetSettingUI("wswan.bmonth");
	//uint8_t bday = MDFN_GetSettingUI("wswan.bday");
	//std::string sex_s = MDFN_GetSettingS("wswan.sex");
	//std::string blood_s = MDFN_GetSettingS("wswan.blood");
	//uint8_t sex = 1, blood = 1;

// if(!strcasecmp(sex_s.c_str(), "m") || !strcasecmp(sex_s.c_str(), "male"))
	// sex = 1;
// else if(!strcasecmp(sex_s.c_str(), "f") || !strcasecmp(sex_s.c_str(), "female"))
	// sex = 2;
	//else
// {
	// int tmp_num;
	// if(sscanf(sex_s.c_str(), "%u", &tmp_num) == 1)
	// {
	//  sex = tmp_num;
	// }
	//}

	//if(!strcasecmp(blood_s.c_str(), "a"))
	// blood = 1;
	//else if(!strcasecmp(blood_s.c_str(), "b"))
	// blood = 2;
	//else if(!strcasecmp(blood_s.c_str(), "o"))
	// blood = 3;
	//else if(!strcasecmp(blood_s.c_str(), "ab"))
	// blood = 4;
	//else
	//{
	// int tmp_num;
	// if(sscanf(blood_s.c_str(), "%u", &tmp_num) == 1)
	// {
	//  blood = tmp_num;
	// }
	//}

	//WSwan_EEPROMInit(MDFN_GetSettingS("wswan.name").c_str(), byear, bmonth, bday, sex, blood);

	if (sram_size) {
		wsSRAM = (uint8_t*) malloc(sram_size);
		memset(wsSRAM, 0, sram_size);
	}

}

void WSwan_MemoryReset(void) {
	memset(&wsRAM, 0, 65536);

	wsRAM[0x75AC] = 0x41;
	wsRAM[0x75AD] = 0x5F;
	wsRAM[0x75AE] = 0x43;
	wsRAM[0x75AF] = 0x31;
	wsRAM[0x75B0] = 0x6E;
	wsRAM[0x75B1] = 0x5F;
	wsRAM[0x75B2] = 0x63;
	wsRAM[0x75B3] = 0x31;

	memset(BankSelector, 0, sizeof(BankSelector));
	ButtonWhich = 0;
	ButtonReadLatch = 0;
	DMASource = 0;
	DMADest = 0;
	DMALength = 0;
	DMAControl = 0;

	SoundDMASource = 0;
	SoundDMALength = 0;
	SoundDMAControl = 0;

	CommControl = 0;
	CommData = 0;
}

