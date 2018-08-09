
package com.atelieryl.wonderdroid;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.SurfaceHolder;

import com.atelieryl.wonderdroid.utils.EmuThread;
import com.atelieryl.wonderdroid.utils.DrawRunnable;
import com.atelieryl.wonderdroid.utils.AudioRunnable;

@SuppressLint("NewApi")
public class WonderSwanRenderer implements EmuThread.Renderer {

    private AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC, WonderSwan.audiofreq,
            WonderSwan.channelconf, WonderSwan.encoding, AudioTrack.getMinBufferSize(
                    WonderSwan.audiofreq, WonderSwan.channelconf, WonderSwan.encoding) * 4,
            AudioTrack.MODE_STREAM);

    private Button[] buttons;

    private boolean showButtons = false;

    private final ShortBuffer frameone;

    private final Bitmap framebuffer;

    private final Matrix scale = new Matrix();

    private final Paint paint = new Paint();

    private final Paint textPaint = new Paint();
    
    private DrawRunnable drawRunnable;
    
    private AudioRunnable audioRunnable;
    
    private boolean surfaceHolderIsSet = false;

    public WonderSwanRenderer() {

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(35);
        textPaint.setShadowLayer(3, 1, 1, 0x99000000);
        textPaint.setAntiAlias(true);

        frameone = ByteBuffer.allocateDirect(WonderSwan.FRAMEBUFFERSIZE).asShortBuffer();
        framebuffer = Bitmap.createBitmap(WonderSwan.SCREEN_WIDTH, WonderSwan.SCREEN_HEIGHT,
                Bitmap.Config.RGB_565);
        
        drawRunnable = new DrawRunnable(framebuffer, scale, paint);
        audioRunnable = new AudioRunnable(audio);
    }

    @Override
    public void render(SurfaceHolder surfaceHolder) {
    	
        // c.drawARGB(0xff, 0, 0, 0);
    	if (!surfaceHolderIsSet) {
    		drawRunnable.setSurfaceHolder(surfaceHolder);
    		surfaceHolderIsSet = true;
    	}
    	drawRunnable.run();
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

    public Paint getPaint() {
        return paint;
    }
    
    public Bitmap getFrameBuffer() {
    	return framebuffer;
    }

    @Override
    public void start() {
        audio.play();
    }

    @Override
    public void update(boolean skip) {
        WonderSwan.execute_frame(frameone, skip);

        if (!skip) {
            frameone.rewind();
            framebuffer.copyPixelsFromBuffer(frameone);
            audioRunnable.run();
        }
    }

    @Override
    public void setButtons(Button[] buttons) {
        this.buttons = buttons;
        drawRunnable.setButtons(buttons);
    }

    @Override
    public void showButtons(boolean show) {
        this.showButtons = show;
        drawRunnable.setShowButtons(show);
    }
    
    public void setClearBeforeDraw(boolean clearBeforeDraw) {
    	drawRunnable.setClearBeforeDraw(clearBeforeDraw);
    }
}
