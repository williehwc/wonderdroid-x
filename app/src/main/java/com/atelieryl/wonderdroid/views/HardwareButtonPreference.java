
package com.atelieryl.wonderdroid.views;

import com.atelieryl.wonderdroid.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HardwareButtonPreference extends Preference {
	private LinearLayout layout;

	public HardwareButtonPreference (Context context) {
		super(context);
	}

	public HardwareButtonPreference (Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HardwareButtonPreference (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView (ViewGroup parent) {

		LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout)layoutInflater.inflate(R.layout.hardwardbuttonpref, null);

		TextView controltext = (TextView)layout.getChildAt(0);
		controltext.setText(getTitle());

		final TextView current = (TextView)layout.getChildAt(1);
		if (getSharedPreferences().contains(getKey())) {
			current.setText(keycodeDecode(getSharedPreferences().getInt(getKey(), 0)));
		}

		Button set = (Button)layout.getChildAt(2);
		set.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick (View v) {
				KeyCaptureAlert alert = new KeyCaptureAlert(v.getContext(), current);
				alert.setTitle(R.string.pressakey);
				alert.show();

			}
		});

		Button clear = (Button)layout.getChildAt(3);
		clear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick (View v) {
				SharedPreferences.Editor editor = getEditor();
				current.setText("");
				editor.remove(getKey());
				editor.commit();
			}
		});

		return layout;
	}

	@Override
	public View getView (View convertView, ViewGroup parent) {
		convertView = this.layout == null ? onCreateView(parent) : this.layout;
		return convertView;
	}

	public class KeyCaptureAlert extends AlertDialog {
		final TextView current;

		public KeyCaptureAlert (Context arg0, TextView current) {
			super(arg0);
			this.current = current;
		}

		public boolean onKeyDown (int keyCode, KeyEvent event) {
			Log.d("key capture", ((Integer)keyCode).toString());

			SharedPreferences.Editor editor = getEditor();
			editor.putInt(getKey(), keyCode);
			editor.commit();
			current.setText(keycodeDecode(keyCode));
			this.dismiss();
			return true;

		}
	}

	static String keycodeDecode (int keycode) {
		switch (keycode) {
		case 0:
			return "";
		case KeyEvent.KEYCODE_BACK:
			return "back";
		case KeyEvent.KEYCODE_CAMERA:
			return "camera";
		case KeyEvent.KEYCODE_MENU:
			return "menu";
		case KeyEvent.KEYCODE_HOME:
			return "home";
		case KeyEvent.KEYCODE_SEARCH:
			return "search";
		case KeyEvent.KEYCODE_DPAD_CENTER:
			return "dpad center";
		case KeyEvent.KEYCODE_DPAD_UP:
			return "dpad up";
		case KeyEvent.KEYCODE_DPAD_DOWN:
			return "dpad down";
		case KeyEvent.KEYCODE_DPAD_LEFT:
			return "dpad left";
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			return "dpad right";
		case KeyEvent.KEYCODE_VOLUME_UP:
			return "volume up";
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			return "volume down";
		default:
			return "key " + keycode;
		}
	}
}
