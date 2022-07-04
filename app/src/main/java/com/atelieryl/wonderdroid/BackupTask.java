package com.atelieryl.wonderdroid;

import android.net.Uri;
import android.os.AsyncTask;

public class BackupTask extends AsyncTask<Void, Void, Void> {

    public interface ProgressUpdater {
        void progressUpdate(String status, int numFilesProcessed, int numFilesTotal);
        void taskFinished();
    }

    private boolean mCanceled;
    private ProgressUpdater mProgressUpdater;

    public BackupTask(Uri dest, String rootFile, boolean migrate, ProgressUpdater progressUpdater) {

    }

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }

    @Override
    protected void onCancelled(Void result) {
        mProgressUpdater.taskFinished();
    }

    @Override
    protected void onPostExecute(Void result) {
        mProgressUpdater.taskFinished();
    }

    @Override
    protected void onProgressUpdate(Void... progress) {

    }
}
