
package com.atelieryl.wonderdroid.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import android.content.Context;
import android.util.Log;

public class ZipCache {

	private static final String TAG = ZipCache.class.getSimpleName();
	private static final int MAXFILES = 5;

	public static File getFile (Context context, ZipFile zip, String wantedFile, String[] extensionsToUnpack) {

		Log.d(TAG, "Someone is asking for " + wantedFile + " from " + zip.getName());

		String shortName = zip.getName().replaceAll(".*/", "");

		File cacheDir = getCacheDir(context);
		File zipDir = new File(cacheDir, shortName);
		File cachedFile;

		if (zipDir.exists()) {
			cachedFile = new File(zipDir, wantedFile);
			if (!cachedFile.exists()) {
				throw new IllegalStateException();
			}
			Log.d(TAG, "Returning file from cache");
			zipDir.setLastModified(System.currentTimeMillis());
			return cachedFile;
		} else {
			prune(context);
			Log.d(TAG, shortName + " hasn't been unpacked yet.. doing it now");
			zipDir.mkdir();
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File target = new File(zipDir, entry.getName());
				for (String extension : extensionsToUnpack) {
					if (entry.getName().endsWith(extension)) {
						ZipUtils.extractFile(zip, entry, target);
						break;
					}
				}
			}

			cachedFile = new File(zipDir, wantedFile);
			if (cachedFile.exists()) {
				return cachedFile;
			}
		}

		return null;
	}

	public static boolean isZipInCache (Context context, ZipFile zip) {
		String shortName = zip.getName().replaceAll(".*/", "");
		File cacheDir = getCacheDir(context);
		File zipDir = new File(cacheDir, shortName);
		return zipDir.exists();
	}

	public synchronized static void prune (Context context) {
		File cacheDir = getCacheDir(context);
		File[] list = cacheDir.listFiles();
		Arrays.sort(list, new LastModifiedFileComparator());
		for (int i = 0; i < list.length; i++) {
			File file = list[i];
			if (i < list.length - MAXFILES) {
				deleteDiretory(file);
			}
		}

	}

	public synchronized static void clean (Context context) {
		File cacheDir = getCacheDir(context);
		File[] list = cacheDir.listFiles();
		long oneWeekAgo = System.currentTimeMillis() - (1000 * (60 * 60 * 24 * 7));

		Arrays.sort(list, new LastModifiedFileComparator());

		for (int i = 0; i < list.length; i++) {
			File file = list[i];
			if (i < list.length - MAXFILES || file.lastModified() < oneWeekAgo) {
				deleteDiretory(file);
			}
		}
	}

	private static void deleteDiretory (File file) {
		Log.d(TAG, "Deleting " + file.getName());
		if (!file.exists()) {
			throw new IllegalArgumentException();
		}
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void dumpInfo (Context context) {
		File cacheDir = getCacheDir(context);
		String[] list = cacheDir.list();
		Log.d(TAG, "Have " + list.length + " extracted zips in cache");
		for (String file : list) {
			Log.d(TAG, file);
		}
	}

	private static File getCacheDir (Context context) {
		File cacheDir = new File(context.getFilesDir(), "zipcache");
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		return cacheDir;
	}

}
