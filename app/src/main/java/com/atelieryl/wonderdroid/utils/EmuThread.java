
package com.atelieryl.wonderdroid.utils;

import com.atelieryl.wonderdroid.Button;
import com.atelieryl.wonderdroid.R;

import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class EmuThread extends Thread {

    @SuppressWarnings("unused")
    private static final String TAG = EmuThread.class.getSimpleName();

    public static interface Renderer {
        public void start ();

        public void setButtons (Button[] buttons);

        public void showButtons (boolean show);

        public int update (boolean skip);

        public void render (SurfaceHolder surfaceHolder);
    }

    private Renderer renderer;

    private static long targetFrameTime;

    private boolean mIsRunning = false;
    private boolean isPaused = false;

    private SurfaceHolder mSurfaceHolder;

    private Canvas c;

    private int frame;
    private long frameStart;
    private long frameRenderTime;
    private long nextUpdateTime;
    private long sleepTimeMillis;

    boolean skip = false;
    boolean behind = false;

    private int frameskip = 0;
    private final double mMasterClockNano;
    private int expectedCyclesPerFrameLowerBound;
    private int expectedCyclesPerFrameUpperBound;
    private short numConsecFramesWithUnexpectedCycles;

    private final short numConsecFramesWithUnexpectedCyclesThreshold = 10;
    private final double sleepTimeMillisFactor = 0.66;

    public EmuThread (Renderer renderer, long masterClock, int fps) {
        this.renderer = renderer;
        mMasterClockNano = masterClock / 1000000000.;
        double expectedCyclesPerFrame = masterClock / (fps / 65536. / 256.);
        expectedCyclesPerFrameLowerBound = (int) (expectedCyclesPerFrame * 0.95);
        expectedCyclesPerFrameUpperBound = (int) (expectedCyclesPerFrame * 1.05);
        targetFrameTime = (long) (1000000000. / (fps / 65536. / 256.)); // in nanoseconds
        calculateSleepTimeMillis();
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
                SystemClock.sleep(100);
            } else {

                frameStart = System.nanoTime();

                int cycles = renderer.update(false/*frame % 2 != 0*/);

                if (frameskip == 0 || frame % frameskip != 0) {
                    renderer.render(mSurfaceHolder);
                }

                frameRenderTime = System.nanoTime() - frameStart;

                targetFrameTime = (long) (cycles / mMasterClockNano);

                if (cycles > 0) {

                    if (cycles < expectedCyclesPerFrameLowerBound || cycles > expectedCyclesPerFrameUpperBound) {
                        numConsecFramesWithUnexpectedCycles++;
                        if (numConsecFramesWithUnexpectedCycles >= numConsecFramesWithUnexpectedCyclesThreshold) {
                            calculateSleepTimeMillis();
                            expectedCyclesPerFrameLowerBound = (int) (cycles * 0.95);
                            expectedCyclesPerFrameUpperBound = (int) (cycles * 1.05);
                            numConsecFramesWithUnexpectedCycles = 0;
                        }
                    }

                    nextUpdateTime = frameStart + targetFrameTime - frameRenderTime / 100;

                    SystemClock.sleep(sleepTimeMillis);

                    while (System.nanoTime() < nextUpdateTime) {}

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

    private void calculateSleepTimeMillis() {
        sleepTimeMillis = (long) (targetFrameTime / 1000000. * sleepTimeMillisFactor);
    }

}
