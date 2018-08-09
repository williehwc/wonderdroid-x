#ifndef __MDFN_TYPES
#define __MDFN_TYPES

// Yes, yes, I know:  There's a better place for including config.h than here, but I'm tired, and this should work fine. :b
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <assert.h>
#include <stdint.h>

typedef void (*writefunc)(uint32_t A, uint8_t V);
typedef uint8_t (*readfunc)(uint32_t A);

//typedef uint32_t UTF32; /* at least 32 bits */
//typedef uint16_t UTF16; /* at least 16 bits */
//typedef uint8_t UTF8; /* typically 8 bits */

typedef unsigned char Boolean; /* 0 or 1 */

#ifndef FALSE
#define FALSE 0
#endif

#ifndef TRUE
#define TRUE 1
#endif

#undef require
#define require( expr ) assert( expr )

#define INT_TO_BCD(A)  (((A) / 10) * 16 + ((A) % 10))              // convert INT --> BCD
#define BCD_TO_INT(B)  (((B) / 16) * 10 + ((B) % 16))              // convert BCD --> INT
#define INT16_TO_BCD(A)  ((((((A) % 100) / 10) * 16 + ((A) % 10))) | (((((((A) / 100) % 100) / 10) * 16 + (((A) / 100) % 10))) << 8))   // convert INT16 --> BCD
#endif
