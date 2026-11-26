package com.braid.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Launcher Braid Android.
 *
 * Fokus repo ini: target Braid (Xbox 360 / XBLA). Activity ini sengaja tanpa
 * dependensi AndroidX agar build stub tetap ringan.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_IMPORT = 1001;
    private static final String KEY_IMPORT_REPORT = "import_report";
    private static final long PROGRESS_STEP_BYTES = 16L * 1024L * 1024L;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView statusBar;
    private TextView importResult;
    private TextView importProgress;
    private LinearLayout importCard;
    private Button btnPlay;
    private Button btnImport;

    private volatile int importToken = 0;
    private volatile boolean importing = false;
    private String lastImportReport = "";
    private boolean gamepadFound = false;

    private final InputManager.InputDeviceListener deviceListener =
            new InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                    refreshStatus();
                }

                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    refreshStatus();
                }

                @Override
                public void onInputDeviceChanged(int deviceId) {
                    refreshStatus();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyImmersiveMode();

        statusBar = findViewById(R.id.status_bar);
        importCard = findViewById(R.id.import_card);
        importResult = findViewById(R.id.import_result);
        importProgress = findViewById(R.id.import_progress);
        btnPlay = findViewById(R.id.btn_play);
        btnImport = findViewById(R.id.btn_import);

        // Listener eksplisit untuk SEMUA elemen yang terlihat bisa ditekan.
        // Sebelumnya kartu atas dan ikon bulat tidak punya handler sama sekali,
        // sehingga terasa "tidak bisa diklik".
        bindClick(R.id.card_collection, view -> showCollectionInfo());
        bindClick(R.id.card_braid, view -> selectBraidTarget());
        bindClick(R.id.btn_info, view -> showAbout());
        bindClick(R.id.btn_settings, view -> showDiagnostics());
        bindClick(R.id.btn_refresh, view -> {
            refreshStatus();
            toast(R.string.toast_refreshed);
        });
        bindClick(R.id.hint_play, view -> onPlay());
        bindClick(R.id.hint_back, view -> finish());

        if (btnPlay != null) {
            btnPlay.setOnClickListener(view -> onPlay());
        }
        if (btnImport != null) {
            btnImport.setOnClickListener(view -> openImportPicker());
        }

        BraidNative.load();

        if (savedInstanceState != null) {
            lastImportReport = savedInstanceState.getString(KEY_IMPORT_REPORT, "");
            if (!lastImportReport.isEmpty() && importCard != null && importResult != null) {
                importCard.setVisibility(View.VISIBLE);
                importResult.setText(lastImportReport);
            }
        }

        refreshStatus();
    }

    private void bindClick(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            im.registerInputDeviceListener(deviceListener, null);
        }
        refreshStatus();
    }

    @Override
    protected void onPause() {
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            im.unregisterInputDeviceListener(deviceListener);
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_IMPORT_REPORT, lastImportReport);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            onPlay();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ===== Status & gamepad =====

    private void refreshStatus() {
        if (statusBar == null || importing) {
            return;
        }

        gamepadFound = detectGamepad();
        int message;
        if (!BraidNative.isLoaded()) {
            message = R.string.state_not_ready;
        } else if (BraidNative.isStub()) {
            message = gamepadFound ? R.string.state_stub_gamepad : R.string.state_stub;
        } else if (gamepadFound) {
            message = R.string.state_ready;
        } else {
            message = R.string.state_ready_no_gamepad;
        }
        setStatus(getString(message));
    }

    private boolean detectGamepad() {
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im == null) {
            return false;
        }
        for (int deviceId : im.getInputDeviceIds()) {
            InputDevice device = im.getInputDevice(deviceId);
            if (device == null) {
                continue;
            }
            if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                return true;
            }
        }
        return false;
    }

    private List<String> gamepadNames() {
        List<String> names = new ArrayList<>();
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im == null) {
            return names;
        }
        for (int deviceId : im.getInputDeviceIds()) {
            InputDevice device = im.getInputDevice(deviceId);
            if (device == null) {
                continue;
            }
            if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                names.add(device.getName());
            }
        }
        return names;
    }

    private void setStatus(String text) {
        if (statusBar == null) {
            return;
        }
        statusBar.setText(text);
        statusBar.startAnimation(AnimationUtils.loadAnimation(this, R.animator.fade_in));
    }

    // ===== Aksi tombol =====

    private void onPlay() {
        if (!BraidNative.isLoaded()) {
            toast(R.string.toast_no_library);
            showDialog(getString(R.string.dialog_no_library_title),
                    getString(R.string.dialog_no_library_body));
            return;
        }
        if (BraidNative.isStub()) {
            toast(R.string.toast_stub);
            showDialog(getString(R.string.dialog_stub_title),
                    getString(R.string.dialog_stub_body));
            return;
        }

        // Runtime nyata: serahkan ke layar game yang punya Surface, loop frame,
        // dan input. start() dipanggil oleh GameActivity, bukan di sini.
        startActivity(new Intent(this, GameActivity.class));
        setStatus(getString(R.string.state_opening_game));
    }

    private void openImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(intent, REQUEST_IMPORT);
            return;
        } catch (ActivityNotFoundException | SecurityException first) {
            // Beberapa perangkat tidak punya DocumentsUI / provider SAF.
            // Fallback ke GET_CONTENT agar tombol tidak "mati" atau crash.
        }

        Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
        fallback.addCategory(Intent.CATEGORY_OPENABLE);
        fallback.setType("*/*");
        fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(fallback, REQUEST_IMPORT);
        } catch (ActivityNotFoundException | SecurityException second) {
            showImportError(getString(R.string.import_no_picker));
            toast(R.string.import_no_picker);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            showImportError(getString(R.string.import_error));
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Tidak semua provider mendukung persistable permission; baca tetap jalan.
        }

        startInspection(uri);
    }

    // ===== Import lokal (tanpa upload / tanpa simpan ke repo) =====

    private void startInspection(Uri uri) {
        final int token = ++importToken;
        importing = true;

        if (importCard != null) {
            importCard.setVisibility(View.VISIBLE);
        }
        if (importResult != null) {
            importResult.setText(R.string.import_pending);
        }
        if (importProgress != null) {
            importProgress.setText("");
        }
        setStatus(getString(R.string.state_importing));

        Thread worker = new Thread(() -> {
            String report = inspectUri(uri, token);
            ui.post(() -> {
                if (token != importToken || isFinishing()) {
                    return;
                }
                importing = false;
                lastImportReport = report;
                if (importResult != null) {
                    importResult.setText(report);
                }
                if (importProgress != null) {
                    importProgress.setText("");
                }
                if (importCard != null) {
                    importCard.startAnimation(
                            AnimationUtils.loadAnimation(this, R.animator.fade_in));
                }
                setStatus(getString(R.string.state_import_done));
            });
        }, "braid-import");
        worker.start();
    }

    /**
     * Baca metadata file game di perangkat: nama, ukuran, SHA-256, magic bytes.
     * Tidak ada upload, tidak ada penyalinan ke repo.
     */
    private String inspectUri(Uri uri, int token) {
        String name = "?";
        long declaredSize = -1;

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIdx >= 0) {
                    name = cursor.getString(nameIdx);
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    declaredSize = cursor.getLong(sizeIdx);
                }
            }
        } catch (Exception ignored) {
            // Kolom metadata tidak selalu tersedia.
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
            long lastProgress = 0;
            int n;

            while ((n = in.read(buffer)) != -1) {
                if (firstRead < first.length) {
                    int take = Math.min(first.length - firstRead, n);
                    System.arraycopy(buffer, 0, first, firstRead, take);
                    firstRead += take;
                }
                digest.update(buffer, 0, n);
                total += n;

                if (total - lastProgress >= PROGRESS_STEP_BYTES) {
                    lastProgress = total;
                    final long snapshot = total;
                    ui.post(() -> {
                        if (token == importToken && importProgress != null) {
                            importProgress.setText(
                                    Formatter.formatShortFileSize(this, snapshot));
                        }
                    });
                }
            }

            long size = declaredSize >= 0 ? declaredSize : total;
            String magic = describeMagic(first, firstRead);
            String extension = extensionOf(name);

            StringBuilder report = new StringBuilder();
            report.append("nama: ").append(name).append('\n');
            report.append("ukuran: ")
                    .append(Formatter.formatShortFileSize(this, size))
                    .append(" (").append(size).append(" byte)\n");
            report.append("sha256: ").append(toHex(digest.digest())).append('\n');
            report.append("magic: ").append(magic).append('\n');
            report.append("16 byte: ").append(toHex(first, firstRead)).append('\n');
            report.append("ekstensi: ")
                    .append(extension.isEmpty() ? "-" : extension).append('\n');

            String note = formatNote(magic, extension);
            if (!note.isEmpty()) {
                report.append("catatan: ").append(note).append('\n');
            }
            report.append('\n').append(getString(R.string.import_legal));
            return report.toString();
        } catch (SecurityException denied) {
            return getString(R.string.import_denied);
        } catch (Exception error) {
            return getString(R.string.import_error) + "\n"
                    + error.getClass().getSimpleName()
                    + (error.getMessage() == null ? "" : ": " + error.getMessage());
        }
    }

    private String formatNote(String magic, String extension) {
        if (magic.startsWith("XBE")) {
            return "Ini XBE (Xbox original), bukan target repo ini (Xbox 360 / XEX).";
        }
        if (magic.startsWith("CON") || magic.startsWith("STFS")
                || magic.startsWith("PIRS") || magic.startsWith("LIVE")) {
            return "Container paket Xbox 360. Ekstrak XEX dari salinan sah Anda dengan tool Anda sendiri.";
        }
        if (magic.startsWith("XEX")) {
            return "XEX terdeteksi. Langkah berikutnya: analisa impor XDK lalu recompile (docs/BRAID-REQUIREMENTS.md).";
        }
        if (magic.equals("ZIP") || magic.equals("RAR") || magic.equals("7z")) {
            return "Arsip terkompresi. Ekstrak dulu, lalu impor file XEX-nya.";
        }
        if ("iso".equals(extension)) {
            return "Disc image. Ambil XEX dari salinan sah Anda dengan tool Anda sendiri.";
        }
        if (magic.equals("tidak dikenal") && !extension.isEmpty()) {
            return "Format belum dikenali. Pastikan ini salinan Braid yang sah (XBLA).";
        }
        return "";
    }

    private void showImportError(String text) {
        importing = false;
        if (importCard != null) {
            importCard.setVisibility(View.VISIBLE);
        }
        if (importResult != null) {
            importResult.setText(text);
        }
        if (importProgress != null) {
            importProgress.setText("");
        }
        lastImportReport = text;
        setStatus(getString(R.string.state_import_done));
    }

    // ===== Dialog & info =====

    private void selectBraidTarget() {
        toast(R.string.toast_braid_selected);
        setStatus(getString(R.string.state_braid_selected));
    }

    private void showCollectionInfo() {
        showDialog(getString(R.string.dialog_collection_title),
                getString(R.string.dialog_collection_body));
    }

    private void showAbout() {
        showDialog(getString(R.string.dialog_about_title),
                getString(R.string.dialog_about_body));
    }

    private void showDiagnostics() {
        StringBuilder text = new StringBuilder();
        text.append("Paket: ").append(getPackageName()).append('\n');
        text.append("Versi: ").append(appVersion()).append('\n');
        text.append("ABI: ")
                .append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?")
                .append('\n');
        text.append("Android: ").append(Build.VERSION.RELEASE).append('\n');

        if (!BraidNative.isLoaded()) {
            text.append("Native: tidak termuat\n");
        } else if (BraidNative.isStub()) {
            text.append("Native: stub ").append(BraidNative.version()).append('\n');
        } else {
            text.append("Native: siap ").append(BraidNative.version()).append('\n');
        }

        List<String> pads = gamepadNames();
        if (pads.isEmpty()) {
            text.append("Gamepad: tidak ada\n");
        } else {
            text.append("Gamepad: ").append(pads.size()).append('\n');
            for (String pad : pads) {
                text.append("  • ").append(pad).append('\n');
            }
        }

        if (!lastImportReport.isEmpty()) {
            text.append("Import terakhir: ada (lihat kartu import)\n");
        } else {
            text.append("Import terakhir: belum ada\n");
        }

        showDialog(getString(R.string.dialog_diagnostics_title), text.toString());
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String appVersion() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException error) {
            return "?";
        }
    }

    private void toast(int message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ===== Utilitas file =====

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.US);
    }

    private static String describeMagic(byte[] first, int length) {
        if (startsWith(first, length, "XBEH")) {
            return "XBE (Xbox original)";
        }
        if (startsWith(first, length, "XEX2")
                || startsWith(first, length, "XEX1")
                || startsWith(first, length, "XEX%")) {
            return "XEX (Xbox 360)";
        }
        if (startsWith(first, length, "CON ")) {
            return "CON (container Xbox 360)";
        }
        if (startsWith(first, length, "STFS")
                || startsWith(first, length, "PIRS")
                || startsWith(first, length, "LIVE")) {
            return "STFS/PIRS/LIVE (paket Xbox 360)";
        }
        if (startsWith(first, length, "PK\u0003\u0004")) {
            return "ZIP";
        }
        if (startsWith(first, length, "Rar!")) {
            return "RAR";
        }
        if (startsWith(first, length, "7z\u00BC\u00AF")) {
            return "7z";
        }
        if (length >= 4 && (first[0] & 0xFF) == 0x7F
                && first[1] == 'E' && first[2] == 'L' && first[3] == 'F') {
            return "ELF";
        }
        return "tidak dikenal";
    }

    private static boolean startsWith(byte[] data, int length, String ascii) {
        if (length < ascii.length()) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if ((data[i] & 0xFF) != (ascii.charAt(i) & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    private static String toHex(byte[] bytes) {
        return toHex(bytes, bytes.length);
    }

    private static String toHex(byte[] bytes, int length) {
        StringBuilder builder = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            builder.append(String.format(Locale.US, "%02x", bytes[i]));
        }
        return builder.toString();
    }
}
