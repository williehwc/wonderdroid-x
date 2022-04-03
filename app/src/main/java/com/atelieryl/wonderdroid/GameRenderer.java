
package com.atelieryl.wonderdroid;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.SurfaceHolder;

import com.atelieryl.wonderdroid.utils.DrawThread;
import com.atelieryl.wonderdroid.utils.EmuThread;
import com.atelieryl.wonderdroid.utils.AudioRunnable;

@SuppressLint("NewApi")
public class GameRenderer implements EmuThread.Renderer {

    private AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC, WonderSwan.audiofreq,
            WonderSwan.channelconf, WonderSwan.encoding, AudioTrack.getMinBufferSize(
                    WonderSwan.audiofreq, WonderSwan.channelconf, WonderSwan.encoding) * 4,
            AudioTrack.MODE_STREAM);

    private Button[] buttons;

    private boolean showButtons = false;

    private final IntBuffer frameone;

    private final Bitmap framebuffer;

    private final Matrix scale = new Matrix();

//    private final Paint paint = new Paint();

    private final Paint textPaint = new Paint();
    
    private DrawThread drawThread;
    
    private AudioRunnable audioRunnable;
    
    private boolean surfaceHolderIsSet = false;

    private boolean clearBeforeDraw = true;

    public GameRenderer() {

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(35);
        textPaint.setShadowLayer(3, 1, 1, 0x99000000);
        textPaint.setAntiAlias(true);

        frameone = ByteBuffer.allocateDirect(WonderSwan.FRAMEBUFFERSIZE).asIntBuffer();
        framebuffer = Bitmap.createBitmap(WonderSwan.SCREEN_WIDTH, WonderSwan.SCREEN_HEIGHT,
                Bitmap.Config.ARGB_8888);
        
        drawThread = new DrawThread(framebuffer, scale);
        drawThread.start();

        audioRunnable = new AudioRunnable(audio);

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
    public void update(boolean skip) {
        WonderSwan.execute_frame(frameone, skip);
        audioRunnable.run();

        if (!skip) {
            frameone.rewind();
            framebuffer.copyPixelsFromBuffer(frameone);
        }
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
    
    public void setClearBeforeDraw(boolean clearBeforeDraw) {
        this.clearBeforeDraw = clearBeforeDraw;
        drawThread.setClearBeforeDraw(clearBeforeDraw);
    }

    public void restartDrawThread() {
        surfaceHolderIsSet = false;
        drawThread = new DrawThread(framebuffer, scale);
        drawThread.start();
        drawThread.setButtons(buttons);
        drawThread.setShowButtons(showButtons);
        drawThread.setClearBeforeDraw(clearBeforeDraw);
    }

    public void stopDrawThread() {
        drawThread.clearRunning();
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
}
