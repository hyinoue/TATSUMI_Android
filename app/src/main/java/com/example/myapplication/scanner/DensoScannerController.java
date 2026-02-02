package com.example.myapplication.scanner;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.densowave.bhtsdk.barcode.BarcodeDataReceivedEvent;
import com.densowave.bhtsdk.barcode.BarcodeException;
import com.densowave.bhtsdk.barcode.BarcodeManager;
import com.densowave.bhtsdk.barcode.BarcodeScanner;
import com.densowave.bhtsdk.barcode.BarcodeScannerSettings;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;


/**
 * DENSO BHT SDK のスキャナ制御を共通化したコントローラ
 * <p>
 * 【責務】
 * - BarcodeManager生成、Scanner取得
 * - onResume: claim / onPause: close
 * - triggerキー(501)で wait-decode風制御
 * - onBarcodeDataReceived を受信して OnScanListener へ通知
 * - C#互換寄せの設定反映（存在する項目のみを反射でON）
 */

//============================================================
//　処理概要　:　DensoScannerControllerクラス
//============================================================

public class DensoScannerController
        implements BarcodeManager.BarcodeManagerListener, BarcodeScanner.BarcodeDataListener {

    private static final String TAG = "DensoScanner";

    // ===== Activity / callback =====
    private final Activity activity;
    private final OnScanListener scanListener;

    // ===== SDK =====
    private BarcodeManager mBarcodeManager = null;
    private BarcodeScanner mBarcodeScanner = null;
    private BarcodeScannerSettings mSettings = null;

    // ===== Trigger / WaitForDecode =====
    private int triggerKeyCode = 501;
    private long waitDecodeMs = 5000;

    private boolean triggerDown = false;
    private boolean resumed = false;

    private boolean waitingDecode = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    // ===== Duplicate guard (optional) =====
    private String lastScanned = "";

    public DensoScannerController(@NonNull Activity activity,
                                  @NonNull OnScanListener scanListener) {
        this.activity = activity;
        this.scanListener = scanListener;
    }

    // ----------------------------
    // ライフサイクル呼び出し
    // ----------------------------

    /**
     * Activity.onCreate で呼ぶ（BarcodeManager生成開始）
     */
    public void onCreate() {
        try {
            BarcodeManager.create(activity, this);
            Log.d(TAG, "BarcodeManager.create called");
        } catch (BarcodeException e) {
            Log.e(TAG, "BarcodeManager.create failed. ErrorCode=" + e.getErrorCode(), e);
        } catch (Exception e) {
            Log.e(TAG, "BarcodeManager.create failed (unexpected)", e);
        }
    }

    /**
     * Activity.onResume で呼ぶ（claim可能なら準備）
     */
    public void onResume() {
        resumed = true;

        if (mBarcodeScanner != null) {
            setupScannerIfPossible("onResume");
        }
    }

    /**
     * Activity.onPause で呼ぶ（待機停止＋claim解放）
     */
    public void onPause() {
        resumed = false;

        cancelWaitForDecode("onPause");

        try {
            if (mBarcodeScanner != null) {
                safePressTrigger(false);
                try {
                    mBarcodeScanner.removeDataListener(this);
                } catch (Exception ignored) {
                }
                safeCloseScanner("onPause");
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Activity.onDestroy で呼ぶ（破棄）
     */
    public void onDestroy() {
        cancelWaitForDecode("onDestroy");

        try {
            if (mBarcodeScanner != null) {
                safePressTrigger(false);
                safeCloseScanner("onDestroy");

                try {
                    mBarcodeScanner.destroy();
                } catch (BarcodeException e) {
                    Log.e(TAG, "Scanner destroy failed. ErrorCode=" + e.getErrorCode(), e);
                } catch (Exception e) {
                    Log.e(TAG, "Scanner destroy failed (unexpected)", e);
                }
                mBarcodeScanner = null;
            }
        } catch (Exception ignored) {
        }

        try {
            if (mBarcodeManager != null) {
                mBarcodeManager.destroy();
                mBarcodeManager = null;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Activity.dispatchKeyEvent から呼ぶ
     *
     * @return ここで処理したら true（イベント消費）
     */
    public boolean handleDispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == triggerKeyCode) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() > 0) return true;

                if (!triggerDown) {
                    triggerDown = true;
                    Log.d(TAG, "TRIGGER DOWN -> startWaitForDecode(" + waitDecodeMs + ")");
                    startWaitForDecode(waitDecodeMs);
                }
                return true;

            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                triggerDown = false;
                Log.d(TAG, "TRIGGER UP -> cancelWaitForDecode");
                cancelWaitForDecode("keyUp");
                return true;
            }
        }
        return false;
    }

    // ----------------------------
    // BarcodeManager callbacks
    // ----------------------------

    @Override
    public void onBarcodeManagerCreated(BarcodeManager barcodeManager) {
        Log.d(TAG, "onBarcodeManagerCreated()");
        mBarcodeManager = barcodeManager;

        try {
            List<BarcodeScanner> scanners = mBarcodeManager.getBarcodeScanners();
            if (scanners == null || scanners.isEmpty()) {
                Log.e(TAG, "No BarcodeScanner found.");
                return;
            }

            mBarcodeScanner = scanners.get(0);

            if (mSettings == null) {
                mSettings = mBarcodeScanner.getSettings();
            }

            Log.d(TAG, "scanner class=" + mBarcodeScanner.getClass().getName());
            Log.d(TAG, "settings class=" + (mSettings != null ? mSettings.getClass().getName() : "null"));

            if (resumed) {
                setupScannerIfPossible("onBarcodeManagerCreated");
            }

        } catch (BarcodeException e) {
            Log.e(TAG, "onBarcodeManagerCreated setup failed. ErrorCode=" + e.getErrorCode(), e);
        } catch (Exception e) {
            Log.e(TAG, "onBarcodeManagerCreated setup failed (unexpected)", e);
        }
    }

    // ----------------------------
    // Scanner setup
    // ----------------------------

    private void setupScannerIfPossible(String from) {
        try {
            if (mBarcodeScanner == null) return;

            // clean
            try {
                mBarcodeScanner.removeDataListener(this);
            } catch (Exception ignored) {
            }
            mBarcodeScanner.addDataListener(this);

            if (mSettings == null) {
                mSettings = mBarcodeScanner.getSettings();
            }

            applyCSharpLikeSettingsReflective(mSettings);
            mBarcodeScanner.setSettings(mSettings);

            mBarcodeScanner.claim();

            Log.d(TAG, "Scanner READY in " + from);

        } catch (BarcodeException e) {
            Log.e(TAG, "setupScannerIfPossible failed in " + from + ". ErrorCode=" + e.getErrorCode(), e);
        } catch (Exception e) {
            Log.e(TAG, "setupScannerIfPossible failed in " + from + " (unexpected)", e);
        }
    }

    // ----------------------------
    // Data receive
    // ----------------------------

    @Override
    public void onBarcodeDataReceived(BarcodeDataReceivedEvent event) {
        List<BarcodeDataReceivedEvent.BarcodeData> list = event.getBarcodeData();
        if (list == null || list.isEmpty()) return;

        String data = list.get(0).getData();
        if (data == null) data = "";
        final String normalized = normalize(data);

        if (waitingDecode) {
            stopWaitForDecodeSuccess();
        }

        final String aim = String.valueOf(list.get(0).getSymbologyAim());
        final String denso = String.valueOf(list.get(0).getSymbologyDenso());

        Log.d(TAG, "onBarcodeDataReceived data=[" + normalized + "] symDenso=" + denso + " symAim=" + aim);

        // UI/業務通知はメインスレッドへ
        activity.runOnUiThread(() -> {
            // 空 or 重複はガード（必要なら外せます）
            if (TextUtils.isEmpty(normalized)) {
                return;
            }
            if (normalized.equals(lastScanned)) {
                return;
            }
            lastScanned = normalized;

            scanListener.onScan(normalized, aim, denso);
        });
    }

    // ----------------------------
    // WaitForDecode
    // ----------------------------

    private void startWaitForDecode(long timeoutMs) {
        if (mBarcodeScanner == null) {
            Log.e(TAG, "startWaitForDecode skipped: scanner is null");
            return;
        }
        if (waitingDecode) return;

        waitingDecode = true;

        timeoutRunnable = () -> {
            if (!waitingDecode) return;
            waitingDecode = false;
            timeoutRunnable = null;

            safePressTrigger(false);
            Log.d(TAG, "WAIT_FOR_DECODE timeout (" + timeoutMs + "ms)");
        };

        handler.postDelayed(timeoutRunnable, timeoutMs);
        safePressTrigger(true);
    }

    private void stopWaitForDecodeSuccess() {
        waitingDecode = false;

        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }

        safePressTrigger(false);
        Log.d(TAG, "WAIT_FOR_DECODE success");
    }

    private void cancelWaitForDecode(String reason) {
        waitingDecode = false;

        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }

        safePressTrigger(false);
        Log.d(TAG, "WAIT_FOR_DECODE cancelled: " + reason);
    }

    private void safePressTrigger(boolean on) {
        if (mBarcodeScanner == null) {
            Log.e(TAG, "pressSoftwareTrigger(" + on + ") skipped: scanner is null");
            return;
        }
        try {
            mBarcodeScanner.pressSoftwareTrigger(on);
        } catch (BarcodeException e) {
            Log.e(TAG, "pressSoftwareTrigger(" + on + ") failed. ErrorCode=" + e.getErrorCode(), e);
        } catch (Exception e) {
            Log.e(TAG, "pressSoftwareTrigger(" + on + ") failed (unexpected)", e);
        }
    }

    private void safeCloseScanner(String from) {
        if (mBarcodeScanner == null) return;
        try {
            mBarcodeScanner.close();
            Log.d(TAG, "Scanner CLOSE in " + from);
        } catch (BarcodeException e) {
            Log.e(TAG, "Scanner close failed in " + from + ". ErrorCode=" + e.getErrorCode(), e);
        } catch (Exception e) {
            Log.e(TAG, "Scanner close failed in " + from + " (unexpected)", e);
        }
    }

    // ----------------------------
    // Utils
    // ----------------------------

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", "").trim();
    }

    /**
     * AIM/Denso の両方を見て “C#時代に近い” 表示名へ寄せる。
     */
    @Nullable
    public String getBarcodeDisplayName(@Nullable String aim, @Nullable String denso) {
        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        // AIM優先
        if (a.startsWith("]E4")) return "JAN-8 (EAN-8)";
        if (a.startsWith("]E")) return "JAN-13 (EAN/UPC系)";
        if (a.startsWith("]A")) return "Code39";
        if (a.startsWith("]G")) return "Code93";
        if (a.startsWith("]C")) return "Code128";
        if (a.startsWith("]I")) return "ITF(2of5)";
        if (a.startsWith("]F")) return "Codabar(NW7)";

        // fallback
        if (a.contains("CODE39") || d.contains("CODE39")) return "Code39";
        if (a.contains("CODE93") || d.contains("CODE93")) return "Code93";
        if (a.contains("CODE128") || d.contains("CODE128")) return "Code128";
        if (a.contains("CODABAR") || d.contains("CODABAR") || a.contains("NW7") || d.contains("NW7"))
            return "Codabar(NW7)";

        if (a.contains("STANDARD2OF5") || d.contains("STANDARD2OF5") || a.contains("STF") || d.contains("STF"))
            return "Standard 2of5";
        if (a.contains("INTERLEAVED2OF5") || d.contains("INTERLEAVED2OF5")
                || a.contains("ITF") || d.contains("ITF")
                || a.contains("I2OF5") || d.contains("I2OF5"))
            return "ITF(2of5)";

        if (a.contains("EAN8") || d.contains("EAN8")) return "JAN-8 (EAN-8)";
        if (a.contains("EAN13") || d.contains("EAN13")
                || a.contains("UPC_A") || d.contains("UPC_A")
                || a.contains("UPC_E") || d.contains("UPC_E")
                || a.contains("EAN13UPCA") || d.contains("EAN13UPCA"))
            return "JAN-13 (EAN/UPC系)";

        if (a.contains("GS1") || d.contains("GS1")
                || a.contains("DATABAR") || d.contains("DATABAR")
                || a.contains("COMPOSITE") || d.contains("COMPOSITE"))
            return "GS1 DataBar";

        if (a.contains("MSI") || d.contains("MSI")) return "MSI";

        if (a.contains("MICROQR") || d.contains("MICROQR")) return "Micro QR";
        if (a.contains("QR") || d.contains("QR")) return "QR";
        if (a.contains("DATAMATRIX") || d.contains("DATAMATRIX")) return "DataMatrix";
        if (a.contains("PDF417") || d.contains("PDF417")) return "PDF417";
        if (a.contains("MICROPDF") || d.contains("MICROPDF")) return "MicroPDF";
        if (a.contains("AZTEC") || d.contains("AZTEC")) return "Aztec";
        if (a.contains("MAXICODE") || d.contains("MAXICODE")) return "MaxiCode";
        if (a.contains("SQRC") || d.contains("SQRC")) return "SQRC";
        if (a.contains("IQR") || d.contains("IQR")) return "iQR";
        if (a.contains("RMQR") || d.contains("RMQR")) return "rMQR";

        if (a.contains("OCR") || d.contains("OCR")) return "OCR";

        return null;
    }

    // =====================================================================================
    //  設定：C#に寄せて「許容シンボル」をON（存在する項目のみ）
    // =====================================================================================

    private void applyCSharpLikeSettingsReflective(Object settingsRoot) {
        if (settingsRoot == null) return;

        // 任意：音など（存在する場合のみ）
        setBoolean(settingsRoot, "notification.sound.enabled", true);
        setBoolean(settingsRoot, "output.intent.enabled", false);
        setBoolean(settingsRoot, "output.keyboard.enabled", false);

        final String[] enablePaths = new String[]{
                // 1D
                "ean8",
                "ean13UpcA",
                "upcE",
                "itf",
                "stf",
                "code39",
                "code93",
                "code128",
                "codabar",
                "gs1DataBar",
                "gs1DataBarLimited",
                "gs1DataBarExpanded",
                "msi",
                "gs1Composite",

                // 2D（表記揺れ吸収）
                "qrCode",
                "qr",
                "microQr",
                "microQR",
                "dataMatrix",
                "datamatrix",
                "pdf417",
                "microPdf417",
                "microPDF417",
                "maxiCode",
                "maxicode",
                "aztec",
                "sQrc",
                "sqrc",
                "iQr",
                "iqr",
                "rMqr",
                "rmqr",
        };

        final String[] decodeRoots = new String[]{
                "decode.symbologies"
        };

        for (String rootPath : decodeRoots) {
            for (String sym : enablePaths) {
                setBoolean(settingsRoot, rootPath + "." + sym + ".enabled", true);
            }
        }
    }

    // ===== Reflection helpers =====

    private void setBoolean(Object root, String path, boolean value) {
        try {
            FieldAndOwner fo = resolveOwnerAndField(root, path);
            if (fo == null) {
                Log.d(TAG, "SKIP (path not found): " + path);
                return;
            }

            Field f = fo.field;
            Object owner = fo.owner;

            if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                f.setAccessible(true);
                f.set(owner, value);
                Log.d(TAG, "SET boolean " + path + " = " + value);
            } else {
                Log.d(TAG, "SKIP (not boolean): " + path);
            }
        } catch (Exception e) {
            Log.d(TAG, "SKIP (exception): " + path + " : " + e.getClass().getSimpleName());
        }
    }

    @Nullable
    private FieldAndOwner resolveOwnerAndField(Object root, String path) {
        String[] parts = path.split("\\.");
        if (parts.length < 2) return null;

        Object cur = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String name = parts[i];
            Field f = findField(cur.getClass(), name);
            if (f == null) return null;

            try {
                f.setAccessible(true);
                Object next = f.get(cur);
                if (next == null) return null;
                cur = next;
            } catch (Exception e) {
                return null;
            }
        }

        String last = parts[parts.length - 1];
        Field lastField = findField(cur.getClass(), last);
        if (lastField == null) return null;

        return new FieldAndOwner(cur, lastField);
    }

    @Nullable
    private Field findField(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    private static final class FieldAndOwner {
        final Object owner;
        final Field field;

        FieldAndOwner(Object owner, Field field) {
            this.owner = owner;
            this.field = field;
        }
    }
}
