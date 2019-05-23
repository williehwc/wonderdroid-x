
package com.atelieryl.wonderdroid.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import com.atelieryl.wonderdroid.Button;

public class DrawThread extends Thread {
	
	private Canvas c;
	private Bitmap framebuffer;
	private Matrix scale;
	private Paint paint;
	private SurfaceHolder mSurfaceHolder;
	private Button[] buttons;
	private boolean showButtons;
	private boolean clearBeforeDraw;
	private boolean draw = false;
	private boolean running = true;
	
	public DrawThread(Bitmap framebuffer, Matrix scale, Paint paint) {
		this.framebuffer = framebuffer;
		this.scale = scale;
		this.paint = paint;
	}
	
	public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
		this.mSurfaceHolder = surfaceHolder;
	}
	
	public void setButtons(Button[] buttons) {
		this.buttons = buttons;
	}
	
	public void setShowButtons(boolean showButtons) {
		this.showButtons = showButtons;
	}
	
	public void setClearBeforeDraw(boolean clearBeforeDraw) {
		this.clearBeforeDraw = clearBeforeDraw;
	}

	public void setDraw() {
		draw = true;
	}

	public void clearRunning() {
		running = false;
	}
	
	@Override
    public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
		while (running) {
			if (draw) {
				c = null;
				try {
					while (c == null) {
						c = mSurfaceHolder.lockCanvas();
					}
					//boolean x = c.isHardwareAccelerated();
					if (clearBeforeDraw) {
						c.drawColor(Color.BLACK); // Make sure out-of-bounds areas remain black
					}
					c.drawBitmap(framebuffer, scale, paint);
					if (showButtons && buttons != null) {
						for (Button button : buttons) {
							c.drawBitmap(button.normal, button.drawrect, button.rect, null);
						}
					}
				} catch (Exception e) {

				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
				draw = false;
			}
		}
		synchronized (this) {
			notifyAll();
		}
	}
	
}