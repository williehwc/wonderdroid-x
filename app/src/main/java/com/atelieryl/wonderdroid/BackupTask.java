package com.atelieryl.wonderdroid;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class BackupTask extends AsyncTask<Void, Void, Void> {

    public interface ProgressUpdater {
        void progressUpdate(String currentFile, int numFilesProcessed, int numFilesTotal);
        void taskFinished(int numFailed);
    }

    private boolean mCanceled;
    private ProgressUpdater mProgressUpdater;
    private int numFailed = 0;

    private Uri mDest;
    private ArrayList<File> mFiles;
    private boolean mMigrate;
    private Context mContext;

    private String mCurrentFile = "";
    private int mNumFilesProcessed = 0;

    final int BUFFER_SIZE = 1000;

    public BackupTask(Uri dest, ArrayList<File> files, boolean migrate, ProgressUpdater progressUpdater, Context context) {
        mDest = dest;
        mFiles = files;
        mMigrate = migrate;
        mProgressUpdater = progressUpdater;
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        DocumentFile dest;
        if (!mMigrate)
            dest = DocumentFile.fromTreeUri(mContext, mDest);
        else
            dest = DocumentFile.fromFile(new File(mDest.toString()));
        for (File file : mFiles) {
            mCurrentFile = file.getName();
            publishProgress();
            try {
                DocumentFile destFile = dest.createFile("application/octet-stream", file.getName());
                InputStream in = mContext.getContentResolver().openInputStream(Uri.fromFile(file));
                OutputStream out = mContext.getContentResolver().openOutputStream(destFile.getUri());
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (isCancelled()) {
                        out.flush();
                        out.close();
                        if (mMigrate) {
                            // Delete all files in destination
                            for (DocumentFile f : dest.listFiles()) f.delete();
                        } else {
                            // Delete partially copied file
                            destFile.delete();
                        }
                        return null;
                    }
                    out.write(buffer, 0, read);
                }
                in.close();
                // write output file
                out.flush();
                out.close();
                // delete original file
                if (mMigrate) {
                    file.delete();
                }
            } catch (Exception e) {
                numFailed++;
            }
            mNumFilesProcessed++;
        }
        return null;
    }

    @Override
    protected void onCancelled(Void result) {
        mProgressUpdater.taskFinished(numFailed);
    }

    @Override
    protected void onPostExecute(Void result) {
        mProgressUpdater.taskFinished(numFailed);
    }

    @Override
    protected void onProgressUpdate(Void... progress) {
        mProgressUpdater.progressUpdate(mCurrentFile, mNumFilesProcessed, mFiles.size());
    }
}
