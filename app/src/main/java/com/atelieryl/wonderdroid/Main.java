
package com.atelieryl.wonderdroid;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;

import com.atelieryl.wonderdroid.utils.RomAdapter.Rom;
import com.atelieryl.wonderdroid.views.EmuView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Main extends BaseActivity {

    public static final String ROM = "rom";

    public static final String ROMHEADER = "romheader";

    private Context mContext;

    private ProgressBar mPB;

    private EmuView view;

    private Rom mRom;

    private WonderSwanHeader mRomHeader;

    private File mCartMem;

    private boolean mControlsVisible = false;

    private boolean showControls = true;
    
    private String memPath = "wonderdroid/cartmem";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRom = (Rom)this.getIntent().getExtras().getSerializable(ROM);
        mRomHeader = (WonderSwanHeader)this.getIntent().getExtras().getSerializable(ROMHEADER);

        if (mRom == null || mRomHeader == null) {
            throw new IllegalArgumentException();
        }

        view = new EmuView(this);
        setContentView(view);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        mContext = this.getBaseContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        parseEmuOptions(prefs);
        parseKeys(prefs);
        
        memPath = prefs.getString("emu_mempath", "wonderdroid/cartmem");
        if (!memPath.startsWith("/")) {
        	memPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + memPath;
        }
        if (!memPath.endsWith("/")) {
        	memPath = memPath + "/";
        }

        mPB = (ProgressBar)this.findViewById(R.id.romloadprogressbar);

        AsyncTask<Void, Void, Void> loader = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
            }

            @Override
            protected Void doInBackground(Void... params) {
                mCartMem = new File(memPath + mRomHeader.internalname + ".mem");
                try {
                    mCartMem.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    //throw new RuntimeException();
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main.this);
                String name = prefs.getString("ws_name", "");
                String sex = prefs.getString("ws_sex", "1");
                String blood = prefs.getString("ws_blood", "1");
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(prefs.getLong("ws_birthday", 0));

                WonderSwan.load(Rom.getRomFile(mContext, mRom).getAbsolutePath(),
                        mRomHeader.isColor, name, cal.get(GregorianCalendar.YEAR),
                        cal.get(GregorianCalendar.MONTH), cal.get(GregorianCalendar.DAY_OF_MONTH),
                        Integer.parseInt(blood), Integer.parseInt(sex));
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (mPB != null) {
                    mPB.setVisibility(ProgressBar.GONE);
                }

                WonderSwan.reset();
                
                if (checkFileAccess(mCartMem, false) && (mCartMem.length() > 0)) {
                    WonderSwan.loadbackupdata(mCartMem.getAbsolutePath());
                }
                view.start();
                // Show controls automatically
                if (showControls) {
                	mControlsVisible = true;
                    view.showButtons(mControlsVisible);
                }
            }
        };

        loader.execute((Void[])null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.main_exitmi:
                this.finish();
                return true;

            case R.id.main_pausemi:
                view.togglepause();
                return true;

            case R.id.main_resetmi:
                WonderSwan.reset();
                return true;

            case R.id.main_prefsmi:
                Intent intent = new Intent(this, Prefs.class);
                startActivity(intent);
                return true;

            case R.id.main_togcntrlmi:
                toggleControls();
                return true;
                // case R.id.quit:
                // quit();
                // return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public void onBackPressed() {
        ((Toolbar) getWindow().getDecorView().findViewById(R.id.action_bar)).showOverflowMenu();
    }

    private void toggleControls() {
        mControlsVisible = !mControlsVisible;
        view.showButtons(mControlsVisible);
    }

    private void parseEmuOptions(SharedPreferences prefs) {
        WonderSwan.audioEnabled = prefs.getBoolean("emusound", true);
        showControls = prefs.getBoolean("showcontrols", true);
    }

    private void parseKeys(SharedPreferences prefs) {

        view.setKeyCodes(prefs.getInt("hwcontrolStart", 0), prefs.getInt("hwcontrolA", 0),
                prefs.getInt("hwcontrolB", 0), prefs.getInt("hwcontrolX1", 0),
                prefs.getInt("hwcontrolX2", 0), prefs.getInt("hwcontrolX3", 0),
                prefs.getInt("hwcontrolX4", 0), prefs.getInt("hwcontrolY1", 0),
                prefs.getInt("hwcontrolY2", 0), prefs.getInt("hwcontrolY3", 0),
                prefs.getInt("hwcontrolY4", 0));

    }

    @Override
    public void onRestart() {
    	// Called first when switching/waking to emulator
        super.onRestart();
    }

    @Override
    protected void onResume() {
    	// Called second when switching/waking to emulator
        super.onResume();
        view.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        parseEmuOptions(prefs);
        parseKeys(prefs);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (mRomHeader.isVertical || prefs.getBoolean("reversehorizontalorientation", false)) {
        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else {
        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
    
    @Override
    public void onPause() {
    	// Called first when switching away or sleeping
        super.onPause();
        view.stop();
    }
    
    @Override
    public void onStop() {
    	// Called second when switching away or sleeping
        if (checkFileAccess(mCartMem, true)) {
            WonderSwan.storebackupdata(mCartMem.getAbsolutePath());
        }
    	super.onStop();
    }
    
    public boolean checkFileAccess(File file, boolean write) {
    	boolean accessOK = false;
    	try {
    		if (file.isFile() && (!write || file.canWrite()) && (write || file.canRead())) {
    			accessOK = true;
    		}
    	} catch (Exception e) {
    		
    	}
    	if (!accessOK) {
    		if (write) {
    			Toast.makeText(this, R.string.writememfileerror, Toast.LENGTH_SHORT).show();
    		} else {
    			Toast.makeText(this, R.string.readmemfileerror, Toast.LENGTH_SHORT).show();
    		}
    	}
    	return accessOK;
    }

}
