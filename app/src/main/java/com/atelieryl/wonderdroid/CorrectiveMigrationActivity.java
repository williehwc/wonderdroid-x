package com.atelieryl.wonderdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CorrectiveMigrationActivity extends AppCompatActivity {

    private Context mContext;
    private TextView correctiveMigrationProgress;
    private int numFilesProcessing;
    private int numTotalFiles;
    private int failed;
    private String fromStoragePath;
    private String toStoragePath;
    SharedPreferences prefs;
    final int BUFFER_SIZE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrective_migration);
        mContext = this;
        correctiveMigrationProgress = findViewById(R.id.corrective_migration_progress);

        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        fromStoragePath = prefs.getString("storage_path", "");

        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        toStoragePath = getFilesDir().getPath();
        if (externalStorageVolumes.length > 0 && externalStorageVolumes[0] != null && !externalStorageVolumes[0].getPath().equals("")) {
            toStoragePath = externalStorageVolumes[0].getPath();
        }

        if (fromStoragePath.equals(toStoragePath)) {
            finish();
        }

        new CorrectiveMigrationTask().execute();
    }

    @Override
    public void onBackPressed() {}

    public class CorrectiveMigrationTask extends AsyncTask<Void, Void, Void> {

        protected void copyFilesInDir(String fromStoragePath, String toStoragePath) {
            File dir = new File(fromStoragePath);
            File[] fromFiles = dir.listFiles();
            numTotalFiles += fromFiles.length;

            for (File fromFile : fromFiles) {
                try {
                    numFilesProcessing++;
                    publishProgress();
                    if (!fromFile.exists()) {
                        continue;
                    }
                    if (fromFile.isDirectory()) {
                        String fromStoragePathDir = fromStoragePath + "/" + fromFile.getName();
                        String toStoragePathDir = toStoragePath + "/" + fromFile.getName();
                        new File(toStoragePathDir).mkdirs();
                        copyFilesInDir(fromStoragePathDir, toStoragePathDir);
                        continue;
                    }
                    if (fromFile.isFile()) {
                        File toFile = new File(toStoragePath + "/" + fromFile.getName());
                        InputStream in = mContext.getContentResolver().openInputStream(Uri.fromFile(fromFile));
                        OutputStream out = mContext.getContentResolver().openOutputStream(Uri.fromFile(toFile));
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        // write output file
                        out.flush();
                        out.close();
                        // delete original file
                        fromFile.delete();
                    }
                } catch (IOException e) {
                    failed++;
                }
            }
        }
        
        @Override
        protected Void doInBackground(Void... voids) {
            copyFilesInDir(fromStoragePath, toStoragePath);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (failed > 0) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.important)
                        .setMessage("" + failed + " file(s) failed to migrate. Please use a file browser like X-plore to manually move these files from " + fromStoragePath + " to " + toStoragePath + ". Sorry for the inconvenience.")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("storage_path", toStoragePath);
                editor.commit();
                finish();
            }
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
            correctiveMigrationProgress.setText("Processing " + (numFilesProcessing) + "/" + numTotalFiles);
        }

    }

}
