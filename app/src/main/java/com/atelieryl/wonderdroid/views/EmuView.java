
package com.atelieryl.wonderdroid.views;

import com.atelieryl.wonderdroid.Button;
import com.atelieryl.wonderdroid.R;
import com.atelieryl.wonderdroid.TouchInputHandler;
import com.atelieryl.wonderdroid.TouchInputHandler.Pointer;
import com.atelieryl.wonderdroid.WonderSwan;
import com.atelieryl.wonderdroid.WonderSwan.WonderSwanButton;
import com.atelieryl.wonderdroid.WonderSwanRenderer;
import com.atelieryl.wonderdroid.utils.EmuThread;
import com.atelieryl.wonderdroid.VibrateTask;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;


public class EmuView extends SurfaceView implements SurfaceHolder.Callback {

	private final static String TAG = EmuView.class.getSimpleName();
	@SuppressWarnings("unused")
	private final static boolean debug = true;
	private boolean mPaused = false;

	private EmuThread mThread;
	private final WonderSwanRenderer renderer;
	private boolean controlsVisible = false;
	private final GradientDrawable[] buttons;
	private final TouchInputHandler inputHandler;
	private static Context mContext;
	
	private float actualWidthToDrawnWidthRatio = 0;
	private float actualHeightToDrawnHeightRatio = 0;
	private float widthToHeightRatio = 0;
	
	private boolean started = false;
	private int sharpness = 3;
	private boolean stretchToFill = false;
	
	static int vibratedown = 5;
	static int vibrateup = 1;

	boolean relocate = false;

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

	public EmuView (Context context) {
		this(context, null);
	}

	public EmuView (Context context, AttributeSet attrs) {
		super(context, attrs);

		this.mContext = context;
		
		inputHandler = new TouchInputHandler(context);

		buttons = new GradientDrawable[WonderSwanButton.values().length];

		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = (GradientDrawable)getResources().getDrawable(R.drawable.button);
		}

		setZOrderOnTop(true); // FIXME any advantage to this?

		SurfaceHolder holder = this.getHolder();
		holder.addCallback(this);

		renderer = new WonderSwanRenderer();
		mThread = new EmuThread(renderer);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		sharpness = Integer.parseInt(prefs.getString("sharpness", "3"));
		mThread.setFrameskip(Integer.parseInt(prefs.getString("frameskip", "0")));
		stretchToFill = prefs.getBoolean("stretchtofill", false);
		renderer.setClearBeforeDraw(!stretchToFill);
		
		vibratedown = Integer.parseInt(prefs.getString("vibratedown", "5"));
		vibrateup = Integer.parseInt(prefs.getString("vibrateup", "1"));

