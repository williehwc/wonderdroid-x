#ifndef __WSWAN_RTC_H
#define __WSWAN_RTC_H

void WSwan_RTCWrite(uint32_t A, uint8_t V);
uint8_t WSwan_RTCRead(uint32_t A);
void WSwan_RTCReset(void);
void WSwan_RTCClock(uint32_t cycles);

#endif
