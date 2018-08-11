#ifndef __WSWAN_GFX_H
#define __WSWAN_GFX_H

#include <stdbool.h>

void WSWan_TCacheInvalidByAddr(uint32_t);

extern uint8_t tiles[256][256][2][8];
extern uint8_t wsTCache[512 * 64]; //tiles cache
extern uint8_t wsTCacheFlipped[512 * 64]; //tiles cache (H flip)
extern uint8_t wsTileRow[8]; //extracted 8 pixels (tile row)
extern uint8_t wsTCacheUpdate[512]; //tiles cache flags
extern uint8_t wsTCache2[512 * 64]; //tiles cache
extern uint8_t wsTCacheFlipped2[512 * 64]; //tiles cache (H flip)
extern uint8_t wsTCacheUpdate2[512]; //tiles cache flags
extern int wsVMode; //Video Mode

uint8_t wsLine; //current scan line

extern uint32_t wsMonoPal[16][4]; // 256-B
extern uint32_t wsColors[8]; // 32-B
extern uint32_t wsCols[16][16]; //1024-B

void wsMakeTiles(void);
void wsGetTile(uint32_t, uint32_t, int, int, int);
void wsSetVideo(int, bool);

void wsScanline(uint16_t *target);

extern uint32_t dx_r, dx_g, dx_b, dx_sr, dx_sg, dx_sb; // 4-B each, 24-B total
extern uint32_t dx_bits, dx_pitch, cmov, dx_linewidth_blit, dx_buffer_line; // 4-B each, 20-B total

extern uint16_t ColorMapG[16]; // 32-B
extern uint16_t ColorMap[16 * 16 * 16]; // 8192-B

/*current scanline*/

extern uint8_t SpriteTable[0x80][4]; // 512-B
extern uint32_t SpriteCountCache; // 4-B
extern uint8_t DispControl;
extern uint8_t BGColor;
extern uint8_t LineCompare;
extern uint8_t SPRBase;
extern uint8_t SpriteStart, SpriteCount; // 2-B total
extern uint8_t FGBGLoc;
extern uint8_t FGx0, FGy0, FGx1, FGy1; // 4-B total
extern uint8_t SPRx0, SPRy0, SPRx1, SPRy1; // 4-B total

extern uint8_t BGXScroll, BGYScroll; // 2-B total
extern uint8_t FGXScroll, FGYScroll; // 2-B total
extern uint8_t LCDControl, LCDIcons; // 2-B total

extern uint8_t BTimerControl;
extern uint16_t HBTimerPeriod; // 2-B
extern uint16_t VBTimerPeriod; // 2-B

extern uint16_t HBCounter, VBCounter; // 2-B each, 4-B total
extern uint8_t VideoMode;

void WSwan_SetPixelFormat();

void WSwan_GfxInit(void);
void WSwan_GfxReset(void);
void WSwan_GfxWrite(uint32_t A, uint8_t V);
uint8_t WSwan_GfxRead(uint32_t A);
void WSwan_GfxWSCPaletteRAMWrite(uint32_t ws_offset, uint8_t data);

int wsExecuteLine(uint16_t *pXBuf, bool skip);

bool WSwan_GfxToggleLayer(int which);

#endif
