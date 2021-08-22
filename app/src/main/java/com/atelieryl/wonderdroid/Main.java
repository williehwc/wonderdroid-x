
package com.atelieryl.wonderdroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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

    private String shortMemPath = "wonderdroid/cartmem";

    private Menu menu;

    private String packageName = "com.atelieryl.wonderdroid"; // Will be checked and replaced automatically if different

    private boolean showStateWarning = true;

    private int currentBackupNo = 0;

    private final int maxBackupNo = 4;

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
        if (!memPath.endsWith("/")) {
        	memPath = memPath + "/";
        }
        shortMemPath = memPath;
        if (!memPath.startsWith("/")) {
            memPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + memPath;
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

                if (checkFileAccess(mCartMem, false, false) && (mCartMem.length() > 0)) {
                    WonderSwan.loadbackup(mCartMem.getAbsolutePath());
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

        packageName = getPackageName();
        showStateWarning = !prefs.getBoolean("hidestatewarning", false);
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
                recreate();
                //WonderSwan.reset();
                return true;

            case R.id.main_prefsmi:
                Intent intent = new Intent(this, Prefs.class);
                startActivity(intent);
                return true;

            case R.id.main_togcntrlmi:
                toggleControls();
                return true;

            case R.id.main_savestate:
            case R.id.main_loadstate:
                updateStateMenuTitles();
                if (showStateWarning) {
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(this);
                    builder.setMessage(getResources().getString(R.string.statewarning).replace("???", shortMemPath))
                            .setPositiveButton(R.string.understand, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    showStateWarning = false;
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("hidestatewarning", true);
                                    editor.commit();
                                }
                            })
                            .show();
                }
                return true;
            case R.id.load_a1:
                loadState(-1);
                return true;
            case R.id.load_0:
                loadState(0);
                return true;
            case R.id.load_1:
                loadState(1);
                return true;
            case R.id.load_2:
                loadState(2);
                return true;
            case R.id.load_3:
                loadState(3);
                return true;
            case R.id.load_4:
                loadState(4);
                return true;
            case R.id.load_5:
                loadState(5);
                return true;
            case R.id.save_1:
                saveState(1);
                return true;
            case R.id.save_2:
                saveState(2);
                return true;
            case R.id.save_3:
                saveState(3);
                return true;
            case R.id.save_4:
                saveState(4);
                return true;
            case R.id.save_5:
                saveState(5);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    public void updateStateMenuTitles() {
        for (int i = -1; i <= 5; i++) {
            String statePath = memPath + mRomHeader.internalname + "_" + Integer.toString(i).replace("-", "a") + "_0.sav";
            String menuTitle = getResources().getString(R.string.slot) + " " + Integer.toString(i).replace("-", "a");
            if (i < 0) {
                menuTitle = getResources().getString(R.string.auto);
            } else if (i == 0) {
                menuTitle = getResources().getString(R.string.undo);
            }
            menuTitle += ": ";
            File stateFile = new File(statePath);
            int loadStateMenuItemId = getResources().getIdentifier("load_" + Integer.toString(i).replace("-", "a"), "id", packageName);
            MenuItem loadStateMenuItem = menu.findItem(loadStateMenuItemId);
            if (checkFileAccess(stateFile, false, true)) {
                menuTitle += formatDate(stateFile.lastModified());
                loadStateMenuItem.setEnabled(true);
            } else {
                menuTitle += getResources().getString(R.string.empty);
                loadStateMenuItem.setEnabled(false);
            }
            loadStateMenuItem.setTitle(menuTitle);
            if (i > 0) {
                int saveStateMenuItemId = getResources().getIdentifier("save_" + Integer.toString(i), "id", packageName);
                MenuItem saveStateMenuItem = menu.findItem(saveStateMenuItemId);
                saveStateMenuItem.setTitle(menuTitle);
            }
        }
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
        WonderSwan.audioEnabled = prefs.getInt("volume", 100) > 0;
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
        if (mRomHeader.isVertical || !prefs.getBoolean("reversehorizontalorientation", false)) {
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
        saveState(-1);
        if (checkFileAccess(mCartMem, true, false)) {
            WonderSwan.savebackup(mCartMem.getAbsolutePath());
        }
    }
    
    @Override
    public void onStop() {
    	// Called second when switching away or sleeping
    	super.onStop();
    }

    public void saveState(int stateNo) {
        for (int backupNo = 0; backupNo <= maxBackupNo; backupNo++) {
            String statePath = memPath + mRomHeader.internalname + "_" + Integer.toString(stateNo).replace("-", "a") + "_" + Integer.toString(backupNo) + ".sav";
            File stateFile = new File(statePath);
            try {
                stateFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                //throw new RuntimeException();
            }
            if (checkFileAccess(stateFile, true, false)) {
                for (int i = 0; i < 2; i++) {
                    WonderSwan.savestate(stateFile.getAbsolutePath());
                }
            } else {
                break;
            }
        }
    }

    public void loadState(int stateNo) {
        String statePath;
        File stateFile;
        int startingBackupNo = currentBackupNo;
        while (true) {
            statePath = memPath + mRomHeader.internalname + "_" + Integer.toString(stateNo).replace("-", "a") + "_" + Integer.toString(currentBackupNo) + ".sav";
            currentBackupNo++;
            if (currentBackupNo > maxBackupNo) {
                currentBackupNo = 0;
            }
            stateFile = new File(statePath);
            if (checkFileAccess(stateFile, false, true)) {
                break;
            } else if (startingBackupNo == currentBackupNo) {
                Toast.makeText(this, R.string.readmemfileerror, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (stateNo != 0) {
            saveState(0);
        }
        WonderSwan.loadstate(stateFile.getAbsolutePath());
    }

    public boolean checkFileAccess(File file, boolean write, boolean suppressToasts) {
    	boolean accessOK = false;
    	try {
    		if (file.isFile() && (!write || file.canWrite()) && (write || file.canRead())) {
    			accessOK = true;
    		}
    	} catch (Exception e) {
    		
    	}
        if (!accessOK && !suppressToasts) {
    		if (write) {
    			Toast.makeText(this, R.string.writememfileerror, Toast.LENGTH_SHORT).show();
    		} else {
    			Toast.makeText(this, R.string.readmemfileerror, Toast.LENGTH_SHORT).show();
    		}
    	}
    	return accessOK;
    }

    private String formatDate(long milliseconds) {
        // From https://stackoverflow.com/questions/36831597/android-convert-int-timestamp-to-human-datetime
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        TimeZone tz = TimeZone.getDefault();
        sdf.setTimeZone(tz);
        return sdf.format(calendar.getTime());
    }

}
