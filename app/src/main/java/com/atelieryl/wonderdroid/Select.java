
package com.atelieryl.wonderdroid;

import java.io.File;
import java.util.Locale;
import java.util.Random;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.atelieryl.wonderdroid.utils.RomAdapter;

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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class Select extends BaseActivity {

    private static final String TAG = Select.class.getSimpleName();
    
    private static String currentRomPath = "";

    private AssetManager mAssetManager;

    private ImageView mBG1;

    private RomAdapter mRAdapter;

    private TextView mScreenFormat;

    private Gallery gallery;

    private GridView grid;

    private boolean adSupported = true;
    
    private AlertDialog pathErrorAlertDialog;

    private int galleryPosition = 0;

    private Activity mActivity = (Activity) this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            galleryPosition = savedInstanceState.getInt("galleryPosition");
        }
        setContentView(R.layout.select);
        gallery = (Gallery)this.findViewById(R.id.select_gallery);
        /*if (gallery == null)
            grid = (GridView)this.findViewById(R.id.select_grid);*/

        findViewById(android.R.id.content).setBackgroundColor(Color.parseColor("#303030"));

        // Help translate button
        /*if (Locale.getDefault().getLanguage().equals("ja")) {
        	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean hideTranslate = prefs.getBoolean("hidetranslate", false);
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
	        	    	editor.putBoolean("hidetranslate", true);
	        	    	editor.commit();
	        	    }
	        	});
            }
        }*/

        // Set reversehorizontalorientation to false if just installed or upgraded
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean setreversehorizontalorientation = prefs.getBoolean("setreversehorizontalorientation", false);
        if (!setreversehorizontalorientation) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("reversehorizontalorientation", false);
            editor.putBoolean("setreversehorizontalorientation", true);
            editor.commit();
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
                Intent intent = new Intent(this, Prefs.class);
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

    private void startEmu(int romid) {
        try {
            Intent intent = new Intent(this, Main.class);
            intent.putExtra(Main.ROM, mRAdapter.getItem(romid));
            intent.putExtra(Main.ROMHEADER, mRAdapter.getHeader(romid));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannotloadrom, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pathErrorAlertDialog != null) {
        	pathErrorAlertDialog.dismiss();
        }
    }

    //AdView ad = null;

    private void parseSupportOptions() {
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //adSupported = prefs.getBoolean("adsupported", false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.askpermissions)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
            ((TextView)this.findViewById(R.id.select_noroms)).setText(getResources().getString(R.string.needpermissions));
            return;
        }

        File romdirx = getWonderDroidApplication().getRomDir();
        if (romdirx == null) {
            Toast.makeText(this, R.string.nosdcard, Toast.LENGTH_LONG).show();
            return;
        }

        String sdpath = romdirx.getAbsolutePath();

        parseSupportOptions();

        /*if (adSupported) {
            FrameLayout adbox = (FrameLayout)findViewById(R.id.adbox);
            ad = new AdView(this, AdSize.BANNER, "a14fbeecba23019");
            AdRequest r = new AdRequest();
            r.addTestDevice("6A3DABBD306114452F0D233CDADCF438");
            ad.loadAd(r);
            adbox.addView(ad);
        }*/

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        String romPath = prefs.getString("emu_rompath", "wonderdroid");
        if (!romPath.endsWith("/")) {
        	romPath = romPath + "/";
        }
        String shortRomPath = romPath;
        if (!romPath.startsWith("/")) {
        	romPath = sdpath + "/" + romPath;
        }
        
        String memPath = prefs.getString("emu_mempath", "wonderdroid/cartmem");
        if (!memPath.startsWith("/")) {
        	memPath = sdpath + "/" + memPath;
        }
        if (!memPath.endsWith("/")) {
        	memPath = memPath + "/";
        }
        
        if (!currentRomPath.equals("") && !romPath.equals(currentRomPath)) {
            galleryPosition = 0;
        	recreate();
        }
        currentRomPath = romPath;
        
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        
        boolean romdirok = false;
        boolean cartmemdirok = false;
        
        try {
	        File romdir = new File(romPath);
	        romdir.mkdirs();
	        romdirok = romdir.exists() && romdir.isDirectory() && romdir.canRead();
        } catch (Exception e) { }
        try {
	        File cartmemdir = new File(memPath);
	        cartmemdir.mkdirs();
	        cartmemdirok = cartmemdir.exists() && cartmemdir.isDirectory() && cartmemdir.canWrite();
        } catch (Exception e) { }
        
        if (!romdirok || !cartmemdirok) {
        	if (!cartmemdirok) {
        		builder.setTitle(R.string.mempatherror);
        		builder.setMessage(R.string.mempatherrordescription);
        		builder.setMessage(getResources().getString(R.string.mempatherrordescription).replace("???", memPath));
        	}
        	if (!romdirok) {
        		builder.setTitle(R.string.rompatherror);
        		builder.setMessage(getResources().getString(R.string.rompatherrordescription).replace("???", romPath));
        		((TextView)this.findViewById(R.id.select_noroms)).setText(getResources().getString(R.string.rompatherrordescription).replace("???", romPath));
        	}
            builder.setPositiveButton(R.string.gotopreferences, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) { 
	            	Intent intent = new Intent(Select.this, Prefs.class);
	                startActivity(intent);
	            }
	        });
            pathErrorAlertDialog = builder.show();
        }
        
        if (!romdirok) return;

        mScreenFormat = (TextView)this.findViewById(R.id.select_screenformat);
        mAssetManager = this.getAssets();
        mRAdapter = new RomAdapter(this.getBaseContext(), romPath, mAssetManager);

        if (mRAdapter.getCount() != 0) {

            ((TextView)this.findViewById(R.id.select_noroms)).setVisibility(View.GONE);

            mScreenFormat.setVisibility(View.VISIBLE);

            setupGalleryOrGrid(mRAdapter, new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    startEmu(arg2);
                }

            }, new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                    WonderSwanHeader header = null;
                    try {
                        header = mRAdapter.getHeader(arg2);
                    } catch (Exception e) {
                        Log.d(TAG, "Cannot get header for ROM at index " + arg2);
                    }
                    if (header != null) {
                        String newtext = "";
                        /*if (header.isColor) {
                            newtext += getString(R.string.colour);
                        } else {
                            newtext += getString(R.string.mono);
                        }*/

                        if (header.isVertical) {
                            newtext += getString(R.string.vertical);
                        } else {
                            newtext += getString(R.string.horizontal);
                        }

                        mScreenFormat.setText(newtext);
                    } else {
                        mScreenFormat.setText("");
                    }

                    Bitmap newbitmap = mRAdapter.getBitmap(arg2);
                    if (prefs.getBoolean("showbackground", true) && newbitmap != null) {
                        mBG1.setVisibility(View.VISIBLE);
                        mBG1.setImageBitmap(newbitmap);
                    } else {
                        mBG1.setVisibility(View.GONE);
                    }

                    galleryPosition = arg2;

                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }

            });

            mBG1 = (ImageView)this.findViewById(R.id.select_bg1);
        } else {
        	((TextView)this.findViewById(R.id.select_noroms)).setText(getResources().getString(R.string.noroms).replace("???", shortRomPath));
        }

    }

    private final void setupGalleryOrGrid(RomAdapter adapter,
            OnItemClickListener itemClickListener, OnItemSelectedListener itemSelectedListener) {
        if (gallery == null) {
            grid.setAdapter(mRAdapter);
            grid.setOnItemClickListener(itemClickListener);
            grid.setOnItemSelectedListener(itemSelectedListener);
        }

        else {
            gallery.setAdapter(mRAdapter);
            gallery.setOnItemClickListener(itemClickListener);
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
}
