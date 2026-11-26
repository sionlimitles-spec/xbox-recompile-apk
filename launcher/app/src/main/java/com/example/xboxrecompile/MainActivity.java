package com.example.xboxrecompile;

import android.app.Activity;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView statusBar;
    private Button btnPlay;
    private Button btnImport;
    private ImageView heroBg;
    private boolean nativeLibraryLoaded = false;

    // Kontrak JNI minimal; diimplementasikan oleh libbraid.so hasil recompile.
    private static native int nativeStart();

    private final InputManager.InputDeviceListener deviceListener =
            new InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                    refresh();
                }

                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    refresh();
                }

                @Override
                public void onInputDeviceChanged(int deviceId) {
                    refresh();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Immersive mode: full-screen tanpa status bar Android
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        statusBar = findViewById(R.id.status_bar);
        btnPlay = findViewById(R.id.btn_play);
        btnImport = findViewById(R.id.btn_import);
        heroBg = findViewById(R.id.hero_bg);

        loadNativeLibrary();
        refresh();
    }

    private void loadNativeLibrary() {
        try {
            System.loadLibrary("braid");
            nativeLibraryLoaded = true;
        } catch (UnsatisfiedLinkError error) {
            nativeLibraryLoaded = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            im.registerInputDeviceListener(deviceListener, null);
        }
        refresh();
    }

    @Override
    protected void onPause() {
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            im.unregisterInputDeviceListener(deviceListener);
        }
        super.onPause();
    }

    private void refresh() {
        if (statusBar == null) {
            return;
        }

        boolean gamepadFound = false;
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            for (int deviceId : im.getInputDeviceIds()) {
                InputDevice device = im.getInputDevice(deviceId);
                if (device == null) {
                    continue;
                }
                if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                    gamepadFound = true;
                }
            }
        }

        String state = buildStateHint(gamepadFound);
        statusBar.setText(state);
        statusBar.startAnimation(AnimationUtils.loadAnimation(this, R.animator.fade_in));
    }

    private String buildStateHint(boolean gamepadFound) {
        if (!nativeLibraryLoaded) {
            return getString(R.string.state_not_ready);
        }
        if (!gamepadFound) {
            return getString(R.string.state_library_only);
        }
        return getString(R.string.state_ready);
    }

    // ===== Click handlers untuk tombol pill =====

    public void onPlayClick(android.view.View view) {
        statusBar.setText(getString(R.string.state_import));
        statusBar.startAnimation(AnimationUtils.loadAnimation(this, R.animator.fade_in));
    }

    public void onImportClick(android.view.View view) {
        statusBar.setText(getString(R.string.state_import));
        statusBar.startAnimation(AnimationUtils.loadAnimation(this, R.animator.fade_in));
    }
}
