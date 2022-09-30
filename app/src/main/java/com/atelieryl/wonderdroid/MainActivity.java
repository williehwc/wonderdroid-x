
package com.atelieryl.wonderdroid;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.Toolbar;
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

public class MainActivity extends BaseActivity {

    public static final String ROM = "rom";

    public static final String PLAYING = "playing";

    public static final String HEADER = "header";

    private Context mContext;

    private ProgressBar mPB;

    private EmuView view = null;

    private Rom mRom;

    private boolean mControlsVisible = false;

    private boolean showControls = true;
    
    private String dirPath;

    private Menu menu;

    private String packageName = "com.atelieryl.wonderdroid"; // Will be checked and replaced automatically if different

    private int currentBackupNo = 0;

    private final int maxBackupNo = 4;

    private boolean vertical = false;

    private boolean portrait;

    private Activity mActivity = this;

    SharedPreferences prefs;

    private WonderSwanHeader mHeader;

    private boolean isWSC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRom = (Rom) this.getIntent().getExtras().getSerializable(ROM);
        mHeader = (WonderSwanHeader) this.getIntent().getExtras().getSerializable(HEADER);

        if (mRom == null) {
            throw new IllegalArgumentException();
        }

        mContext = this.getBaseContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        parseEmuOptions(prefs);

        dirPath = prefs.getString("storage_path", "");

        mPB = (ProgressBar)this.findViewById(R.id.romloadprogressbar);

        new GameLoader().execute((Void[])null);

        packageName = getPackageName();

        portrait = prefs.getString("orientation", "landscape").equals("portrait");

