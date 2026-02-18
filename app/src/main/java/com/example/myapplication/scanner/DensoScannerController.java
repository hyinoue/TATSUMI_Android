package com.example.myapplication.scanner;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.densowave.bhtsdk.barcode.BarcodeDataReceivedEvent;
import com.densowave.bhtsdk.barcode.BarcodeManager;
import com.densowave.bhtsdk.barcode.BarcodeScanner;
import com.densowave.bhtsdk.barcode.BarcodeScannerSettings;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

/**
 * DENSO BHT SDK スキャナ制御（最小版）
 * - 画面が必要なときだけ生成して使う
 * - focus ONの間だけ Code39 デコードをON、OFF時はデコードOFF（アプリに入ってこない）
 * <p>
 * ※光/マーカー制御はしない（端末既定）
 * ※SCANキー制御もしない（端末既定）
 */
public class DensoScannerController
        implements BarcodeManager.BarcodeManagerListener, BarcodeScanner.BarcodeDataListener {

    private static final String TAG = "DensoScannerMin";
    private static final long SCAN_EMIT_DURATION_MS = 5000L;

    // 端末によってSCANトリガーのキーコードが違うので、必要分を列挙
    private static final int[] SCAN_TRIGGER_KEY_CODES = new int[]{501, 230, 233, 234};

    public enum SymbologyProfile {
        NONE,
        CODE39_ONLY,
        ALL
    }

    public interface ScanPolicy {
        /**
         * 受信したデータをアプリ処理して良いか（例：EditTextフォーカス時のみ）
         */
        boolean canAcceptResult();

        /**
         * デコードとして有効化したいプロファイル
         */
        @NonNull
        SymbologyProfile getSymbologyProfile();

        /**
         * 念のためのフィルタ（Code39以外を弾く等）
         */
        boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName);
    }

    /**
     * 現品No入力欄など、フォーカス中だけ Code39 を受け付ける標準ポリシー。
     */
    @NonNull
    public static ScanPolicy createFocusCode39Policy(@Nullable EditText target) {
        return new ScanPolicy() {
            @Override
            public boolean canAcceptResult() {
                return target != null && target.hasFocus() && target.isEnabled();
            }

            @NonNull
            @Override
            public SymbologyProfile getSymbologyProfile() {
                return canAcceptResult() ? SymbologyProfile.CODE39_ONLY : SymbologyProfile.NONE;
            }

            @Override
            public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                return isCode39(aim, denso, displayName);
            }
        };
    }

    private final Activity activity;
    private final OnScanListener listener;
    private final ScanPolicy policy;

    private BarcodeManager manager;
    private BarcodeScanner scanner;
    private BarcodeScannerSettings settings;

    private boolean resumed = false;
    private boolean timedScanRunning = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable timedStopRunnable = this::stopScanProgrammatically;


    // 任意：重複ガード
    private String last = "";

    public DensoScannerController(@NonNull Activity activity,
                                  @NonNull OnScanListener listener,
                                  @NonNull ScanPolicy policy) {
        this.activity = activity;
        this.listener = listener;
        this.policy = policy;
    }

    public void onCreate() {
        try {
            BarcodeManager.create(activity, this);
        } catch (Exception e) {
            Log.e(TAG, "BarcodeManager.create failed", e);
        }
    }

    public void onResume() {
        resumed = true;
        applyProfileIfReady("onResume");
    }

    public void onPause() {
        resumed = false;
        stopTimedScan();
        try {
            if (scanner != null) {
                try {
                    scanner.removeDataListener(this);
                } catch (Exception ignored) {
                }
                try {
                    scanner.close();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void onDestroy() {
        stopTimedScan();
        try {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Exception ignored) {
                }
                try {
                    scanner.destroy();
                } catch (Exception ignored) {
                }
                scanner = null;
            }
        } catch (Exception ignored) {
        }

        try {
            if (manager != null) {
                manager.destroy();
                manager = null;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * フォーカス変化などでプロファイルを即時反映したいときに呼ぶ
     */
    public void refreshProfile(@NonNull String from) {
        applyProfileIfReady(from);
    }

    /**
     * Activity.dispatchKeyEvent から呼ぶ想定（SCANトリガーキーをSDKに渡す）
     * <p>
     * ※ここではSCANキーを「握りつぶさない」方針に寄せるため、
     * 実際のキー処理は端末既定に任せる。
     * （ただし必要なら、将来ここでSDKに渡す処理を追加）
     */
    public boolean handleDispatchKeyEvent(@NonNull KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!isScanTriggerKey(keyCode)) return false;

        // 現品No入力欄が非フォーカス時はスキャンキーを握りつぶし、レーザー照射を抑止する
        if (!policy.canAcceptResult()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                stopTimedScan();
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            return startTimedScan();
        }

        // timed scanを使っている場合はキーUPも消費する
        if (event.getAction() == KeyEvent.ACTION_UP && timedScanRunning) {
            return true;
        }

        // 端末既定に委譲
        return false;
    }

    private boolean startTimedScan() {
        if (scanner == null) return false;

        // 設定反映とclaim状態を整える
        applyProfileIfReady("startTimedScan");

        boolean started = invokeNoArgAny(scanner,
                "startScan", "startRead", "softTriggerOn", "pressTrigger", "triggerOn");
        if (!started) return false;

        timedScanRunning = true;
        uiHandler.removeCallbacks(timedStopRunnable);
        uiHandler.postDelayed(timedStopRunnable, SCAN_EMIT_DURATION_MS);
        return true;
    }

    private void stopTimedScan() {
        uiHandler.removeCallbacks(timedStopRunnable);
        stopScanProgrammatically();
    }

    private void stopScanProgrammatically() {
        if (!timedScanRunning || scanner == null) {
            timedScanRunning = false;
            return;
        }

        invokeNoArgAny(scanner,
                "stopScan", "stopRead", "softTriggerOff", "releaseTrigger", "triggerOff");
        timedScanRunning = false;
    }

    private boolean invokeNoArgAny(@NonNull Object target, @NonNull String... methodNames) {
        Class<?> cls = target.getClass();
        for (String methodName : methodNames) {
            try {
                java.lang.reflect.Method method = cls.getMethod(methodName);
                method.setAccessible(true);
                method.invoke(target);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isScanTriggerKey(int keyCode) {
        for (int code : SCAN_TRIGGER_KEY_CODES) {
            if (code == keyCode) return true;
        }
        return false;
    }

    @Override
    public void onBarcodeManagerCreated(BarcodeManager barcodeManager) {
        this.manager = barcodeManager;
        try {
            List<BarcodeScanner> list = manager.getBarcodeScanners();
            if (list == null || list.isEmpty()) return;

            scanner = list.get(0);
            settings = scanner.getSettings();

            applyProfileIfReady("onBarcodeManagerCreated");
        } catch (Exception e) {
            Log.e(TAG, "onBarcodeManagerCreated failed", e);
        }
    }

    /**
     * ★重要：端末/SDKによって setSettings だけだとデコード設定が反映されないことがある。
     * close→setSettings→claim の順で、毎回確実に反映させる。
     */
    private void applyProfileIfReady(@NonNull String from) {
        if (!resumed) return;
        if (scanner == null || settings == null) return;

        try {
            // listener付け直し
            try {
                scanner.removeDataListener(this);
            } catch (Exception ignored) {
            }
            try {
                scanner.addDataListener(this);
            } catch (Exception ignored) {
            }

            // ★設定反映が効かない端末があるため、いったんcloseしてから適用
            try {
                scanner.close();
            } catch (Exception ignored) {
            }

            // プロファイル反映（Code39だけ/全部OFF/ALL）
            applySymbology(settings, policy.getSymbologyProfile());

            try {
                scanner.setSettings(settings);
            } catch (Exception ignored) {
            }

            // claim() で有効化
            try {
                scanner.claim();
            } catch (Exception ignored) {
            }

            Log.d(TAG, "applyProfile=" + policy.getSymbologyProfile() + " from=" + from);

        } catch (Exception e) {
            Log.e(TAG, "applyProfileIfReady failed from=" + from, e);
        }
    }

    @Override
    public void onBarcodeDataReceived(BarcodeDataReceivedEvent event) {
        List<BarcodeDataReceivedEvent.BarcodeData> list = event.getBarcodeData();
        if (list == null || list.isEmpty()) return;

        BarcodeDataReceivedEvent.BarcodeData first = list.get(0);

        String data = first.getData();
        if (data == null) data = "";
        final String normalized = normalize(data);

        final String aim = safeToString(first.getSymbologyAim());
        final String denso = safeToString(first.getSymbologyDenso());
        final String displayName = getBarcodeDisplayName(aim, denso);

        // フォーカスOFF等なら無視（アプリ処理しない）
        if (!policy.canAcceptResult()) return;

        // 念のため Code39 以外は弾く等
        if (!policy.isSymbologyAllowed(aim, denso, displayName)) return;

        // 重複ガード
        if (TextUtils.isEmpty(normalized)) return;
        if (normalized.equals(last)) return;
        last = normalized;

        activity.runOnUiThread(() -> listener.onScan(normalized, aim, denso));
    }

    private String normalize(String s) {
        return s.replace("\r", "").replace("\n", "").trim();
    }

    @NonNull
    private String safeToString(@Nullable Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    // ============================
    // ★追加：Code39 判定（外部から呼べる）
    // ============================

    /**
     * aim/denso/displayName から Code39 かどうか判定する（Activity / View から呼べる）
     */
    public static boolean isCode39(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
        if ("Code39".equals(displayName)) return true;

        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        // AIM優先：]A は Code39
        if (a.startsWith("]A")) return true;

        // fallback
        return a.contains("CODE39") || d.contains("CODE39");
    }

    @Nullable
    private String getBarcodeDisplayName(@Nullable String aim, @Nullable String denso) {
        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        // AIM優先
        if (a.startsWith("]A")) return "Code39";
        if (a.startsWith("]G")) return "Code93";
        if (a.startsWith("]C")) return "Code128";
        if (a.startsWith("]F")) return "Codabar(NW7)";
        if (a.startsWith("]I")) return "ITF(2of5)";
        if (a.startsWith("]E")) return "EAN/UPC";

        // fallback
        if (a.contains("CODE39") || d.contains("CODE39")) return "Code39";
        if (a.contains("CODE93") || d.contains("CODE93")) return "Code93";
        if (a.contains("CODE128") || d.contains("CODE128")) return "Code128";
        return null;
    }

    // --- Symbology (reflectionで存在するものだけ) ---
    private void applySymbology(Object settingsRoot, @NonNull SymbologyProfile profile) {
        final String root = "decode.symbologies";

        // 使う可能性があるものだけ（最小）
        final String[] all = new String[]{
                "code39",
                "ean8", "ean13UpcA", "upcE",
                "itf", "stf",
                "code93", "code128", "codabar",
                "qr", "qrCode", "dataMatrix", "pdf417"
        };

        // 全OFF
        for (String s : all) setBoolean(settingsRoot, root + "." + s + ".enabled", false);

        if (profile == SymbologyProfile.NONE) return;

        if (profile == SymbologyProfile.CODE39_ONLY) {
            setBoolean(settingsRoot, root + ".code39.enabled", true);
            return;
        }

        // ALL
        for (String s : all) setBoolean(settingsRoot, root + "." + s + ".enabled", true);
    }

    private void setBoolean(Object root, String path, boolean value) {
        try {
            FieldAndOwner fo = resolveOwnerAndField(root, path);
            if (fo == null) return;

            Field f = fo.field;
            Object owner = fo.owner;
            f.setAccessible(true);

            Class<?> t = f.getType();
            if (t == boolean.class || t == Boolean.class) {
                f.set(owner, value);
            }
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private FieldAndOwner resolveOwnerAndField(Object root, String path) {
        String[] parts = path.split("\\.");
        if (parts.length < 2) return null;

        Object cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Field f = findField(cur.getClass(), parts[i]);
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

        Field last = findField(cur.getClass(), parts[parts.length - 1]);
        if (last == null) return null;

        return new FieldAndOwner(cur, last);
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
