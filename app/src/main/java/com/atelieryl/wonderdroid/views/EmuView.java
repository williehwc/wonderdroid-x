
package com.atelieryl.wonderdroid.views;

import com.atelieryl.wonderdroid.Button;
import com.atelieryl.wonderdroid.TouchInputHandler;
import com.atelieryl.wonderdroid.TouchInputHandler.Pointer;
import com.atelieryl.wonderdroid.WonderSwan;
import com.atelieryl.wonderdroid.WonderSwan.WonderSwanButton;
import com.atelieryl.wonderdroid.GameRenderer;
import com.atelieryl.wonderdroid.utils.EmuThread;
import com.atelieryl.wonderdroid.VibrateTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class EmuView extends SurfaceView implements SurfaceHolder.Callback {

	private final static String TAG = EmuView.class.getSimpleName();
	@SuppressWarnings("unused")
	private final static boolean debug = true;
	private boolean mPaused = false;

	private EmuThread mThread;
	private final GameRenderer renderer;
	private boolean controlsVisible = false;
	private GradientDrawable[] buttons;
	private final TouchInputHandler inputHandler;
	private static Context mContext;
	
	private float actualWidthToDrawnWidthRatio = 0;
	private float actualHeightToDrawnHeightRatio = 0;
	private float widthToHeightRatio = 0;
	
	private boolean started = false;
	private int sharpness = 3;
	private boolean stretchToFill = false;
	private double scaling = 1;
	
	static int vibratedown = 5;
	static int vibrateup = 1;

	private int width = 0;
	private int height = 0;

	private int mNominalWidth = 0;
	private int mNominalHeight = 0;
	private int mFps;
	private char mSystem;

	private float postscale;

	private static final float[] NEGATIVE = {
			-1.0f,     0,     0,    0, 255, // red
			0, -1.0f,     0,    0, 255, // green
			0,     0, -1.0f,    0, 255, // blue
			0,     0,     0, 1.0f,   0  // alpha
	};

	SurfaceHolder mHolder = null;

	public void setKeyCodes (int start, int a, int b, int x1, int x2, int x3, int x4, int y1, int y2, int y3, int y4) {
		WonderSwanButton.START.keyCode = start;
		WonderSwanButton.A.keyCode = a;
		WonderSwanButton.B.keyCode = b;
		WonderSwanButton.X1.keyCode = x1;
		WonderSwanButton.X2.keyCode = x2;
		WonderSwanButton.X3.keyCode = x3;
		WonderSwanButton.X4.keyCode = x4;
		WonderSwanButton.Y1.keyCode = y1;
		WonderSwanButton.Y2.keyCode = y2;
		WonderSwanButton.Y3.keyCode = y3;
		WonderSwanButton.Y4.keyCode = y4;
	}

	public EmuView (Context context, int[] gameInfo) {
		super(context);

		mFps = gameInfo[0];
		mNominalWidth = gameInfo[1];
		mNominalHeight = gameInfo[2];
		mSystem = (char) gameInfo[7];

		this.mContext = context;

		inputHandler = new TouchInputHandler(context);

		setZOrderOnTop(true); // FIXME any advantage to this?

		SurfaceHolder holder = this.getHolder();
		holder.addCallback(this);

		renderer = new GameRenderer(gameInfo, sharpness);
		renderer.setScaling(scaling);
		mThread = new EmuThread(renderer, mFps);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		sharpness = Integer.parseInt(prefs.getString("sharpness", "3"));
		mThread.setFrameskip(Integer.parseInt(prefs.getString("frameskip", "0")));
		stretchToFill = prefs.getBoolean("stretchtofill", false);
		scaling = (prefs.getInt("scaling", 95) + 5) / 100.;

		renderer.setClearBeforeDraw(!stretchToFill);

		vibratedown = Integer.parseInt(prefs.getString("vibratedown", "5"));
		vibrateup = Integer.parseInt(prefs.getString("vibrateup", "1"));
	}

	@Override
	public void surfaceChanged (SurfaceHolder holder, int format, int w, int h) {
		if (actualHeightToDrawnHeightRatio == 0 || actualHeightToDrawnHeightRatio == 1) {
			if (widthToHeightRatio == 0)
				widthToHeightRatio = (float) this.getWidth() / (float) this.getHeight();
			int newWidth = mNominalWidth * sharpness;
			int newHeight = mNominalHeight * sharpness;
			if (this.getWidth() > this.getHeight()) {
				// Device in landscape
				newWidth = (int) (newHeight * widthToHeightRatio);
			} else {
				// Device in portrait
				newHeight = (int) (newWidth / widthToHeightRatio);
			}
//			if (!stretchToFill) {
//				while ((float) newWidth / (float) newHeight < widthToHeightRatio) newWidth++;
//			}
			actualWidthToDrawnWidthRatio = (float) w / (float) newWidth;
			actualHeightToDrawnHeightRatio = (float) h / (float) newHeight;
			width = newWidth;
			height = newHeight;
			renderer.updateSurfaceDimens(width, height);
			
			makeButtons(PreferenceManager.getDefaultSharedPreferences(mContext));

			postscale = (float)width / (float)mNominalWidth;

			if (mNominalHeight * postscale > height) {
				postscale = (float)height / (float)mNominalHeight;

			}

//			rescale();
		}
		mHolder = holder;
	}

	@Override
	public void surfaceCreated (SurfaceHolder holder) {
		holder.setFormat(PixelFormat.RGB_565);
		if (widthToHeightRatio == 0)
			widthToHeightRatio = (float) this.getWidth() / (float) this.getHeight();
		int holderWidth = mNominalWidth * sharpness;
		int holderHeight = mNominalHeight * sharpness;
		if (this.getWidth() > this.getHeight()) {
			// Device in landscape
			holderWidth = (int) (holderHeight * widthToHeightRatio);
		} else {
			// Device in portrait
			holderHeight = (int) (holderWidth / widthToHeightRatio);
		}
//		if (!stretchToFill) {
//			while ((float) holderWidth / (float) holderHeight < widthToHeightRatio) holderWidth++;
//		}
		holder.setFixedSize(holderWidth, holderHeight);
		mHolder = holder;
		mThread.setSurfaceHolder(holder);
	}

	@Override
	public void surfaceDestroyed (SurfaceHolder holder) {
		mHolder = null;
		//mThread.clearRunning();
	}

//	public void rescale () {
//		Matrix scale = renderer.getMatrix();
//
//		scale.reset();
//		scale.postScale(sharpness * (float) scaling, sharpness * (float) scaling);
//		scale.postTranslate((width - mNominalWidth * sharpness * (float) scaling) / 2,
//				(height - mNominalHeight * sharpness * (float) scaling) / 2);
//	}

	public void start () {
		Log.d(TAG, "emulation started");
		mThread.setRunning();
		mThread.start();
		started = true;
	}

	public void togglepause () {
		if (mPaused) {
			mPaused = false;
			mThread.unpause();
		} else {
			mPaused = true;
			mThread.pause();
		}
	}
	
	/*public void pause () {
		mPaused = true;
		mThread.pause();
	}
	
	public void unpause() {
		mPaused = false;
		mThread.unpause();
	}*/

	public void onResume () {
		if (started) {
			renderer.restartDrawThread();
			mThread = new EmuThread(renderer, mFps);
			start();
		}
		mThread.setSurfaceHolder(mHolder);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mThread.setFrameskip(Integer.parseInt(prefs.getString("frameskip", "0")));
		scaling = (prefs.getInt("scaling", 95) + 5) / 100.;
		vibratedown = Integer.parseInt(prefs.getString("vibratedown", "5"));
		vibrateup = Integer.parseInt(prefs.getString("vibrateup", "1"));
		if (width > 0 && height > 0) {
			makeButtons(prefs);
		}
		renderer.setScaling(scaling);
		renderer.setVolume(prefs.getInt("volume", 100));
//		rescale();
	}

	public void stop () {

		if (mThread.isRunning()) {
			Log.d(TAG, "shutting down emulation");

			mThread.clearRunning();
			renderer.stopDrawThread();

			synchronized (mThread) {
				try {
					mThread.wait(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}

		}
	}

	public void makeButtons(SharedPreferences prefs) {
		buttons = new GradientDrawable[WonderSwanButton.values().length];
		int buttonBaseId = mContext.getResources().getIdentifier(
				"button_" + prefs.getString("opacity", "4"), "drawable", mContext.getPackageName());
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = (GradientDrawable)getResources().getDrawable(buttonBaseId);
			if (prefs.getString("buttoncolor", "black").equals("white"))
				buttons[i].setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
		}

		// Fix for gesture conflicts: https://stackoverflow.com/questions/65056838
		if (Build.VERSION.SDK_INT >= 29) {
			List<Rect> exclusionRects = new ArrayList<>();
			exclusionRects.add(new Rect(0, 0, 200, 2048));
			this.setSystemGestureExclusionRects(exclusionRects);
		}

		int defaultSpacing = -1 * height / 50;
		int defaultButtonsize = (int)(height / 6.7);
		for (int i = 0; i < buttons.length; i++) {
			int spacing = 0;
			int buttonsize = 0;
			switch (i) {
				case 0:
				case 1:
				case 2:
				case 3:
					spacing = (int) ((prefs.getInt("size_y", 20) + 5) / 25. * defaultSpacing);
					buttonsize = (int) ((prefs.getInt("size_y", 20) + 5) / 25. * defaultButtonsize);
					break;
				case 4:
				case 5:
				case 6:
				case 7:
					spacing = (int) ((prefs.getInt("size_x", 20) + 5) / 25. * defaultSpacing);
					buttonsize = (int) ((prefs.getInt("size_x", 20) + 5) / 25. * defaultButtonsize);
					break;
				case 8:
				case 9:
					spacing = (int) ((prefs.getInt("size_ab", 20) + 5) / 25. * defaultSpacing);
					buttonsize = (int) ((prefs.getInt("size_ab", 20) + 5) / 25. * defaultButtonsize);
					break;
				case 10:
					spacing = (int) ((prefs.getInt("size_start", 20) + 5) / 25. * defaultSpacing);
					buttonsize = (int) ((prefs.getInt("size_start", 20) + 5) / 25. * defaultButtonsize);
					break;
			}

			buttons[i].setSize(buttonsize, buttonsize);

			int updownleft = buttonsize + spacing;
			int updownright = buttonsize + buttonsize + spacing;
			int bottomrowtop = height - buttonsize;

			int marginTop = (int) ((float) prefs.getInt("margin_top", 0) * postscale);
			int marginLeft = (int) ((float) prefs.getInt("margin_left", 15) * postscale);
			int marginRight = (int) ((float) prefs.getInt("margin_right", 15) * postscale);
			int marginBottom = (int) ((float) prefs.getInt("margin_bottom", 0) * postscale);

			switch (i) {
				// Y
				case 0: //up
					buttons[i].setBounds(marginLeft + updownleft, marginTop, marginLeft + updownright, marginTop + buttonsize);
					break;
				case 1: //left
					buttons[i].setBounds(marginLeft, marginTop + buttonsize + spacing, marginLeft + buttonsize, marginTop + (buttonsize * 2) + spacing);
					break;
				case 2: //right
					buttons[i].setBounds(marginLeft + 2 * (buttonsize + spacing), marginTop + buttonsize + spacing, marginLeft + buttonsize + 2 * (buttonsize + spacing), marginTop + (buttonsize * 2)
							+ spacing);
					break;
				case 3: //down
					buttons[i].setBounds(marginLeft + updownleft, marginTop + (buttonsize * 2) + (spacing * 2), marginLeft + updownright, marginTop + (buttonsize * 3) + (spacing * 2));
					break;
				// X
				case 4:
					buttons[i].setBounds(marginLeft + updownleft, height - buttonsize - marginBottom, marginLeft + updownright, height - marginBottom);
					break;
				case 5:
					buttons[i].setBounds(marginLeft, height - (buttonsize * 2) - spacing - marginBottom, marginLeft + buttonsize, height - buttonsize - spacing - marginBottom);
					break;
				case 6:
					buttons[i].setBounds(marginLeft + 2 * (buttonsize + spacing), height - (buttonsize * 2) - spacing - marginBottom, marginLeft + buttonsize + 2 * (buttonsize + spacing), height
							- buttonsize - spacing - marginBottom);
					break;
				case 7:
					buttons[i].setBounds(marginLeft + updownleft, (height - (buttonsize * 3)) - (2 * spacing) - marginBottom, marginLeft + updownright,
							(height - (buttonsize * 2)) - (2 * spacing) - marginBottom);
					break;
				// A,B
				case 8:
					if (!prefs.getBoolean("swapab", false)) {
						if (prefs.getString("abposition", "sidebyside").equals("sidebyside"))
							buttons[i].setBounds(width - buttonsize - marginRight, bottomrowtop - marginBottom, width - marginRight, height - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("topbottom"))
							buttons[i].setBounds(width - buttonsize - marginRight, height - (buttonsize * 2) + spacing - marginBottom, width - marginRight, height - buttonsize + spacing - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("diagonal"))
							buttons[i].setBounds(width - buttonsize - marginRight, height - (buttonsize * 2) - spacing - marginBottom, width - marginRight, height - buttonsize - spacing - marginBottom);
					} else {
						if (prefs.getString("abposition", "sidebyside").equals("sidebyside"))
							buttons[i].setBounds(width - (buttonsize * 2) + spacing * 2 - marginRight, bottomrowtop - marginBottom, (width - buttonsize) + spacing * 2 - marginRight, height - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("topbottom"))
							buttons[i].setBounds(width - buttonsize - marginRight, bottomrowtop - marginBottom, width - marginRight, height - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("diagonal"))
							buttons[i].setBounds(width - (buttonsize * 2) - spacing - marginRight, height - buttonsize - marginBottom, width - buttonsize - spacing - marginRight, height - marginBottom);
					}
					break;
				case 9:
					if (!prefs.getBoolean("swapab", false)) {
						if (prefs.getString("abposition", "sidebyside").equals("sidebyside"))
							buttons[i].setBounds(width - (buttonsize * 2) + spacing * 2 - marginRight, bottomrowtop - marginBottom, (width - buttonsize) + spacing * 2 - marginRight, height - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("topbottom"))
							buttons[i].setBounds(width - buttonsize - marginRight, bottomrowtop - marginBottom, width - marginRight, height - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("diagonal"))
							buttons[i].setBounds(width - (buttonsize * 2) - spacing - marginRight, height - buttonsize - marginBottom, width - buttonsize - spacing - marginRight, height - marginBottom);
					} else {
						if (prefs.getString("abposition", "sidebyside").equals("sidebyside"))
							buttons[i].setBounds(width - buttonsize - marginRight, bottomrowtop - marginBottom, width - marginRight, height - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("topbottom"))
							buttons[i].setBounds(width - buttonsize - marginRight, height - (buttonsize * 2) + spacing - marginBottom, width - marginRight, height - buttonsize + spacing - marginBottom);
						else if (prefs.getString("abposition", "sidebyside").equals("diagonal"))
							buttons[i].setBounds(width - buttonsize - marginRight, height - (buttonsize * 2) - spacing - marginBottom, width - marginRight, height - buttonsize - spacing - marginBottom);
					}
					break;
				// Start
				case 10:
					buttons[i].setSize(buttonsize * 2, buttonsize);
					if (prefs.getBoolean("relocate", false)) {
						buttons[i].setBounds(width - buttonsize * 2 - marginRight, marginTop, width - marginRight, marginTop + buttonsize);
					} else {
						buttons[i].setBounds((width / 2) - buttonsize, bottomrowtop - marginBottom, (width / 2) + buttonsize, height - marginBottom);
					}
					break;
			}
		}

		Button[] buts = new Button[buttons.length];

		if (buttons != null) {
			Paint textPaint = new Paint();
			textPaint.setAntiAlias(true);
			textPaint.setTextSize(height / 30);

			if (prefs.getBoolean("showbuttonlabels", true)) {
				if (prefs.getString("buttoncolor", "black").equals("white")) {
					textPaint.setColor(0xFF000000);
				} else {
					textPaint.setColor(0xFFFFFFFF);
					textPaint.setShadowLayer(1, 1, 1, 0x99000000);
				}
			} else {
				textPaint.setColor(0x00000000);
			}


			for (int i = 0; i < buttons.length; i++) {
				// Skip button if system lacks it
				if (mSystem != 'w' && i <= 3) {
					// Y buttons are only on WonderSwan
					buttons[i] = null;
					continue;
				}
				String buttonLabel = WonderSwanButton.values()[i].name();
				if (i == 8) {
					if (mSystem == 'g') {
						buttonLabel = "2";
					} else if (mSystem == 'n') {
						buttonLabel = "B";
					} else if (mSystem == 'p') {
						buttonLabel = "I";
					}
				} else if (i == 9) {
					if (mSystem == 'g') {
						buttonLabel = "1";
					} else if (mSystem == 'n') {
						buttonLabel = "A";
					} else if (mSystem == 'p') {
						buttonLabel = "II";
					}
				} else if (i == 4) {
					if (mSystem != 'w') {
						buttonLabel = "▽";
					}
				} else if (i == 5) {
					if (mSystem != 'w') {
						buttonLabel = "◁";
					}
				} else if (i == 6) {
					if (mSystem != 'w') {
						buttonLabel = "▷";
					}
				} else if (i == 7) {
					if (mSystem != 'w') {
						buttonLabel = "△";
					}
				}
				buts[i] = new Button(buttons[i], textPaint, buttonLabel);
			}
		}

		renderer.setButtons(buts);
	}

	public static void changeButton (WonderSwanButton button, boolean newState, boolean touch) {
		if (touch) {
			if (newState && !button.touchDown) {
				if (vibratedown > 0) new VibrateTask(mContext).execute(vibratedown);
				button.touchDown = newState;
				WonderSwan.buttonsDirty = true;
			}
			if (!newState && button.touchDown) {
				if (vibrateup > 0) new VibrateTask(mContext).execute(vibrateup);
				button.touchDown = newState;
				WonderSwan.buttonsDirty = true;
			}
		} else {
			// This function is called only when any key state has changed, so no need to check
			button.hardwareKeyDown = newState;
			WonderSwan.buttonsDirty = true;
		}
		button.down = (button.touchDown || button.hardwareKeyDown);
	}

	public EmuThread getThread () {
		return mThread;
	}

	public EmuThread.Renderer getRenderer () {
		return renderer;
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {

		if (!controlsVisible) {
			return false;
		}

		inputHandler.onTouchEvent(event);

		boolean[] buttonStates = new boolean[buttons.length];
		for (Pointer pointer : inputHandler.pointers) {
			for (int i = 0; i < buttons.length; i++) {
				if (buttons[i] == null) continue;
				Rect bounds = buttons[i].getBounds();
				float length = bounds.top - bounds.bottom;
				bounds = new Rect((int) ((bounds.left + length / 5) * actualWidthToDrawnWidthRatio), (int) ((bounds.top + length / 5) * actualHeightToDrawnHeightRatio), (int) ((bounds.right - length / 5) * actualWidthToDrawnWidthRatio), (int) ((bounds.bottom - length / 5) * actualHeightToDrawnHeightRatio));
				if (bounds.contains((int)pointer.x, (int)pointer.y) && pointer.down) {
					buttonStates[i] = true;
				} else if (!buttonStates[i] && (bounds.contains((int)pointer.x, (int)pointer.y) || pointer.down)) {
					buttonStates[i] = false;
				}
			}
		}
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] == null) continue;
			if (buttonStates[i]) {
				changeButton(WonderSwanButton.values()[i], true, true);
			} else {
				changeButton(WonderSwanButton.values()[i], false, true);
			}
		}
		return true;

	}

	private boolean decodeKey (int keycode, boolean down) {

		for (WonderSwanButton button : WonderSwanButton.values()) {
			if (button.keyCode == keycode) {
				changeButton(button, down, false);
				return true;
			}
		}

		return false;

	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {

		// Log.d(TAG, "key down");

		/*if (keyCode == KeyEvent.KEYCODE_BACK) {
			// show menu
			((Activity) mContext).openOptionsMenu();
			return true;
		}*/

		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
			// if (!mRomLoaded) {
			// return true;
			// }
			return false;
		}

		return decodeKey(keyCode, true);
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event) {
		return decodeKey(keyCode, false);
	}

	public void showButtons (boolean show) {
		controlsVisible = show;
		renderer.showButtons(show);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (prefs.getString("opacity", "4").equals("0") && !prefs.getBoolean("showbuttonlabels", true)) {
			if (show) {
				Toast.makeText(mContext, "Touch controls are enabled but not visible due to your settings.", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(mContext, "Touch controls are disabled.", Toast.LENGTH_SHORT).show();
			}
		}
	}

}
