
package com.atelieryl.wonderdroid;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

public class TouchInputHandler {

    private final String TAG = TouchInputHandler.class.getSimpleName();

    private final boolean hasMultiTouch;

    public TouchInputHandler(Context context) {
        hasMultiTouch = context.getPackageManager().hasSystemFeature(
                "android.hardware.touchscreen.multitouch");
        if (hasMultiTouch) {
            Log.d(TAG, "has multitouch");
        }
        for (int i = 0; i < pointers.length; i++) {
            pointers[i] = new Pointer();
        }
    }

    public static class Pointer {
        private int id = -1;

        public boolean down = false;

        public int x, y;

        public long downTime = 0;

        public int downX, downY;
    }

    public Pointer[] pointers = new Pointer[6];

    private void newPointer(int id, int x, int y) {
        for (Pointer pointer : pointers) {
            if (pointer.id == id) {
                pointer.x = x;
                pointer.y = y;
                pointer.down = true;

                pointer.downX = x;
                pointer.downY = y;
                pointer.downTime = System.currentTimeMillis();
                // Log.d(TAG, "Reusing existing pointer for " + id + " @ " + x +
                // ":" + y);
                return;
            }
        }

        for (Pointer pointer : pointers) {
            if (!pointer.down) {
                pointer.id = id;
                pointer.x = x;
                pointer.y = y;
                pointer.down = true;
                // Log.d(TAG, "Created pointer for " + id + " @ " + x + ":" +
                // y);
                return;
            }
        }
    }

    private void freePointer(int id) {
        for (Pointer pointer : pointers) {
            if (pointer.id == id) {
                pointer.down = false;
                // Log.d(TAG, "Freed pointer for " + id);
                return;
            }
        }
    }

    private void updatePointer(int id, int x, int y) {
        for (Pointer pointer : pointers) {
            if (pointer.id == id) {
                pointer.x = x;
                pointer.y = y;
                // Log.d(TAG, "Updated pointer for " + id + " @ " + x + ":" +
                // y);
                return;
            }
        }
    }

    public synchronized void onTouchEvent(MotionEvent event) {

        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (hasMultiTouch) {
            int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
            int pointerId = event.getPointerId(pointerIndex);
            int x, y;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    x = (int)event.getX(pointerIndex);
                    y = (int)event.getY(pointerIndex);
                    newPointer(pointerId, x, y);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_CANCEL:
                    freePointer(pointerId);
                    break;

                case MotionEvent.ACTION_MOVE:
                    int pointerCount = event.getPointerCount();
                    for (int i = 0; i < pointerCount; i++) {
                        pointerIndex = i;
                        pointerId = event.getPointerId(pointerIndex);
                        x = (int)event.getX(pointerIndex);
                        y = (int)event.getY(pointerIndex);
                        updatePointer(pointerId, x, y);
                    }
                    break;
            }

        } else {
            int x, y;
            x = (int)event.getX();
            y = (int)event.getY();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    newPointer(0, x, y);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_CANCEL:
                    freePointer(0);
                    break;

                case MotionEvent.ACTION_MOVE:
                    updatePointer(0, x, y);
                    break;
            }
        }

    }
}