        // https://stackoverflow.com/questions/21724420
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public class GameLoader extends AsyncTask<Void, Void, long[]> {

        @Override
        protected long[] doInBackground(Void... params) {
            File romFile = Rom.getRomFile(mContext, mRom);
            // Check legacy save file
            for (String extension : Rom.wsRomExtensions) {
                if (romFile.getName().endsWith(extension)) {
                    String storagePath = prefs.getString("storage_path", "");
                    File saveFile = new File(storagePath + "/" + romFile.getName() + ".sav");
                    if (!saveFile.exists()) {
                        File legacySaveFile = new File(dirPath + "/" + mHeader.internalname + ".mem");
                        if (legacySaveFile.exists()) {
                            legacySaveFile.renameTo(saveFile);
                        }
                    }
                }
            }
            // Initialize Mednafen
            String name = prefs.getString("ws_name", "");
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(prefs.getLong("ws_birthday", 0));
            String blood = prefs.getString("ws_blood", "o");
            String sex = prefs.getString("ws_sex", "male");
            String language = prefs.getString("ws_language", "english");

            return WonderSwan.load(romFile.getAbsolutePath(), dirPath + "/",
                    name, cal.get(GregorianCalendar.YEAR),
                    cal.get(GregorianCalendar.MONTH),
                    cal.get(GregorianCalendar.DAY_OF_MONTH),
                    blood, sex, language);
        }

        @Override
        protected void onPostExecute(long[] gameInfo) {
            if (mPB != null) {
                mPB.setVisibility(ProgressBar.GONE);
            }

            if (gameInfo == null) {
                Toast.makeText(mContext, R.string.cannotloadrom, Toast.LENGTH_SHORT).show();
                WonderSwan.exit();
                finish();
                return;
            } else if (gameInfo[0] == 0) {
                Toast.makeText(mContext, R.string.no_sms, Toast.LENGTH_SHORT).show();
                WonderSwan.exit();
                finish();
                return;
            }

            WonderSwan.reset();
            //WonderSwan.loadbackup(mCartMem.getAbsolutePath());

            if (gameInfo[6] == 1) {
                vertical = true;
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }

            if (gameInfo[7] == 'w') {
                isWSC = true;
            }

            view = new EmuView(mContext, gameInfo, portrait && !vertical);
            setContentView(view);
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.onResume();
            view.start();
            parseKeys(prefs);
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

            // Show controls automatically
            if (showControls) {
                mControlsVisible = true;
                view.showButtons(mControlsVisible);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.main_exitmi:
                view.stop();
                WonderSwan.exit();
                intent = new Intent(mContext, SelectActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                Runtime.getRuntime().exit(0);
                return true;

            case R.id.main_pausemi:
                view.togglepause();
                return true;

            case R.id.main_resetmi:
                WonderSwan.reset();
                //recreate();
                return true;

            case R.id.main_prefsmi:
                intent = new Intent(this, PrefsActivity.class);
                intent.putExtra(MainActivity.PLAYING, true);
                startActivity(intent);
                return true;

            case R.id.main_togcntrlmi:
                toggleControls();
                return true;

            case R.id.main_savestate:
            case R.id.main_loadstate:
                updateStateMenuTitles();
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
        boolean fileAccessible;
        boolean wscLegacyState = false;
        for (int i = -1; i <= 5; i++) {
            String statePath;
            if (wscLegacyState) {
                statePath = dirPath + "/" + mHeader.internalname + "_" + Integer.toString(i).replace("-", "a") + "_0.sav";
            } else {
                statePath = dirPath + "/" + mRom.fileName + ".mc" + Integer.toString(i).replace("-1", "9");
            }
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
            fileAccessible = checkFileAccess(stateFile, false, true);
            if (fileAccessible) {
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
            if (wscLegacyState) {
                wscLegacyState = false;
            } else if (isWSC && !fileAccessible) {
                wscLegacyState = true;
                i--;
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
                prefs.getInt("hwcontrolY4", 0), prefs.getInt("hwcontrolSelect", 0));

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
        parseEmuOptions(prefs);
        if (view != null) {
            view.onResume();
            parseKeys(prefs);
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        String orientation = prefs.getString("orientation", "landscape");
        if (vertical || (orientation.equals("reverselandscape") && !portrait)) {
        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (portrait) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
    
    @Override
    public void onPause() {
    	// Called first when switching away or sleeping
        super.onPause();
        if (view != null) {
            view.stop();
            saveState(-1);
        }
        //WonderSwan.savebackup(mCartMem.getAbsolutePath());
    }
    
    @Override
    public void onStop() {
    	// Called second when switching away or sleeping
    	super.onStop();
    }

    public void saveState(int stateNo) {
        String statePath = dirPath + "/" + mRom.fileName + ".mc" + Integer.toString(stateNo).replace("-1", "9");
        File stateFile = new File(statePath);
        try {
            stateFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException();
        }
        if (checkFileAccess(stateFile, true, false)) {
            WonderSwan.savestate(stateFile.getAbsolutePath());
        }
    }

    public void loadState(int stateNo) {
        String statePath;
        File stateFile;
        int startingBackupNo = currentBackupNo;
        boolean wscLegacyState = false;
        while (true) {
            if (wscLegacyState) {
                statePath = dirPath + "/" + mHeader.internalname + "_" + Integer.toString(stateNo).replace("-", "a") + "_" + Integer.toString(currentBackupNo) + ".sav";
                currentBackupNo++;
                if (currentBackupNo > maxBackupNo) {
                    currentBackupNo = 0;
                }
            } else {
                statePath = dirPath + "/" + mRom.fileName + ".mc" + Integer.toString(stateNo).replace("-1", "9");
            }
            stateFile = new File(statePath);
            if (checkFileAccess(stateFile, false, true)) {
                if (wscLegacyState) Toast.makeText(this, R.string.legacystate, Toast.LENGTH_SHORT).show();
                break;
            } else if (isWSC && !wscLegacyState) {
                wscLegacyState = true;
            } else if (startingBackupNo == currentBackupNo) {
                Toast.makeText(this, R.string.readmemfileerror, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (stateNo != 0) {
            saveState(0);
        }
        // Wait until the undo state has finished saving
        for (int i = 0; i < 1000; i++) {
            if (WonderSwan.running) {
                break;
            }
        }
        if (wscLegacyState) {
            WonderSwan.loadstate(stateFile.getName());
        } else {
            WonderSwan.loadstate(stateFile.getAbsolutePath());
        }
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
