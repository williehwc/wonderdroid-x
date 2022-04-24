
package com.atelieryl.wonderdroid.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.atelieryl.wonderdroid.WonderSwanHeader;
import com.atelieryl.wonderdroid.views.RomGalleryView;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class RomAdapter extends BaseAdapter {

    public static final class Rom implements Serializable {

        private static final long serialVersionUID = 1L;

        public static String[] allRomExtensions = new String[] {
                "ws", "wsc", "gg", "pce", "ngp", "ngc"
        };

        public static String[] wsRomExtensions = new String[] {
                "ws", "wsc"
        };

        public static String[] boxArtExtensions = new String[] {
                "jpg", "png"
        };

        public enum Type {
            ZIP, RAW
        }

        public final Type type;

        public final String displayName;

        public final File sourcefile;

        public final String fileName;

        public Rom(Type type, File sourceFile, String fileName, String displayName) {
            this.type = type;
            this.sourcefile = sourceFile;
            this.fileName = fileName;
            this.displayName = displayName;
        }

        public static File getRomFile(Context context, Rom rom) {
            switch (rom.type) {
                case RAW:
                    return rom.sourcefile;
                case ZIP:
                    try {
                        return ZipCache.getFile(context, new ZipFile(rom.sourcefile), rom.fileName,
                                allRomExtensions);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return null;
                    }

            }
            return null;
        }

        public static WonderSwanHeader getHeader(Context context, Rom rom) {

            File romFile = null;
            try {
                if (rom.type == Type.RAW || rom.type == Type.ZIP
                        && ZipCache.isZipInCache(context, new ZipFile(rom.sourcefile))) {
                    romFile = Rom.getRomFile(context, rom);
                } else if (rom.type == Type.ZIP) {
                    ZipFile zip = new ZipFile(rom.sourcefile);
                    ZipEntry entry = ZipUtils.getEntry(zip, rom.fileName);
                    return new WonderSwanHeader(ZipUtils.getBytesFromEntry(zip, entry,
                            entry.getSize() - WonderSwanHeader.HEADERLEN,
                            WonderSwanHeader.HEADERLEN));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (romFile != null) {
                WonderSwanHeader header = new WonderSwanHeader(romFile);
                return header;
            }

            return null;
        }
    }

    private static final String TAG = RomAdapter.class.getSimpleName();

    @SuppressLint("UseSparseArrays")
    private final HashMap<Integer, WonderSwanHeader> mHeaderCache = new HashMap<Integer, WonderSwanHeader>();

    private final LruCache<String, Bitmap> splashCache = new LruCache<String, Bitmap>(1024 * 512) {

        @SuppressLint("NewApi")
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }

    };

    private final AssetManager mAssetManager;

    private final File mRomDir;

    private final Context mContext;

    private final Rom[] mRoms;

    private final OnDownloadListener mOnDownloadListener;

    public RomAdapter(Context context, String romdir, AssetManager assetManager, OnDownloadListener onDownloadListener) {
        mAssetManager = assetManager;
        mRomDir = new File(romdir);
        mContext = context;
        mRoms = findRoms();
        mOnDownloadListener = onDownloadListener;
    }

    private Rom[] findRoms() {
        File[] sourceFiles = mRomDir.listFiles(new RomFilter());
        ArrayList<Rom> roms = new ArrayList<>();
        if (sourceFiles != null) {
            for (File sourceFile : sourceFiles) {

                if (sourceFile.getName().endsWith("zip")) {
                    try {
                        for (String entry : ZipUtils.getValidEntries(new ZipFile(sourceFile), Rom.allRomExtensions)) {
                            roms.add(new Rom(Rom.Type.ZIP, sourceFile, entry, entry + " (ZIP)"));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        break;
                    }
                } else {
                    roms.add(new Rom(Rom.Type.RAW, sourceFile, sourceFile.getName(), sourceFile.getName()));
                }

            }
        }
        Rom[] allRoms = roms.toArray(new Rom[0]);

        Arrays.sort(allRoms, new Comparator<Rom>() {
            public int compare(Rom lhs, Rom rhs) {
                return lhs.sourcefile.compareTo(rhs.sourcefile);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (!prefs.getBoolean("no_box_art", false))
            new BoxArtTask().execute(allRoms);

        return allRoms;
    }

    public class BoxArtTask extends AsyncTask<Rom, Void, Void> {

        SQLiteDatabase openVGDB = null;

        private boolean openDB() { // Also gets the download manager
            try {
                openVGDB = SQLiteDatabase.openDatabase(mRomDir + "/openvgdb.sqlite", null, SQLiteDatabase.OPEN_READONLY);
                return true;
            } catch (Exception e) {
                Log.e("WonderDroid", e.toString());
                return false;
            }
        }

        @Override
        protected Void doInBackground(Rom... roms) {

            for (Rom rom : roms) {
                // Look for box art
                boolean boxArtExists = false;
                for (String boxArtExtension : Rom.boxArtExtensions) {
                    File boxArtFile = new File(mRomDir + "/" + rom.fileName + "." + boxArtExtension);
                    if (boxArtFile.exists()) {
                        boxArtExists = true;
                        break;
                    }
                }
                if (!boxArtExists) {
                    if (openVGDB == null && !openDB()) return null;
                    File romFile = Rom.getRomFile(mContext, rom);
                    String md5 = MD5.calculateMD5(romFile).toUpperCase();
                    Cursor cursor = openVGDB.rawQuery("SELECT * FROM ROMs WHERE romHashMD5 = \"" + md5 + "\"", null);
                    String url = null;
                    if (cursor.moveToFirst()) {
                        String romID = cursor.getString(0);
                        cursor = openVGDB.rawQuery("SELECT * FROM RELEASES WHERE romID = \"" + romID + "\"", null);
                        if (cursor.moveToFirst()) {
                            url = cursor.getString(7);
                        }
                    }
                    if (url != null) {
                        // Get extension
                        String extension = null;
                        for (String boxArtExtension : Rom.boxArtExtensions) {
                            if (url.endsWith("." + boxArtExtension)) {
                                extension = boxArtExtension;
                                break;
                            }
                        }
                        if (extension == null) continue;
                        // Queue download
                        PRDownloader.download(url, mRomDir.getPath(), rom.fileName + "." + extension)
                                    .build()
                                    .start(mOnDownloadListener);
                    } else {
                        File emptyFile = new File(rom.fileName + "." + Rom.boxArtExtensions[0]);
                        try {
                            emptyFile.createNewFile();
                        } catch (IOException e) {
                            Log.e("WonderDroid", e.toString());
                        }
                    }
                }
            }
            if (openVGDB != null) openVGDB.close();
            return null;
        }

    }

    public Bitmap getBitmap(int index) {
        try {
            Rom rom = (Rom)(this.getItem(index));
            String filename = rom.fileName;

            // Cache
            Bitmap splash = splashCache.get(filename);
            if (splash != null)
                return splash;

            // Box art
            for (String extension : Rom.boxArtExtensions) {
                String boxArtFilePath = mRomDir + "/" + rom.fileName + "." + extension;
                File boxArtFile = new File(boxArtFilePath);
                if (boxArtFile.exists()) {
                    splash = BitmapFactory.decodeFile(boxArtFilePath);
                    if (splash != null)
                        splashCache.put(filename, splash);
                    return splash;
                }
            }

            // Check if WS game before proceeding
            boolean isWsGame = false;
            for (String extension : Rom.wsRomExtensions) {
                if (filename.endsWith("." + extension)) {
                    isWsGame = true;
                    break;
                }
            }
            if (!isWsGame) return null;

            // WS games: screenshot
            WonderSwanHeader header = getHeader(index);
            String internalname = header.internalname;
            splash = BitmapFactory.decodeStream(mAssetManager.open("snaps/" + internalname + ".png"));
            if (header.isVertical) {
                Matrix rotationmatrix = new Matrix();
                rotationmatrix.setRotate(270, splash.getWidth() / 2, splash.getHeight() / 2);
                splash = Bitmap.createBitmap(splash, 0, 0, splash.getWidth(), splash.getHeight(),
                        rotationmatrix, false);
            }
            if (splash != null)
                splashCache.put(filename, splash);
            return splash;
        } catch (Exception e) {
            // e.printStackTrace();
            Log.d(TAG, "No box art for ROM at index " + index + " because " + e.getMessage());
            return null;
        }

    }

    @Override
    public int getCount() {
        if (mRoms != null) {
            return mRoms.length;
        }
        return 0;
    }

    @Override
    public Rom getItem(int arg0) {
        return mRoms[arg0];
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View oldview, ViewGroup parent) {
        RomGalleryView view;
        if (oldview == null)
            view = new RomGalleryView(mContext);
        else
            view = (RomGalleryView)oldview;

        try {
            view.setTitle(mRoms[position].displayName);
        } catch (Exception e) {
            view.setTitle("");
        }

        WonderSwanHeader header = null;
        try {
            header = getHeader(position);
        } catch (Exception e) {

        }
        if (header != null) {
            Bitmap shot = getBitmap(position);
            view.setSnap(shot);
            /*if (shot != null) {
                Log.d(TAG, "snap is null for " + mRoms[position].sourcefile);
            }*/
        } else {
            view.setSnap(null);
        }

        return view;
    }

    public synchronized WonderSwanHeader getHeader(int index) {
        if (mHeaderCache.containsKey(index))
            return mHeaderCache.get(index);
        Rom rom = (Rom)(this.getItem(index));
        WonderSwanHeader header = Rom.getHeader(mContext, rom);
        if (header != null)
            mHeaderCache.put(index, header);
        return header;
    }
}
