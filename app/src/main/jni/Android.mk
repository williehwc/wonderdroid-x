TOP_PATH := $(call my-dir)
include $(CLEAR_VARS)

# libiconv

# LOCAL_PATH := $(TOP_PATH)/libiconv
# include $(CLEAR_VARS)
# LOCAL_MODULE := libiconv
# LOCAL_SRC_FILES := $(LOCAL_PATH)/lib/$(TARGET_ARCH_ABI)/libiconv.so
# include $(PREBUILT_SHARED_LIBRARY)

# libsndfile

LOCAL_PATH := $(TOP_PATH)/libsndfile
include $(CLEAR_VARS)
LOCAL_MODULE := libsndfile
LOCAL_SRC_FILES := $(LOCAL_PATH)/lib/$(TARGET_ARCH_ABI)/libsndfile.a
include $(PREBUILT_STATIC_LIBRARY)

# mednafen

LOCAL_PATH := $(TOP_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE := mednafen

LOCAL_SRC_FILES := \
    mednafen/ExtMemStream.cpp \
    mednafen/FileStream.cpp \
    mednafen/IPSPatcher.cpp \
    mednafen/MTStreamReader.cpp \
    mednafen/MemoryStream.cpp \
    mednafen/NativeVFS.cpp \
    mednafen/PSFLoader.cpp \
    mednafen/Stream.cpp \
    mednafen/time/Time_POSIX.cpp \
    mednafen/VirtualFS.cpp \
    mednafen/debug.cpp \
    mednafen/endian.cpp \
    mednafen/error.cpp \
    mednafen/file.cpp \
    mednafen/general.cpp \
    mednafen/git.cpp \
    mednafen/mednafen.cpp \
    mednafen/memory.cpp \
    mednafen/mempatcher.cpp \
    mednafen/movie.cpp \
    mednafen/netplay.cpp \
    mednafen/player.cpp \
    mednafen/qtrecord.cpp \
    mednafen/settings.cpp \
    mednafen/state.cpp \
    mednafen/state_rewind.cpp \
    mednafen/tests.cpp \
    mednafen/testsexp.cpp \
    mednafen/cdplay/cdplay.cpp \
    mednafen/cdrom/CDAFReader.cpp \
    mednafen/cdrom/CDAFReader_MPC.cpp \
    mednafen/cdrom/CDAFReader_PCM.cpp \
    mednafen/cdrom/CDAFReader_Vorbis.cpp \
    mednafen/cdrom/CDAccess.cpp \
    mednafen/cdrom/CDAccess_CCD.cpp \
    mednafen/cdrom/CDAccess_Image.cpp \
    mednafen/cdrom/CDInterface.cpp \
    mednafen/cdrom/CDInterface_MT.cpp \
    mednafen/cdrom/CDInterface_ST.cpp \
    mednafen/cdrom/CDUtility.cpp \
    mednafen/cdrom/crc32.cpp \
    mednafen/cdrom/galois.cpp \
    mednafen/cdrom/l-ec.cpp \
    mednafen/cdrom/lec.cpp \
    mednafen/cdrom/recover-raw.cpp \
    mednafen/cdrom/scsicd.cpp \
    mednafen/cheat_formats/gb.cpp \
    mednafen/cheat_formats/psx.cpp \
    mednafen/cheat_formats/snes.cpp \
    mednafen/compress/ArchiveReader.cpp \
    mednafen/compress/DecompressFilter.cpp \
    mednafen/compress/GZFileStream.cpp \
    mednafen/compress/ZIPReader.cpp \
    mednafen/compress/ZLInflateFilter.cpp \
    mednafen/compress/ZstdDecompressFilter.cpp \
    mednafen/cputest/cputest.c \
    mednafen/demo/demo.cpp \
    mednafen/hash/crc.cpp \
    mednafen/hash/md5.cpp \
    mednafen/hash/sha1.cpp \
    mednafen/hash/sha256.cpp \
    mednafen/hw_cpu/m68k/m68k.cpp \
    mednafen/hw_cpu/v810/v810_cpu.cpp \
    mednafen/hw_cpu/v810/v810_fp_ops.cpp \
    mednafen/hw_cpu/z80-fuse/z80.cpp \
    mednafen/hw_cpu/z80-fuse/z80_ops.cpp \
    mednafen/hw_misc/arcade_card/arcade_card.cpp \
    mednafen/hw_sound/pce_psg/pce_psg.cpp \
    mednafen/hw_sound/sms_apu/Sms_Apu.cpp \
    mednafen/hw_sound/ym2413/emu2413.cpp \
    mednafen/hw_video/huc6270/vdc.cpp \
    mednafen/lynx/c65c02.cpp \
    mednafen/lynx/cart.cpp \
    mednafen/lynx/memmap.cpp \
    mednafen/lynx/mikie.cpp \
    mednafen/lynx/ram.cpp \
    mednafen/lynx/rom.cpp \
    mednafen/lynx/susie.cpp \
    mednafen/lynx/system.cpp \
    mednafen/minilzo/minilzo.c \
    mednafen/mpcdec/crc32.c \
    mednafen/mpcdec/huffman.c \
    mednafen/mpcdec/mpc_bits_reader.c \
    mednafen/mpcdec/mpc_decoder.c \
    mednafen/mpcdec/mpc_demux.c \
    mednafen/mpcdec/requant.c \
    mednafen/mpcdec/streaminfo.c \
    mednafen/mpcdec/synth_filter.c \
    mednafen/mthreading/MThreading_POSIX.cpp \
    mednafen/ngp/T6W28_Apu.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_disassemble.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_disassemble_dst.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_disassemble_extra.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_disassemble_reg.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_disassemble_src.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_interpret.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_interpret_dst.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_interpret_reg.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_interpret_single.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_interpret_src.cpp \
    mednafen/ngp/TLCS-900h/TLCS900h_registers.cpp \
    mednafen/ngp/Z80_interface.cpp \
    mednafen/ngp/bios.cpp \
    mednafen/ngp/biosHLE.cpp \
    mednafen/ngp/dma.cpp \
    mednafen/ngp/flash.cpp \
    mednafen/ngp/gfx.cpp \
    mednafen/ngp/gfx_scanline_colour.cpp \
    mednafen/ngp/gfx_scanline_mono.cpp \
    mednafen/ngp/interrupt.cpp \
    mednafen/ngp/mem.cpp \
    mednafen/ngp/neopop.cpp \
    mednafen/ngp/rom.cpp \
    mednafen/ngp/rtc.cpp \
    mednafen/ngp/sound.cpp \
    mednafen/pce/hes.cpp \
    mednafen/pce/huc.cpp \
    mednafen/pce/huc6280.cpp \
    mednafen/pce/input.cpp \
    mednafen/pce/input/gamepad.cpp \
    mednafen/pce/input/mouse.cpp \
    mednafen/pce/input/tsushinkb.cpp \
    mednafen/pce/mcgenjin.cpp \
    mednafen/pce/pce.cpp \
    mednafen/pce/pcecd.cpp \
    mednafen/pce/tsushin.cpp \
    mednafen/pce/vce.cpp \
    mednafen/pcfx/fxscsi.cpp \
    mednafen/pcfx/huc6273.cpp \
    mednafen/pcfx/idct.cpp \
    mednafen/pcfx/input.cpp \
    mednafen/pcfx/input/gamepad.cpp \
    mednafen/pcfx/input/mouse.cpp \
    mednafen/pcfx/interrupt.cpp \
    mednafen/pcfx/king.cpp \
    mednafen/pcfx/pcfx.cpp \
    mednafen/pcfx/rainbow.cpp \
    mednafen/pcfx/soundbox.cpp \
    mednafen/pcfx/timer.cpp \
    mednafen/psx/cdc.cpp \
    mednafen/psx/cpu.cpp \
    mednafen/psx/dis.cpp \
    mednafen/psx/dma.cpp \
    mednafen/psx/frontio.cpp \
    mednafen/psx/gpu.cpp \
    mednafen/psx/gpu_line.cpp \
    mednafen/psx/gpu_polygon.cpp \
    mednafen/psx/gpu_sprite.cpp \
    mednafen/psx/gte.cpp \
    mednafen/psx/input/dualanalog.cpp \
    mednafen/psx/input/dualshock.cpp \
    mednafen/psx/input/gamepad.cpp \
    mednafen/psx/input/guncon.cpp \
    mednafen/psx/input/justifier.cpp \
    mednafen/psx/input/memcard.cpp \
    mednafen/psx/input/mouse.cpp \
    mednafen/psx/input/multitap.cpp \
    mednafen/psx/input/negcon.cpp \
    mednafen/psx/irq.cpp \
    mednafen/psx/mdec.cpp \
    mednafen/psx/psx.cpp \
    mednafen/psx/sio.cpp \
    mednafen/psx/spu.cpp \
    mednafen/psx/timer.cpp \
    mednafen/quicklz/quicklz.c \
    mednafen/resampler/resample.c \
    mednafen/sms/cart.cpp \
    mednafen/sms/memz80.cpp \
    mednafen/sms/pio.cpp \
    mednafen/sms/render.cpp \
    mednafen/sms/romdb.cpp \
    mednafen/sms/sms.cpp \
    mednafen/sms/sound.cpp \
    mednafen/sms/system.cpp \
    mednafen/sms/tms.cpp \
    mednafen/sms/vdp.cpp \
    mednafen/sound/Blip_Buffer.cpp \
    mednafen/sound/DSPUtility.cpp \
    mednafen/sound/Fir_Resampler.cpp \
    mednafen/sound/OwlResampler.cpp \
    mednafen/sound/Stereo_Buffer.cpp \
    mednafen/sound/SwiftResampler.cpp \
    mednafen/sound/WAVRecord.cpp \
    mednafen/sound/okiadpcm.cpp \
    mednafen/string/escape.cpp \
    mednafen/string/string.cpp \
    mednafen/tremor/bitwise.c \
    mednafen/tremor/block.c \
    mednafen/tremor/codebook.c \
    mednafen/tremor/floor0.c \
    mednafen/tremor/floor1.c \
    mednafen/tremor/framing.c \
    mednafen/tremor/info.c \
    mednafen/tremor/mapping0.c \
    mednafen/tremor/mdct.c \
    mednafen/tremor/registry.c \
    mednafen/tremor/res012.c \
    mednafen/tremor/sharedbook.c \
    mednafen/tremor/synthesis.c \
    mednafen/tremor/vorbisfile.c \
    mednafen/tremor/window.c \
    mednafen/trio/trio.c \
    mednafen/trio/trionan.c \
    mednafen/trio/triostr.c \
    mednafen/vb/input.cpp \
    mednafen/vb/timer.cpp \
    mednafen/vb/vb.cpp \
    mednafen/vb/vip.cpp \
    mednafen/vb/vsu.cpp \
    mednafen/video/convert.cpp \
    mednafen/video/Deinterlacer.cpp \
    mednafen/video/Deinterlacer_Blend.cpp \
    mednafen/video/Deinterlacer_Simple.cpp \
    mednafen/video/font-data.cpp \
    mednafen/video/png.cpp \
    mednafen/video/primitives.cpp \
    mednafen/video/resize.cpp \
    mednafen/video/surface.cpp \
    mednafen/video/tblur.cpp \
    mednafen/video/text.cpp \
    mednafen/video/video.cpp \
    mednafen/wswan/comm.cpp \
    mednafen/wswan/dis/dis_decode.cpp \
    mednafen/wswan/dis/dis_groups.cpp \
    mednafen/wswan/dis/resolve.cpp \
    mednafen/wswan/dis/syntax.cpp \
    mednafen/wswan/eeprom.cpp \
    mednafen/wswan/gfx.cpp \
    mednafen/wswan/interrupt.cpp \
    mednafen/wswan/main.cpp \
    mednafen/wswan/memory.cpp \
    mednafen/wswan/rtc.cpp \
    mednafen/wswan/sound.cpp \
    mednafen/wswan/tcache.cpp \
    mednafen/wswan/v30mz.cpp \
    mednafen/zstd/common/entropy_common.c \
    mednafen/zstd/common/error_private.c \
    mednafen/zstd/common/fse_decompress.c \
    mednafen/zstd/common/xxhash.c \
    mednafen/zstd/common/zstd_common.c \
    mednafen/zstd/decompress/huf_decompress.c \
    mednafen/zstd/decompress/zstd_ddict.c \
    mednafen/zstd/decompress/zstd_decompress.c \
    mednafen/zstd/decompress/zstd_decompress_block.c \
    stubs.cpp \
    com_atelieryl_wonderdroid_WonderSwan.cpp

LOCAL_C_INCLUDES := \
    libsndfile \
#     libiconv

LOCAL_CFLAGS := \
    -fwrapv \
    -DHAVE_MKDIR \
    -DMEDNAFEN_VERSION=\"1.26.1\" \
    -DPACKAGE=\"mednafen\" \
    -DMEDNAFEN_VERSION_NUMERIC=0x00102900 \
    -DPSS_STYLE=1 \
    -DMPC_FIXED_POINT \
    -DWANT_NGP_EMU \
    -DWANT_PCE_EMU \
    -DWANT_SMS_EMU \
    -DWANT_WSWAN_EMU \
    -DSTDC_HEADERS \
    -DICONV_CONST= \
    -DLSB_FIRST \
    -D__STDC_LIMIT_MACROS \
    -DSIZEOF_CHAR=1 \
    -DSIZEOF_SHORT=2 \
    -DSIZEOF_INT=4 \
    -DSIZEOF_LONG=8 \
    -DSIZEOF_LONG_LONG=8 \
    -DSIZEOF_OFF_T=8 \
    -DSIZEOF_PTRDIFF_T=8 \
    -DSIZEOF_SIZE_T=8 \
    -DSIZEOF_VOID_P=8 \
    -DSIZEOF_DOUBLE=8 \
    -DHAVE_LIBSNDFILE \
    -DMDFN_PCE_VCE_AWESOMEMODE \
    -mllvm \
    -disable-lsr \
    -fexceptions \
    -frtti \
    -fsigned-char \
    -std=gnu99 \
    -O3
#    -O0
#    -DTRIO_PLATFORM_WINCE

LOCAL_CPPFLAGS := \
    -std=gnu++11

LOCAL_LDLIBS := -lz -llog

# LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
## LOCAL_LDFLAGS := -Wl,--no-fatal-warnings

# LOCAL_SHARED_LIBRARIES := libiconv

LOCAL_STATIC_LIBRARIES := libsndfile

include $(BUILD_SHARED_LIBRARY)
