
package com.atelieryl.wonderdroid.utils;

import java.io.File;
import java.io.FileFilter;

public class RomFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        if (pathname.isFile()) {
            String[] parts = pathname.getName().split("\\.");
            if (parts.length >= 2) {
                String extension = parts[parts.length - 1];
                if (extension.compareTo("zip") == 0) {
                    return true;
                }
                for (String ext : RomAdapter.Rom.allRomExtensions) {
                    if (extension.compareTo(ext) == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
