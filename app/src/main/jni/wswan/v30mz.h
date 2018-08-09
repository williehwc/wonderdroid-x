#ifndef __V30MZ_H_
#define __V30MZ_H_

#include <stdbool.h>

enum {
	NEC_PC = 1,
	NEC_AW,
	NEC_CW,
	NEC_DW,
	NEC_BW,
	NEC_SP,
	NEC_BP,
	NEC_IX,
	NEC_IY,
	NEC_FLAGS,
	NEC_DS1,
	NEC_PS,
	NEC_SS,
	NEC_DS0
};

/* Public variables */
int v30mz_ICount;
uint32_t v30mz_timestamp;

/* Public functions */
void v30mz_execute(int cycles);
void v30mz_set_reg(int, unsigned);
unsigned v30mz_get_reg(int regnum);
void v30mz_reset(void);
void v30mz_init(uint8_t (*readmem20)(uint32_t), void (*writemem20)(uint32_t, uint8_t), uint8_t (*readport)(uint32_t),
		void (*writeport)(uint32_t, uint8_t));

void v30mz_int(uint32_t vector, bool IgnoreIF);

#endif
