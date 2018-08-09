
package com.atelieryl.wonderdroid.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import android.util.Log;

public class ZipUtils {

    private static final String TAG = ZipUtils.class.getSimpleName();

    public static String[] getValidEntries(ZipFile zip, String[] validExtensions) {

        ArrayList<String> validEntries = new ArrayList<String>();

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            String thisEntry = entries.nextElement().getName();
            for (String extension : validExtensions) {
                if (thisEntry.endsWith(extension)) {
                    validEntries.add(thisEntry);
                }
            }
        }

        return validEntries.toArray(new String[0]);
    }

    public static ZipEntry getEntry(ZipFile zip, String wantedFile) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().compareTo(wantedFile) == 0) {
                return entry;
            }
        }
        return null;
    }

    public static byte[] getBytesFromEntry(ZipFile zip, ZipEntry entry, long offset, int len) {
        byte[] bytes = new byte[len];

        try {
            InputStream is = zip.getInputStream(entry);
            is.skip(offset);
            is.read(bytes);
            return bytes;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static boolean extractFile(ZipFile zip, ZipEntry entry, File target) {
        try {
            Log.d(TAG, "extracting " + entry.getName());
            byte[] buffer = new byte[1024];
            InputStream is = zip.getInputStream(entry);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
            bos.close();
            Log.d(TAG, "Done!");
            return true;
        } catch (Exception ex) {
            Log.d(TAG, "Failed!");
            ex.printStackTrace();
            return false;
        }
    }

}
