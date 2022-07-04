
package com.atelieryl.wonderdroid.utils;

import java.io.File;
import java.io.FileFilter;

public class RomFilter implements FileFilter {
    private boolean mZipOnly;
    private boolean mWsOnly;
    private boolean mBoxArt;

    public RomFilter(boolean zipOnly, boolean wsOnly, boolean boxArt) {
        mZipOnly = zipOnly;
        mWsOnly = wsOnly;
        mBoxArt = boxArt;
    }

    @Override
    public boolean accept(File pathname) {
        if (pathname.isFile()) {
            String[] parts = pathname.getName().split("\\.");
            if (parts.length >= 2) {
                String extension = parts[parts.length - 1];
                if (mBoxArt) {
                    for (String ext : RomAdapter.Rom.boxArtExtensions) {
                        if (extension.compareTo(ext) == 0) {
                            return true;
                        }
                    }
                } else {
                    if (extension.compareTo("zip") == 0) {
                        return true;
                    }
                    if (!mZipOnly) {
                        for (String ext : RomAdapter.Rom.allRomExtensions) {
                            if (extension.compareTo(ext) == 0) {
                                return true;
                            }
                        }
                    } else if (mWsOnly) {
                        for (String ext : RomAdapter.Rom.wsRomExtensions) {
                            if (extension.compareTo(ext) == 0) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
