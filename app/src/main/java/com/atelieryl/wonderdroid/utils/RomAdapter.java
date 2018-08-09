
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
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class RomAdapter extends BaseAdapter {

    public static final class Rom implements Serializable {

        private static final long serialVersionUID = 1L;

        public static String[] romExtension = new String[] {
                "ws", "wsc"
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
                                romExtension);
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
            if (Build.VERSION.SDK_INT >= 12)
                return value.getByteCount();
            else
                return value.getRowBytes() * value.getHeight();
        }

    };

    private final AssetManager mAssetManager;

    private final File mRomDir;

    private final Context mContext;

    private final Rom[] mRoms;

    public RomAdapter(Context context, String romdir, AssetManager assetManager) {
        mAssetManager = assetManager;
        mRomDir = new File(romdir);
        mContext = context;
        mRoms = findRoms();
    }

    private Rom[] findRoms() {
        File[] sourceFiles = mRomDir.listFiles(new RomFilter());
        ArrayList<Rom> roms = new ArrayList<Rom>();
        if (sourceFiles != null) {
            for (int i = 0; i < sourceFiles.length; i++) {

                if (sourceFiles[i].getName().endsWith("zip")) {
                    try {
                        for (String entry : ZipUtils.getValidEntries(new ZipFile(sourceFiles[i]),
                                Rom.romExtension)) {
                            roms.add(new Rom(Rom.Type.ZIP, sourceFiles[i], entry, sourceFiles[i]
                                    .getName().replaceFirst("\\.zip", "")));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        break;
                    }
                } else {
                    roms.add(new Rom(Rom.Type.RAW, sourceFiles[i], null, sourceFiles[i].getName()
                            .replaceFirst("\\.wsc", "").replaceFirst("\\.ws", "")));
                }

            }
        }
        Rom[] allRoms = roms.toArray(new Rom[0]);

        Arrays.sort(allRoms, new Comparator<Rom>() {
            public int compare(Rom lhs, Rom rhs) {
                return lhs.sourcefile.compareTo(rhs.sourcefile);
            }
        });

        return allRoms;
    }

    public Bitmap getBitmap(int index) {
        WonderSwanHeader header = getHeader(index);
        String internalname = header.internalname;
        Bitmap splash = splashCache.get(internalname);
        if (splash != null)
            return splash;

        try {
            splash = BitmapFactory.decodeStream(mAssetManager
                    .open("snaps/" + internalname + ".png"));
            if (header.isVertical) {
                Matrix rotationmatrix = new Matrix();
                rotationmatrix.setRotate(270, splash.getWidth() / 2, splash.getHeight() / 2);
                splash = Bitmap.createBitmap(splash, 0, 0, splash.getWidth(), splash.getHeight(),
                        rotationmatrix, false);
            }
            if (splash != null)
                splashCache.put(internalname, splash);
            return splash;
        } catch (IOException e) {
            // e.printStackTrace();
            Log.d(TAG, "No shot for " + internalname);
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

        view.setTitle(mRoms[position].displayName);

        WonderSwanHeader header = getHeader(position);
        if (header != null) {
            Bitmap shot = getBitmap(position);
                view.setSnap(shot);
            if (shot != null) {
                Log.d(TAG, "snap is null for " + mRoms[position].sourcefile);
            }
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
