
package com.atelieryl.wonderdroid;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.SurfaceHolder;

import com.atelieryl.wonderdroid.utils.DrawThread;
import com.atelieryl.wonderdroid.utils.EmuThread;

@SuppressLint("NewApi")
public class GameRenderer implements EmuThread.Renderer {

    private AudioTrack audio;

    private Button[] buttons;

    private boolean showButtons = false;

    private final IntBuffer frameone;

    private final Bitmap framebuffer;

    private final Matrix scale = new Matrix();

//    private final Paint paint = new Paint();

    private final Paint textPaint = new Paint();
    
    private DrawThread drawThread;

    private boolean surfaceHolderIsSet = false;

    private boolean stretchToFill = false;

    private static final int BYTES_PER_PX = 4;

    private int mSoundChan;

    private boolean scaleGenerated;

    private int mNominalWidth;
    private int mNominalHeight;

    private double mScaling;
    private int mSharpness;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private boolean mPortrait;
    private boolean mMask;

    private int mLastX;
    private int mLastY;
    private int mLastW;
    private int mLastH;

    public GameRenderer(long[] gameInfo, int sharpness, boolean portrait) {

        mNominalWidth = (int) gameInfo[1];
        mNominalHeight = (int) gameInfo[2];

        int fbWidth = (int) gameInfo[3];
        int fbHeight = (int) gameInfo[4];

        int soundChan = (int) gameInfo[5];

        mSharpness = sharpness;

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(35);
        textPaint.setShadowLayer(3, 1, 1, 0x99000000);
        textPaint.setAntiAlias(true);

        frameone = ByteBuffer.allocateDirect(fbWidth * fbHeight * BYTES_PER_PX).asIntBuffer();
        framebuffer = Bitmap.createBitmap(fbWidth, fbHeight, Bitmap.Config.ARGB_8888);
        
        drawThread = new DrawThread(framebuffer, scale);
        drawThread.start();

        int channelConf = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        if (soundChan == 1) {
            channelConf = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        }
        mSoundChan = soundChan;

        audio = new AudioTrack(AudioManager.STREAM_MUSIC, WonderSwan.audiofreq,
                channelConf, WonderSwan.encoding, AudioTrack.getMinBufferSize(
                WonderSwan.audiofreq, channelConf, WonderSwan.encoding) * 4,
                AudioTrack.MODE_STREAM);

        mPortrait = portrait;
    }

    @Override
    public void render(SurfaceHolder surfaceHolder) {
    	
        // c.drawARGB(0xff, 0, 0, 0);
    	if (!surfaceHolderIsSet) {
            drawThread.setSurfaceHolder(surfaceHolder);
    		surfaceHolderIsSet = true;
    	}
    	drawThread.setDraw();
        // c.drawBitmap(framebuffer, scale, paint);
        // c.drawBitmap(framebuffer, 0, 0, null);

        /*if (showButtons && buttons != null) {
            for (Button button : buttons) {
                c.drawBitmap(button.normal, button.drawrect, button.rect, null);
            }
        }*/

    }

    public Matrix getMatrix() {
        return scale;
    }

//    public Paint getPaint() {
//        return paint;
//    }
    
//    public Bitmap getFrameBuffer() {
//    	return framebuffer;
//    }

    @Override
    public void start() {
        try {
            audio.play();
        } catch (Exception e) {

        }
    }

    @Override
    public int update(boolean skip) {
        int[] frameInfo = WonderSwan.execute_frame(frameone, skip);
        if (frameInfo == null) return 0;
        audio.write(WonderSwan.audiobuffer, 0, WonderSwan.samples * mSoundChan);
        if (!scaleGenerated || frameInfo[1] != mLastX || frameInfo[2] != mLastY || frameInfo[3] != mLastW || frameInfo[4] != mLastH) {
            scale.reset();
            if (frameInfo[4] > 0) {
                scale.postScale((float) mNominalWidth / frameInfo[3], (float) mNominalHeight / frameInfo[4]);
            }
            scale.postTranslate(-frameInfo[1], -frameInfo[2]);
            float sx;
            float sy;
            float dx;
            float dy;
            if (mPortrait) {
                sx = mSharpness * (float) mScaling;
                sy = mSharpness * (float) mScaling;
                dx = (mSurfaceWidth - mNominalWidth * mSharpness * (float) mScaling) / 2;
                dy = 10 * mSharpness;
            } else if (stretchToFill) {
                sx = (float) mScaling * mSurfaceWidth / mNominalWidth;
                sy = mSharpness * (float) mScaling;
                dx = (mSurfaceWidth - mSurfaceWidth * (float) mScaling) / 2;
                dy = (mSurfaceHeight - mNominalHeight * mSharpness * (float) mScaling) / 2;
            } else {
                sx = mSharpness * (float) mScaling;
                sy = mSharpness * (float) mScaling;
                dx = (mSurfaceWidth - mNominalWidth * mSharpness * (float) mScaling) / 2;
                dy = (mSurfaceHeight - mNominalHeight * mSharpness * (float) mScaling) / 2;
            }
            scale.postScale(sx, sy);
            scale.postTranslate(dx, dy);
            if (mMask) {
                drawThread.setMask((int) dx, (int) dy, (int) (sx * mNominalWidth + dx), (int) (sy * mNominalHeight + dy));
            } else {
                drawThread.setMask(0, 0, 0, 0);
            }
            scaleGenerated = true;
            mLastX = frameInfo[1];
            mLastY = frameInfo[2];
            mLastW = frameInfo[3];
            mLastH = frameInfo[4];
        }
        if (!skip) {
            frameone.rewind();
            framebuffer.copyPixelsFromBuffer(frameone);
        }
        return frameInfo[5];
    }

    @Override
    public void setButtons(Button[] buttons) {
        this.buttons = buttons;
        drawThread.setButtons(buttons);
    }

    @Override
    public void showButtons(boolean show) {
        this.showButtons = show;
        drawThread.setShowButtons(show);
    }
    
    public void setStretchToFill(boolean stretchToFill) {
        this.stretchToFill = stretchToFill;
        this.scaleGenerated = false;
    }

    public void restartDrawThread() {
        surfaceHolderIsSet = false;
        drawThread = new DrawThread(framebuffer, scale);
        drawThread.start();
        drawThread.setButtons(buttons);
        drawThread.setShowButtons(showButtons);
    }

    public void stopDrawThread() {
        drawThread.clearRunning();
    }

    public void setScaling(double scaling) {
        mScaling = scaling;
        scaleGenerated = false;
    }

    public void updateSurfaceDimens(int surfaceWidth, int surfaceHeight) {
        mSurfaceWidth = surfaceWidth;
        mSurfaceHeight = surfaceHeight;
    }

    public void setVolume(int volume) {
        float volumeFloat = (float) volume / 100;
        if (volumeFloat > 1) {
            volumeFloat = 1;
        } else if (volumeFloat < 0) {
            volumeFloat = 0;
        }
        audio.setVolume(volumeFloat);
    }

    public void setMask(boolean mask) {
        mMask = mask;
        scaleGenerated = false;
    }
}
