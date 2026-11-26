package com.example.xboxrecompile;

import android.app.Activity;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView gamepadStatusView;
    private TextView libraryStatusView;
    private TextView targetStatusView;
    private TextView hintView;
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

        gamepadStatusView = findViewById(R.id.gamepad_status);
        libraryStatusView = findViewById(R.id.library_status);
        targetStatusView = findViewById(R.id.target_status);
        hintView = findViewById(R.id.hint);

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
        if (gamepadStatusView == null) {
            return;
        }

        boolean gamepadFound = false;
        StringBuilder devices = new StringBuilder();
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            for (int deviceId : im.getInputDeviceIds()) {
                InputDevice device = im.getInputDevice(deviceId);
                if (device == null) {
                    continue;
                }
                if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                    gamepadFound = true;
                    devices.append(String.format(Locale.getDefault(),
                            "- %s (vendor 0x%04X, product 0x%04X)\n",
                            device.getName(), device.getVendorId(), device.getProductId()));
                }
            }
        }

        gamepadStatusView.setText(
                gamepadFound ? devices.toString() : getString(R.string.gamepad_empty));
        libraryStatusView.setText(getString(nativeLibraryLoaded
                ? R.string.library_ready
                : R.string.library_missing));
        targetStatusView.setText(getString(R.string.target_value));
        hintView.setText(buildStateHint(gamepadFound));
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
}
