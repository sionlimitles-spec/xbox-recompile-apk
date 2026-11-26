package com.example.xboxrecompile;

import android.app.Activity;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView statusView;

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
        statusView = findViewById(R.id.status);
        refresh();
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
        if (statusView == null) {
            return;
        }

        StringBuilder text = new StringBuilder("Xbox Recompile Launcher\n\n");
        text.append("Gamepad terdeteksi:\n");

        boolean found = false;
        for (InputDevice device : InputDevice.getDevices()) {
            if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                found = true;
                text.append(String.format(Locale.getDefault(),
                        "- %s (vendor 0x%04X, product 0x%04X)\n",
                        device.getName(), device.getVendorId(), device.getProductId()));
            }
        }

        if (!found) {
            text.append("- Belum ada. Hubungkan gamepad Xbox via Bluetooth/OTG.\n");
        }

        text.append("\nStatus: stub launcher. Tambahkan native library hasil recompile "
                + "di jniLibs/arm64-v8a/ untuk menjalankan game.");
        statusView.setText(text.toString());
    }
}
