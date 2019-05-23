
package com.atelieryl.wonderdroid;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class Button {

	public final Rect rect;
	public final Rect drawrect;
	public final Bitmap normal; // pressed;

	public Button (Drawable base, Paint textPaint, String text) {

		rect = new Rect(base.getBounds());
		drawrect = new Rect(0, 0, rect.width(), rect.height());
		normal = Bitmap.createBitmap(drawrect.width(), drawrect.height(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(normal);

		float textLen = (textPaint.measureText(text)) / 2;
		base.setBounds(drawrect);
		base.draw(canvas);
		canvas.drawText(text, (drawrect.left + drawrect.width() / 2) - textLen,
			(drawrect.top + drawrect.height() / 2) + (textPaint.getTextSize() / 3), textPaint);

		base.setBounds(rect);
	}

}
