/******************************************************************************/
/* Mednafen - Multi-system Emulator                                           */
/******************************************************************************/
/* sound.h - WonderSwan Sound Emulation
**  Copyright (C) 2007-2016 Mednafen Team
**
** This program is free software; you can redistribute it and/or
** modify it under the terms of the GNU General Public License
** as published by the Free Software Foundation; either version 2
** of the License, or (at your option) any later version.
**
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU General Public License for more details.
**
** You should have received a copy of the GNU General Public License
** along with this program; if not, write to the Free Software Foundation, Inc.,
** 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

#ifndef __WSWAN_SOUND_H
#define __WSWAN_SOUND_H

namespace MDFN_IEN_WSWAN
{

extern uint16 period[4]; ///
extern uint8 volume[4]; ///
extern uint8 voice_volume; ///

extern uint8 sweep_step, sweep_value; ///
extern uint8 noise_control; ///
extern uint8 control; ///
extern uint8 output_control; ///

extern int32 sweep_8192_divider; ///
extern uint8 sweep_counter; ///
extern uint8 SampleRAMPos; ///

extern int32 sample_cache[4][2]; ///

extern int32 last_v_val; ///

extern uint8 HyperVoice; ///
extern int32 last_hv_val[2]; ///

extern int32 period_counter[4]; ///
extern int32 last_val[4][2]; ///
extern uint8 sample_pos[4]; ///
extern uint16 nreg; ///
extern uint32 last_ts; ///

int32 WSwan_SoundFlush(int16 *SoundBuf, const int32 MaxSoundFrames);

void WSwan_SoundInit(void) MDFN_COLD;
void WSwan_SoundKill(void) MDFN_COLD;
void WSwan_SetSoundMultiplier(double multiplier);
bool WSwan_SetSoundRate(uint32 rate);
void WSwan_SoundStateAction(StateMem *sm, const unsigned load, const bool data_only);

void WSwan_SoundWrite(uint32, uint8);
uint8 WSwan_SoundRead(uint32);
void WSwan_SoundReset(void);
void WSwan_SoundCheckRAMWrite(uint32 A);

}

#endif
