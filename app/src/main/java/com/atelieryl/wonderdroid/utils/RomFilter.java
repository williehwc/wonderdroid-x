
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
                if (extension.compareTo("wsc") == 0 || extension.compareTo("ws") == 0
                        || extension.compareTo("zip") == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
