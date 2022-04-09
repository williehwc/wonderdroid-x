
package com.atelieryl.wonderdroid;

import java.nio.IntBuffer;

import com.atelieryl.wonderdroid.utils.CpuUtils;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

public class WonderSwan {

    private static final String TAG = WonderSwan.class.getSimpleName();

    static public final int SCREEN_WIDTH = 224;

    static public final int SCREEN_HEIGHT = 144;

    static public final int FRAMEBUFFERSIZE = (SCREEN_WIDTH * SCREEN_HEIGHT) * 4;

    static public boolean audioEnabled = true;

    static public int samples;
    
    static public short[] prevSamples;

    static final int audiobufferlen = 4096;

    static public short[] audiobuffer = new short[audiobufferlen];
    
    static public short[] workingaudiobuffer = new short[audiobufferlen];

    public static enum WonderSwanButton {
        Y1, Y4, Y2, Y3, X3, X4, X2, X1, A, B, START; // FIXME the is screen
                                                     // rendering order
        public boolean hardwareKeyDown = false;
        
        public boolean touchDown = false;

        public boolean down = false;

        public int keyCode = 0;
    };

    public static boolean buttonsDirty = false;

    public static final int channelconf = AudioFormat.CHANNEL_CONFIGURATION_STEREO;

    public static final int encoding = AudioFormat.ENCODING_PCM_16BIT;

    public static final int audiofreq = 24000;

    public WonderSwan() {
        throw new UnsupportedOperationException();
    }

    static {
        System.loadLibrary("mednafen");
    }

    static public native short[] load(String rom_path, String dir_path);

    static public native void reset();

    static public native void exit();

    /*static public void execute_frame(ShortBuffer framebuffer, boolean skipframe) {
        if (buttonsDirty) {
            WonderSwan.updatebuttons(WonderSwanButton.Y1.down, WonderSwanButton.Y2.down,
                    WonderSwanButton.Y3.down, WonderSwanButton.Y4.down, WonderSwanButton.X1.down,
                    WonderSwanButton.X2.down, WonderSwanButton.X3.down, WonderSwanButton.X4.down,
                    WonderSwanButton.A.down, WonderSwanButton.B.down, WonderSwanButton.START.down);
            buttonsDirty = false;
        }
        
        if (audioEnabled) {
	        for (int i = 0; i < samples * 2; i++) {
	        	audiobuffer[i] = audiobuffer[prevSamples * 2 + i];
	        }
        }
        
        prevSamples = samples;
        samples = _execute_frame(skipframe, audioEnabled, framebuffer, audioEnabled ? workingaudiobuffer
                : null);
        
        if (audioEnabled) {
	        for (int i = 0; i < samples * 2; i++) {
	        	audiobuffer[prevSamples * 2 + i] = workingaudiobuffer[i];
	        }
        }
        
        synchronized (audiobuffer) {
            audiobuffer.notify();
        }
    }*/

    static public short[] execute_frame(IntBuffer framebuffer, boolean skipframe) {
        if (buttonsDirty) {
            WonderSwan.updatebuttons(WonderSwanButton.Y1.down, WonderSwanButton.Y2.down,
                    WonderSwanButton.Y3.down, WonderSwanButton.Y4.down, WonderSwanButton.X1.down,
                    WonderSwanButton.X2.down, WonderSwanButton.X3.down, WonderSwanButton.X4.down,
                    WonderSwanButton.A.down, WonderSwanButton.B.down, WonderSwanButton.START.down);
            buttonsDirty = false;
        }

        short[] frameInfo = _execute_frame(skipframe, audioEnabled, framebuffer, audioEnabled ? audiobuffer : null);
        samples = frameInfo[0];
        synchronized (audiobuffer) {
            audiobuffer.notify();
        }
        return frameInfo;
    }

    static private native short[] _execute_frame(boolean skipframe, boolean audio,
            IntBuffer framebuffer, short[] audiobuffer);

    static public native void updatebuttons(boolean y1, boolean y2, boolean y3, boolean y4,
            boolean x1, boolean x2, boolean x3, boolean x4, boolean a, boolean b, boolean start);

    static public void outputDebugShizzle() {
        Log.d(TAG,
                "Audio buffer min " + AudioTrack.getMinBufferSize(audiofreq, channelconf, encoding));
    }

    public static native void savebackup(String filename);

    public static native void loadbackup(String filename);

    public static native void savestate(String filename);

    public static native void loadstate(String filename);
}
