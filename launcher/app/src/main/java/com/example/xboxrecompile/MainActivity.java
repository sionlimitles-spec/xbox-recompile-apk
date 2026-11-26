package com.example.xboxrecompile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.InputDevice;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_IMPORT = 1001;

    private TextView statusBar;
    private TextView importResult;
    private LinearLayout importCard;
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
        importCard = findViewById(R.id.import_card);
        importResult = findViewById(R.id.import_result);
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

    public void onPlayClick(View view) {
        setStatus(getString(R.string.state_import));
    }

    public void onImportClick(View view) {
        // Buka file picker sistem — file TIDAK disimpan ke repo, hanya dibaca.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/octet-stream",
                "application/x-xex",
                "application/x-xbe",
                "application/zip",
                "*/*"
        });
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            showImport(getString(R.string.import_error));
            return;
        }

        showImport(getString(R.string.import_pending));
        new Thread(() -> {
            String report = inspectUri(uri);
            runOnUiThread(() -> showImport(report));
        }).start();
    }

    /** Baca metadata lokal: nama, ukuran, SHA-256, 16 byte pertama. Tidak ada upload. */
    private String inspectUri(Uri uri) {
        String name = "?";
        long size = -1;

        try (Cursor cursor = getContentResolver().query(
                uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIdx >= 0) {
                    name = cursor.getString(nameIdx);
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    size = cursor.getLong(sizeIdx);
                }
            }
        } catch (Exception ignored) {
            // kolom bisa tidak tersedia; lanjut tanpa nama/ukuran
        }

        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return getString(R.string.import_error);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] first = new byte[16];
            int firstRead = 0;
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buffer)) > 0) {
                if (firstRead < first.length) {
                    int take = Math.min(first.length - firstRead, n);
                    System.arraycopy(buffer, 0, first, firstRead, take);
                    firstRead += take;
                }
                digest.update(buffer, 0, n);
                total += n;
            }

            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < firstRead; i++) {
                hex.append(String.format(Locale.US, "%02X", first[i]));
            }

            if (size < 0) {
                size = total;
            }

            return "nama: " + name + "\n"
                    + "ukuran: " + size + " byte\n"
                    + "sha256: " + toHex(digest.digest()) + "\n"
                    + "16 byte: " + hex + "\n"
                    + getString(R.string.import_legal);
        } catch (Exception error) {
            return getString(R.string.import_error) + "\n" + error.getClass().getSimpleName();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }

    private void showImport(String text) {
        if (importCard == null || importResult == null) {
            return;
        }
        importResult.setText(text);
        importCard.setVisibility(View.VISIBLE);
        importCard.startAnimation(AnimationUtils.loadAnimation(this, R.animator.fade_in));
        setStatus(getString(R.string.state_import));
    }

    private void setStatus(String text) {
        statusBar.setText(text);
        statusBar.startAnimation(AnimationUtils.loadAnimation(this, R.animator.fade_in));
    }
}
