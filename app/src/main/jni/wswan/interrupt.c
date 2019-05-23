#include "wswan.h"
#include "interrupt.h"
#include "v30mz.h"

uint8_t IStatus;
uint8_t IEnable;
uint8_t IVectorBase;

bool IOn_Cache = FALSE;
uint32_t IOn_Which = 0;
uint32_t IVector_Cache = 0;

static void RecalcInterrupt(void) {
	IOn_Cache = FALSE;

	for (int i = 0; i < 8; i++) {
		if (IStatus & IEnable & (1 << i)) {
			IOn_Cache = TRUE;
			IOn_Which = i;
			IVector_Cache = (IVectorBase + i) * 4;
			break;
		}
	}
}

void WSwan_InterruptDebugForce(unsigned int level) {
	v30mz_int((IVectorBase + level) * 4, TRUE);
}

void WSwan_Interrupt(int which) {
	if (IEnable & (1 << which))
		IStatus |= 1 << which;

	//printf("Interrupt: %d\n", which);
	RecalcInterrupt();
}

void WSwan_InterruptWrite(uint32_t A, uint8_t V) {
	//printf("Write: %04x %02x\n", A, V);
	switch (A) {
		case 0xB0:
			IVectorBase = V;
			RecalcInterrupt();
			break;
		case 0xB2:
			IEnable = V;
			IStatus &= IEnable;
			RecalcInterrupt();
			break;
		case 0xB6: /*printf("IStatus: %02x\n", V);*/
			IStatus &= ~V;
			RecalcInterrupt();
			break;
	}
}

uint8_t WSwan_InterruptRead(uint32_t A) {
	//printf("Read: %04x\n", A);
	switch (A) {
		case 0xB0:
			return (IVectorBase);
		case 0xB2:
			return (IEnable);
		case 0xB6:
			return (1 << IOn_Which); //return(IStatus);
	}
	return (0);
}

void WSwan_InterruptCheck(void) {
	if (IOn_Cache) {
		v30mz_int(IVector_Cache, FALSE);
	}
}

void WSwan_InterruptReset(void) {
	IEnable = 0x00;
	IStatus = 0x00;
	IVectorBase = 0x00;
	RecalcInterrupt();
}

