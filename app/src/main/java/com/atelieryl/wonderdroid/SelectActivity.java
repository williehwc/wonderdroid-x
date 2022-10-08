
package com.atelieryl.wonderdroid;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.atelieryl.wonderdroid.utils.RomAdapter;
import com.atelieryl.wonderdroid.utils.RomFilter;
import com.downloader.Error;
import com.downloader.OnDownloadListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class SelectActivity extends BaseActivity {

    private static final String TAG = SelectActivity.class.getSimpleName();
    
    private static String currentRomPath = "";

    private AssetManager mAssetManager;

    private ImageView mBG1;

    private RomAdapter mRAdapter;

    private Gallery gallery;

    private GridView grid;

    private int galleryPosition = 0;

    private Activity mActivity = (Activity) this;

    private FloatingActionButton floatingActionButton;

    private SharedPreferences prefs;

    private boolean onboarding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select);
        gallery = (Gallery)this.findViewById(R.id.select_gallery);
        /*if (gallery == null)
            grid = (GridView)this.findViewById(R.id.select_grid);*/

        findViewById(android.R.id.content).setBackgroundColor(Color.parseColor("#303030"));

        floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddGameActivity.class);
                startActivity(intent);
            }
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState != null) {
            galleryPosition = savedInstanceState.getInt("galleryPosition");
        } else {
            galleryPosition = prefs.getInt("lastplayedromindex", 0);
        }

        // Help translate button
        if (Locale.getDefault().getLanguage().equals("ja")) {
            boolean hideTranslate = prefs.getBoolean("hidetranslate2", false);
            if (!hideTranslate) {
            	findViewById(R.id.helptranslate).setVisibility(View.VISIBLE);
            	findViewById(R.id.helptranslate).bringToFront();
	        	findViewById(R.id.helptranslatebutton).setOnClickListener(new View.OnClickListener() {
	        	    @Override
	        	    public void onClick(View v) {
	        	        openURL("https://goo.gl/forms/d1bu6SDCz0OYXBbE3");
	        	    }
	        	});
	        	findViewById(R.id.hidehelptranslate).setOnClickListener(new View.OnClickListener() {
	        	    @Override
	        	    public void onClick(View v) {
	        	    	findViewById(R.id.helptranslate).setVisibility(View.GONE);
	        	    	SharedPreferences.Editor editor = prefs.edit();
	        	    	editor.putBoolean("hidetranslate2", true);
	        	    	editor.commit();
	        	    }
	        	});
            }
        }

        // Onboarding
        String storageVolume = prefs.getString("storage_path", null);
        onboarding = (storageVolume == null);
        if (onboarding) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        // Handle item selection
        switch (item.getItemId()) {
            /*case R.id.select_exitmi:
                this.finish();
                return true;*/
            case R.id.select_prefsmi:
                Intent intent = new Intent(this, PrefsActivity.class);
                startActivity(intent);
                return true;
            case R.id.select_reportmi:
                builder.setTitle(R.string.report)
                .setMessage(R.string.reportdescription)
                .setPositiveButton(R.string.issues, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                    	openURL("https://github.com/williehwc/wonderdroid-x/issues");
                    }
                 })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                        // do nothing
                    }
                 })
                .show();
            	return true;
            case R.id.select_moreappsmi:
            	openURL("https://play.google.com/store/apps/dev?id=8429993243664540065");
            	return true;
            case R.id.select_aboutmi:
                builder.setTitle(R.string.about)
                .setMessage(R.string.aboutdescription)
                .setPositiveButton(R.string.visit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                    	openURL("http://yearbooklabs.com/");
                    }
                 })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                        // do nothing
                    }
                 })
                .show();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void openURL(String url) {
    	try {
    		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    		startActivity(browserIntent);
    	} catch (Exception e) {
    		android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		    clipboard.setText(url);
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.cannotopenurl)
            .setMessage(R.string.cannotopenurldescription)
            .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int which) { 
                    // do nothing
                }
             })
            .show();
    	}
    }

    private void startEmu(int romIndex) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.ROM, mRAdapter.getItem(romIndex));
            intent.putExtra(MainActivity.HEADER, mRAdapter.getHeader(romIndex));
            startActivity(intent);
            // Save gallery position
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("lastplayedromindex", romIndex);
            editor.commit();
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannotloadrom, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get destination path and check if it's available
        String storagePath = prefs.getString("storage_path", "");
        try {
            File testFile = new File(storagePath + "/" + "test.txt");
            testFile.createNewFile();
            testFile.delete();
            File[] sourceFiles = new File(storagePath).listFiles(new RomFilter(false, false, false));
            if (sourceFiles.length == 0) throw new Exception();
        } catch (Exception e) {
//            AlertDialog.Builder builder;
//            builder = new AlertDialog.Builder(this);
//            builder.setMessage(getString(R.string.storage_error) + "\n" + e.getMessage());
//            builder.setPositiveButton(R.string.gotopreferences, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    Intent intent = new Intent(SelectActivity.this, AddGameActivity.class);
//                    startActivity(intent);
//                }
//            });
//            return;

            // Reset the storage path
            if (!onboarding) {
                File[] externalStorageVolumes = ContextCompat.getExternalCacheDirs(getApplicationContext());
                storagePath = getFilesDir().getPath();
                if (externalStorageVolumes.length > 0 && externalStorageVolumes[0] != null && !externalStorageVolumes[0].getPath().equals("")) {
                    storagePath = externalStorageVolumes[0].getPath();
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("storage_path", storagePath);
                editor.commit();
                Log.e("WonderDroid", "Storage path was reset");
            }
        }

        OnDownloadListener onDownloadListener = new OnDownloadListener() {
            @Override
            public void onDownloadComplete() {
                mRAdapter.notifyDataSetChanged();
                setBackground();
            }

            @Override
            public void onError(Error error) {
                Log.e("WonderDroid Box Art", error.toString());
            }
        };

        mAssetManager = this.getAssets();
        mRAdapter = new RomAdapter(this.getBaseContext(), storagePath, mAssetManager, onDownloadListener);

        if (mRAdapter.getCount() != 0) {

            this.findViewById(R.id.select_noroms).setVisibility(View.GONE);

            setUpGallery(mRAdapter, new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    startEmu(arg2);
                }

            }, new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getApplicationContext(), FileMgmtActivity.class);
                    intent.putExtra(MainActivity.ROM, mRAdapter.getItem(position));
                    startActivity(intent);
                    return true;
                }

            }, new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    galleryPosition = arg2;
                    setBackground();
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }

            });

            mBG1 = (ImageView)this.findViewById(R.id.select_bg1);

            // Uninstall warning
            boolean uninstallWarning = prefs.getBoolean("uninstallwarning", false);
            if (!uninstallWarning) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.important)
                    .setMessage(R.string.uninstall_warning)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("uninstallwarning", true);
                            editor.commit();
                        }
                    })
                    .show();
            }

        } else {
            // Check if user is upgrading from WonderDroid X.
            // Either storage permission is enabled or "setreversehorizontalorientation" is set, while storagePath is null
            boolean storagePermission = PackageManager.PERMISSION_GRANTED == ContextCompat
                    .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean setreversehorizontalorientation = prefs.getBoolean("setreversehorizontalorientation", false);
            String romPath = prefs.getString("emu_rompath", "\0");
            boolean upgrade = (storagePermission || setreversehorizontalorientation || !romPath.equals("\0"));

            String emptyState = getResources().getString(R.string.noroms);

            if (upgrade) {
                String emuRomPath = prefs.getString("emu_rompath", "wonderdroid");
                emptyState += "\n\n" + getResources().getString(R.string.noroms_upgrade).replace("???", emuRomPath);
                View helpButton = findViewById(R.id.helpButton);
                helpButton.setVisibility(View.VISIBLE);
                helpButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String param = URLEncoder.encode(emuRomPath);
                        try {
                            param = URLEncoder.encode(emuRomPath, "GB2312");
                        } catch (Exception e) {}
                        openURL("http://yearbooklabs.com/sd/import.php?activity=select&path=" + param);
                    }
                });
            }

            ((TextView)this.findViewById(R.id.select_noroms_text)).setText(emptyState);
        }

    }

    private void setBackground() {
        Bitmap newbitmap = mRAdapter.getBitmap(galleryPosition);
        if (prefs.getBoolean("showbackground", true) && newbitmap != null) {
            mBG1.setVisibility(View.VISIBLE);
            mBG1.setImageBitmap(newbitmap);
        } else {
            mBG1.setVisibility(View.GONE);
        }
    }

    private final void setUpGallery(RomAdapter adapter,
                                    OnItemClickListener itemClickListener, AdapterView.OnItemLongClickListener itemLongClickListener, OnItemSelectedListener itemSelectedListener) {
        if (gallery == null) {
            grid.setAdapter(mRAdapter);
            grid.setOnItemClickListener(itemClickListener);
            grid.setOnItemLongClickListener(itemLongClickListener);
            grid.setOnItemSelectedListener(itemSelectedListener);
        }

        else {
            gallery.setAdapter(mRAdapter);
            gallery.setOnItemClickListener(itemClickListener);
            gallery.setOnItemLongClickListener(itemLongClickListener);
            gallery.setOnItemSelectedListener(itemSelectedListener);
            try {
                gallery.setSelection(galleryPosition);
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        try {
            savedInstanceState.putInt("galleryPosition", galleryPosition);
        } catch (Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }
}
