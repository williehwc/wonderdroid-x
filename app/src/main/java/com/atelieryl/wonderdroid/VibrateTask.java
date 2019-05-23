package com.atelieryl.wonderdroid;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class VibrateTask extends AsyncTask<Integer, Void, Void> {

	static Vibrator vibrator = null;
	
	public VibrateTask(Context context) {
		try {
			vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		} catch (Exception e) {
			Toast.makeText(context, "Vibration not available", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected Void doInBackground(Integer... vibrateLength) {
		try {
			vibrator.vibrate(vibrateLength[0].intValue());
		} catch (Exception e) {
			
		}
		return null;
	}

}
