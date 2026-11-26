package com.braid.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

/**
 * Layar game. Hanya dipakai bila libbraid.so nyata (bukan stub) berhasil dimuat.
 *
 * Activity ini tidak tahu apa-apa soal Braid — dia cuma menyediakan:
 *  - Surface / ANativeWindow untuk renderer native
 *  - loop frame 60 fps
 *  - state tombol & axis (mirip XInput) ke native
 *  - siklus audio native
 */
public class GameActivity extends Activity implements SurfaceHolder.Callback {

    public static final int BTN_A = 0;
    public static final int BTN_B = 1;
    public static final int BTN_X = 2;
    public static final int BTN_Y = 3;
    public static final int BTN_LB = 4;
    public static final int BTN_RB = 5;
    public static final int BTN_UP = 6;
    public static final int BTN_DOWN = 7;
    public static final int BTN_LEFT = 8;
    public static final int BTN_RIGHT = 9;
    public static final int BTN_START = 10;
    public static final int BTN_TOUCH = 11;

    private static final long FRAME_INTERVAL_MS = 1000L / 60L;

    private SurfaceView surfaceView;
    private TextView hint;
    private Thread frameThread;
    private volatile boolean running = false;

    private final Runnable frameLoop = () -> {
        while (running) {
            long start = System.nanoTime();
            BraidNative.frame(System.currentTimeMillis());
            long spentMs = (System.nanoTime() - start) / 1_000_000L;
            long sleepMs = FRAME_INTERVAL_MS - spentMs;
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        surfaceView = findViewById(R.id.game_surface);
        hint = findViewById(R.id.game_hint);

        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener((view, event) -> {
            handleTouch(event);
            return true;
        });
        surfaceView.setFocusable(true);
        surfaceView.requestFocus();

        if (!BraidNative.isLoaded() || BraidNative.isStub()) {
            failAndClose(getString(R.string.game_error_no_runtime));
            return;
        }

        int result = BraidNative.start();
        if (result != 0) {
            failAndClose(getString(R.string.game_error_start) + "\n" + BraidNative.lastError());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BraidNative.audioInit();
    }

    @Override
    protected void onPause() {
        BraidNative.audioShutdown();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopFrameLoop();
        BraidNative.stop();
        super.onDestroy();
    }

    private void failAndClose(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.game_error_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    // ===== SurfaceHolder.Callback =====

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        BraidNative.surfaceCreated(holder.getSurface());
        running = true;
        frameThread = new Thread(frameLoop, "braid-frame");
        frameThread.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (hint != null) {
                hint.setVisibility(View.GONE);
            }
        }, 3000);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        BraidNative.surfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopFrameLoop();
        BraidNative.surfaceDestroyed();
    }

    private void stopFrameLoop() {
        running = false;
        Thread thread = frameThread;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            frameThread = null;
        }
    }

    // ===== Input =====

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int button = buttonFor(keyCode);
        if (button >= 0) {
            BraidNative.setButton(button, true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int button = buttonFor(keyCode);
        if (button >= 0) {
            BraidNative.setButton(button, false);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private static int buttonFor(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                return BTN_A;
            case KeyEvent.KEYCODE_BUTTON_B:
                return BTN_B;
            case KeyEvent.KEYCODE_BUTTON_X:
                return BTN_X;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return BTN_Y;
            case KeyEvent.KEYCODE_BUTTON_L1:
                return BTN_LB;
            case KeyEvent.KEYCODE_BUTTON_R1:
                return BTN_RB;
            case KeyEvent.KEYCODE_DPAD_UP:
                return BTN_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return BTN_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return BTN_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return BTN_RIGHT;
            case KeyEvent.KEYCODE_BUTTON_START:
                return BTN_START;
            default:
                return -1;
        }
    }

    private void handleTouch(MotionEvent event) {
        if (surfaceView.getWidth() <= 0 || surfaceView.getHeight() <= 0) {
            return;
        }
        int action = event.getActionMasked();
        float x = clamp(event.getX(0) / surfaceView.getWidth() * 2f - 1f);
        float y = clamp(event.getY(0) / surfaceView.getHeight() * 2f - 1f);
        BraidNative.setAxis(0, x);
        BraidNative.setAxis(1, y);
        boolean pressed = action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_CANCEL;
        BraidNative.setButton(BTN_TOUCH, pressed);
    }

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
