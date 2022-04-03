
package com.atelieryl.wonderdroid;

import com.atelieryl.wonderdroid.utils.ZipCache;
import com.downloader.PRDownloader;

import android.app.Application;
import android.os.Environment;

import java.io.File;

//@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=30ee9348", formKey = "")
public class WonderDroid extends Application {

    //public static final String DIRECTORY = "/wonderdroid/";

    //public static final String CARTMEMDIRECTORY = DIRECTORY + "cartmem/";

    //public static final String SAVESTATEDIRECTORY = DIRECTORY + "savestates/";

    @Override
    public void onCreate() {
        super.onCreate();
        WonderSwan.outputDebugShizzle();
        ZipCache.dumpInfo(this.getBaseContext());
        ZipCache.clean(this.getBaseContext());
        PRDownloader.initialize(getApplicationContext());
    }

    public File getRomDir() {
        if (Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED) != 0)
            return null;

        return Environment.getExternalStorageDirectory();
    }

}