		relocate = prefs.getBoolean("relocate", false);
	}

	@Override
	public void surfaceChanged (SurfaceHolder holder, int format, int width, int height) {
		if (actualHeightToDrawnHeightRatio == 0 || actualHeightToDrawnHeightRatio == 1) {
			if (widthToHeightRatio == 0)
				widthToHeightRatio = (float) this.getWidth() / (float) this.getHeight();
			int newWidth = WonderSwan.SCREEN_WIDTH * sharpness;
			int newHeight = WonderSwan.SCREEN_HEIGHT * sharpness;
			if (!stretchToFill) {
				while ((float) newWidth / (float) newHeight < widthToHeightRatio) newWidth++;
			}
			actualWidthToDrawnWidthRatio = (float) width / (float) newWidth;
			actualHeightToDrawnHeightRatio = (float) height / (float) newHeight;
			width = newWidth;
			height = newHeight;
			
			int spacing = -1 * height / 50;
			int buttonsize = (int)(height / 6.7);
			for (int i = 0; i < buttons.length; i++) {
				buttons[i].setSize(buttonsize, buttonsize);
	
				int updownleft = buttonsize + spacing;
				int updownright = buttonsize + buttonsize + spacing;
				int bottomrowtop = height - buttonsize;
	
				switch (i) {
				// Y
				case 0: //up
					buttons[i].setBounds(updownleft, 0, updownright, buttonsize);
					break;
				case 1: //left
					buttons[i].setBounds(0, buttonsize + spacing, buttonsize, (buttonsize * 2) + spacing);
					break;
				case 2: //right
					buttons[i].setBounds(2 * (buttonsize + spacing), buttonsize + spacing, buttonsize + 2 * (buttonsize + spacing), (buttonsize * 2)
						+ spacing);
					break;
				case 3: //down
					buttons[i].setBounds(updownleft, (buttonsize * 2) + (spacing * 2), updownright, (buttonsize * 3) + (spacing * 2));
					break;
				// X
				case 4:
					buttons[i].setBounds(updownleft, height - buttonsize, updownright, height);
					break;
				case 5:
					buttons[i].setBounds(0, height - (buttonsize * 2) - spacing, buttonsize, height - buttonsize - spacing);
					break;
				case 6:
					buttons[i].setBounds(2 * (buttonsize + spacing), height - (buttonsize * 2) - spacing, buttonsize + 2 * (buttonsize + spacing), height
						- buttonsize - spacing);
					break;
				case 7:
					buttons[i].setBounds(updownleft, (height - (buttonsize * 3)) - (2 * spacing), updownright,
						(height - (buttonsize * 2)) - (2 * spacing));
					break;
				// A,B
				case 8:
					buttons[i].setBounds(width - buttonsize, bottomrowtop, width, height);
					break;
				case 9:
					buttons[i].setBounds(width - (buttonsize * 2) + spacing * 2, bottomrowtop, (width - buttonsize) + spacing * 2, height);
					break;
				// Start
				case 10:
					buttons[i].setSize(buttonsize * 2, buttonsize);
					if (relocate) {
						buttons[i].setBounds(width - buttonsize * 2, 0, width, buttonsize);
					} else {
						buttons[i].setBounds((width / 2) - buttonsize, bottomrowtop, (width / 2) + buttonsize, height);
					}
					break;
				}
			}
	
			Button[] buts = new Button[buttons.length];
	
			if (buttons != null) {
				Paint textPaint = new Paint();
				textPaint.setColor(0xFFFFFFFF);
				textPaint.setTextSize(height / 30);
				textPaint.setShadowLayer(3, 1, 1, 0x99000000);
				textPaint.setAntiAlias(true);
	
				for (int i = 0; i < buttons.length; i++) {
					buts[i] = new Button(buttons[i], textPaint, WonderSwanButton.values()[i].name());
				}
			}
	
			float postscale = (float)width / (float)WonderSwan.SCREEN_WIDTH;
	
			if (WonderSwan.SCREEN_HEIGHT * postscale > height) {
				postscale = (float)height / (float)WonderSwan.SCREEN_HEIGHT;
	
			}
	
			Matrix scale = renderer.getMatrix();
	
			renderer.setButtons(buts);
	
			scale.reset();
			scale.postScale(sharpness, sharpness);
			scale.postTranslate((width - WonderSwan.SCREEN_WIDTH * sharpness) / 2, 0);
		}
		mHolder = holder;
	}

	@Override
	public void surfaceCreated (SurfaceHolder holder) {
		holder.setFormat(PixelFormat.RGB_565);
		if (widthToHeightRatio == 0)
			widthToHeightRatio = (float) this.getWidth() / (float) this.getHeight();
		int holderWidth = WonderSwan.SCREEN_WIDTH * sharpness;
		int holderHeight = WonderSwan.SCREEN_HEIGHT * sharpness;
		if (!stretchToFill) {
			while ((float) holderWidth / (float) holderHeight < widthToHeightRatio) holderWidth++;
		}
		holder.setFixedSize(holderWidth, holderHeight);
		mHolder = holder;
		mThread.setSurfaceHolder(holder);
	}

	@Override
	public void surfaceDestroyed (SurfaceHolder holder) {
		mHolder = null;
		//mThread.clearRunning();
	}

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
			mThread = new EmuThread(renderer);
			start();
		}
		mThread.setSurfaceHolder(mHolder);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mThread.setFrameskip(Integer.parseInt(prefs.getString("frameskip", "0")));
		vibratedown = Integer.parseInt(prefs.getString("vibratedown", "5"));
		vibrateup = Integer.parseInt(prefs.getString("vibrateup", "1"));
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

	public static void changeButton (WonderSwanButton button, boolean newState, boolean touch) {
		if (newState && !button.touchDown && touch && vibratedown > 0) {
			new VibrateTask(mContext).execute(vibratedown);
		}
		if (!newState && button.touchDown && touch && vibrateup > 0) {
			new VibrateTask(mContext).execute(vibrateup);
		}
		if (touch) {
			button.touchDown = newState;
		} else {
			button.hardwareKeyDown = newState;
		}
		button.down = (button.touchDown || button.hardwareKeyDown);
		WonderSwan.buttonsDirty = true;
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
				Rect bounds = buttons[i].getBounds();
				bounds = new Rect((int) (bounds.left * actualWidthToDrawnWidthRatio), (int) (bounds.top * actualHeightToDrawnHeightRatio), (int) (bounds.right * actualWidthToDrawnWidthRatio), (int) (bounds.bottom * actualHeightToDrawnHeightRatio));
				if (bounds.contains((int)pointer.x, (int)pointer.y) && pointer.down) {
					buttonStates[i] = true;
				} else if (!buttonStates[i] && (bounds.contains((int)pointer.x, (int)pointer.y) || pointer.down)) {
					buttonStates[i] = false;
				}
			}
		}
		for (int i = 0; i < buttons.length; i++) {
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
	}

}
