
package com.atelieryl.wonderdroid.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
//import android.graphics.Paint;
//import android.util.Log;
import android.graphics.Paint;
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
	private boolean draw = false;
	private boolean running = true;

	private int maskLeft = 0;
	private int maskTop = 0;
	private int maskRight = 0;
	private int maskBottom = 0;

	public DrawThread(Bitmap framebuffer, Matrix scale) {
		this.framebuffer = framebuffer;
		this.scale = scale;
  		this.paint = new Paint();
  		paint.setColor(Color.BLACK);
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

	public void setDraw() {
		draw = true;
	}

	public void clearRunning() {
		running = false;
	}

	public void setMask(int left, int top, int right, int bottom) {
		maskLeft = left;
		maskTop = top;
		maskRight = right;
		maskBottom = bottom;
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
					c.drawColor(Color.BLACK); // Make sure out-of-bounds areas remain black
					c.drawBitmap(framebuffer, scale, null);
					float width = (float) c.getWidth();
					float height = (float) c.getHeight();
					// Top mask
					if (maskTop > 0) {
						c.drawRect(0, 0, width, maskTop, paint);
					}
					// Left mask
					if (maskLeft > 0) {
						c.drawRect(0, 0, maskLeft, height, paint);
					}
					// Right mask
					if (maskRight > 0 && maskRight < width) {
						c.drawRect(maskRight, 0, width, height, paint);
					}
					// Bottom mask
					if (maskBottom > 0 && maskBottom < height) {
						c.drawRect(0, maskBottom, width, height, paint);
					}
					if (showButtons && buttons != null) {
						for (Button button : buttons) {
							if (button != null)
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