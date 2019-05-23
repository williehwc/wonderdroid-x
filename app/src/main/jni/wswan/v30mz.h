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

typedef union { /* eight general registers */
	uint16_t w[8]; /* viewed as 16 bits registers */
	uint8_t b[16]; /* or as 8 bit registers */
} v30mz_basicregs_t;

typedef struct {
	v30mz_basicregs_t regs;
	uint16_t sregs[4];

	uint16_t pc;

	int32_t SignVal;
	uint32_t AuxVal, OverVal, ZeroVal, CarryVal, ParityVal; /* 0 or non-0 valued flags */
	uint8_t TF, IF, DF;
} v30mz_regs_t;

/* Public variables */
extern int v30mz_ICount; // 4 B
extern uint32_t v30mz_timestamp; // 4 B
extern v30mz_regs_t I; // 56 B
extern bool InHLT; // 1 B
extern uint32_t prefix_base; // 4 B
extern char seg_prefix; // 1 B
extern uint8_t parity_table[256]; // 256 B

/* Public functions */
void v30mz_execute(int cycles);
void v30mz_set_reg(int, unsigned);
unsigned v30mz_get_reg(int regnum);
void v30mz_reset(void);
void v30mz_init(uint8_t (*readmem20)(uint32_t), void (*writemem20)(uint32_t, uint8_t), uint8_t (*readport)(uint32_t),
		void (*writeport)(uint32_t, uint8_t));

void v30mz_int(uint32_t vector, bool IgnoreIF);

#endif
