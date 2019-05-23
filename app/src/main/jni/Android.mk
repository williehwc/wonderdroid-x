LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -llog
LOCAL_CFLAGS   = -std=c99 -ftree-vectorizer-verbose=1 -ffast-math -finline-functions -funswitch-loops -fpredictive-commoning -fgcse-after-reload -ftree-vectorize -fipa-cp-clone
LOCAL_MODULE    := wonderswan
LOCAL_SRC_FILES := blip/Blip_Buffer.cpp wswan/sound.cpp wswan/tcache.c wswan/rtc.c wswan/gfx.c  wswan/memory.c wswan/eeprom.c wswan/interrupt.c wswan/v30mz.c wswan/jni.c
LOCAL_DISABLE_FORMAT_STRING_CHECKS := true

include $(BUILD_SHARED_LIBRARY)

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)	
	include $(CLEAR_VARS)
	LOCAL_ARM_MODE := arm     	
	LOCAL_LDLIBS := -llog
	    LOCAL_CFLAGS   = -std=c99 -ftree-vectorizer-verbose=1 -ffast-math -finline-functions -funswitch-loops -fpredictive-commoning -fgcse-after-reload -ftree-vectorize -fipa-cp-clone
		LOCAL_MODULE    := wonderswan-neon
		LOCAL_SRC_FILES := blip/Blip_Buffer.cpp wswan/sound.cpp wswan/tcache.c wswan/rtc.c wswan/gfx.c  wswan/memory.c wswan/eeprom.c wswan/interrupt.c wswan/v30mz.c wswan/jni.c
      	LOCAL_DISABLE_FORMAT_STRING_CHECKS := true
      	LOCAL_ARM_NEON  := true
      	include $(BUILD_SHARED_LIBRARY)
endif # TARGET_ARCH_ABI == armeabi-v7a

