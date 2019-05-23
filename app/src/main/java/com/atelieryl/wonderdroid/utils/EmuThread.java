
package com.atelieryl.wonderdroid.utils;

import com.atelieryl.wonderdroid.Button;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

public class EmuThread extends Thread {

    @SuppressWarnings("unused")
    private static final String TAG = EmuThread.class.getSimpleName();

    public static interface Renderer {
        public void start ();

        public void setButtons (Button[] buttons);

        public void showButtons (boolean show);

        public void update (boolean skip);

        public void render (SurfaceHolder surfaceHolder);
    }

    private Renderer renderer;

    private static final long[] TARGETFRAMETIME = {12, 13};
    private static int frameTimeIndex = 0;

    private boolean mIsRunning = false;
    private boolean isPaused = false;

    private SurfaceHolder mSurfaceHolder;

    private Canvas c;

    private int frame;
    private long frameStart;
    private long frameEnd;
    private long nextUpdateTime;
    private int frametime;

    boolean skip = false;
    boolean behind = false;

    private int frameskip = 0;

    public EmuThread (Renderer renderer) {
        this.renderer = renderer;
    }

    public void setSurfaceHolder (SurfaceHolder sh) {
        mSurfaceHolder = sh;
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setFormat(PixelFormat.RGB_565);
        }
    }

    public void pause () {
        isPaused = true;
    }

    public void unpause () {
        isPaused = false;
        // if (WonderSwan.audio.getState() == AudioTrack.PLAYSTATE_PAUSED) {
        // WonderSwan.audio.play();
        // }
    }

    @Override
    public void run () {

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);

        while (mSurfaceHolder == null) {
            SystemClock.sleep(20);
        }

        while (mIsRunning) {

            if (isPaused) {
                //Log.d(TAG, "Paused!!!");
                SystemClock.sleep(TARGETFRAMETIME[0]);
            } else {

                frameStart = System.currentTimeMillis();

                renderer.update(frame % 2 != 0);

                if (frameskip == 0 || frame % frameskip != 0) {
                    renderer.render(mSurfaceHolder);
                }

                frameEnd = System.currentTimeMillis();
                frametime = (int)(frameEnd - frameStart);

                if (frametime < TARGETFRAMETIME[frameTimeIndex]) {
                    SystemClock.sleep(TARGETFRAMETIME[frameTimeIndex] - frametime);
                } else if (frametime > TARGETFRAMETIME[frameTimeIndex]) {
                    //Log.d(TAG, "Overtime " + frametime);
                }

                frameTimeIndex++;
                if (frameTimeIndex >= TARGETFRAMETIME.length) {
                    frameTimeIndex = 0;
                }

                frame++;
            }

        }

        synchronized (this) {
            notifyAll();
        }

    }

    public boolean isRunning () {
        return mIsRunning;
    }

    public void setRunning () {
        mIsRunning = true;
        renderer.start();
    }

    public void clearRunning () {
        mIsRunning = false;
    }

    public void setFrameskip(int frameskip) {
        this.frameskip = frameskip;
    }

}
