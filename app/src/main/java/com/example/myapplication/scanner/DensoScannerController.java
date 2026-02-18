package com.example.myapplication.scanner;

import android.app.Activity;
import android.os.SystemClock;
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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * DENSO BHT SDK スキャナ制御（BHT-M70向け強化版）
 * <p>
 * 要件：
 * - BundleSelect画面でのみ使用する
 * - etGenpinNo にフォーカスがある時だけスキャン（照射）できる
 * - etGenpinNo 以外フォーカス時はSCAN押しても照射しない
 * <p>
 * ポイント：
 * - フォーカス外は「Symbology OFF」だけでなく、claim解除＋closeで“照射させない”方向に寄せる
 */
public class DensoScannerController
        implements BarcodeManager.BarcodeManagerListener, BarcodeScanner.BarcodeDataListener {

    private static final String TAG = "DensoScannerM70";

    /**
     * 連続同一データの弾き（短時間だけ）
     */
    private static final long DUP_GUARD_MS = 300L;

    // 端末によってSCANトリガーのキーコードが違うので列挙（必要に応じて追加）
    private static final int[] SCAN_TRIGGER_KEY_CODES = new int[]{501, 230, 233, 234};

    public enum SymbologyProfile {
        NONE,
        CODE39_ONLY,
        ALL
    }

    public interface ScanPolicy {
        /**
         * 受信/照射を許可するか（例：EditTextフォーカス時のみ）
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

    /**
     * 現在 claim しているか
     */
    private boolean claimed = false;

    /**
     * 最後に適用したプロファイル（無駄なclose/claimを減らす）
     */
    @Nullable
    private SymbologyProfile appliedProfile = null;

    // 重複ガード（短時間のみ）
    private String last = "";
    private long lastAt = 0L;

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
        disableScannerHard("onPause");

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
        disableScannerHard("onDestroy");

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
     * Activity.dispatchKeyEvent から呼ぶ想定
     * <p>
     * 要件に合わせて：
     * - 現品Noフォーカス時：SCAN押下で5秒照射
     * - それ以外：SCAN押しても照射させない（アプリが握って停止）
     */
    public boolean handleDispatchKeyEvent(@NonNull KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!isScanTriggerKey(keyCode)) return false;

        int action = event.getAction();

        // フォーカス外：端末の照射を許さない（強制OFF）
        if (!policy.canAcceptResult()) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                // 念のため、スキャナを無効化しておく（照射抑止）
                applyProfileIfReady("SCAN_DOWN_noAccept");
                stopTimedScan();
            }
            if (action == KeyEvent.ACTION_UP) {
                stopScanProgrammatically();
            }
            return true; // ★ここで握る（端末側の押しっぱなし照射を防ぐ）
        }

        // フォーカス中：5秒照射
        if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            return true; // ★握る
        }

        if (action == KeyEvent.ACTION_UP) {
            stopScanProgrammatically();
            return true;
        }

        return true;
    }

    private boolean startScanWhilePressed() {
        if (scanner == null) return false;

        applyProfileIfReady("startScanWhilePressed");
        if (!policy.canAcceptResult()) return false;

        boolean started = invokeNoArgAny(scanner,
                "softTriggerOn", "pressTrigger", "triggerOn", "startScan", "startRead");
        if (!started) {
            Log.w(TAG, "startScanWhilePressed: trigger method not found/failed");
            return false;
        }
        return true;
    }

    private void stopTimedScan() {
        stopScanProgrammatically();
    }

    private void stopScanProgrammatically() {
        if (scanner == null) return;

        invokeNoArgAny(scanner,
                "softTriggerOff", "releaseTrigger", "triggerOff", "stopScan", "stopRead");
    }

    private boolean invokeNoArgAny(@NonNull Object target, @NonNull String... methodNames) {
        Class<?> cls = target.getClass();
        for (String methodName : methodNames) {
            try {
                Method method = cls.getMethod(methodName);
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
     * フォーカス状態に応じて、確実に「照射可能/不可能」を切り替える。
     * <p>
     * - フォーカス外：claim解除＋close（照射させない）
     * - フォーカス中：CODE39_ONLYを適用してclaim（照射可能）
     */
    private void applyProfileIfReady(@NonNull String from) {
        if (!resumed) return;
        if (scanner == null || settings == null) return;

        final boolean accept = policy.canAcceptResult();
        final SymbologyProfile want = policy.getSymbologyProfile();

        try {
            // フォーカス外：確実に無効化（照射させない）
            if (!accept || want == SymbologyProfile.NONE) {
                if (claimed || appliedProfile != SymbologyProfile.NONE) {
                    disableScannerHard("applyProfile(noAccept) from=" + from);
                    appliedProfile = SymbologyProfile.NONE;
                }
                return;
            }

            // フォーカス中：必要なときだけ適用
            if (want == appliedProfile && claimed) {
                return; // 既に適用済み
            }

            // listener付与
            try {
                scanner.removeDataListener(this);
            } catch (Exception ignored) {
            }
            try {
                scanner.addDataListener(this);
            } catch (Exception ignored) {
            }

            // 設定反映が機種依存で効かない対策：必要時だけ close→setSettings→claim
            try {
                scanner.close();
            } catch (Exception ignored) {
            }

            applySymbology(settings, want);
            try {
                scanner.setSettings(settings);
            } catch (Exception ignored) {
            }

            try {
                scanner.claim();
            } catch (Exception ignored) {
            }
            claimed = true;
            appliedProfile = want;

            Log.d(TAG, "applyProfile=" + want + " from=" + from);

        } catch (Exception e) {
            Log.e(TAG, "applyProfileIfReady failed from=" + from, e);
        }
    }

    /**
     * 解除を強めに行う（端末設定OFFを尊重して“照射させない”方向へ）
     */
    private void disableScannerHard(@NonNull String from) {
        try {
            stopTimedScan();

            // claim解除系（SDK/機種で名前が違うことがあるので総当たり）
            if (scanner != null) {
                invokeNoArgAny(scanner, "release", "unclaim", "releaseClaim");
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

        claimed = false;
        Log.d(TAG, "scanner disabled from=" + from);
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

        // フォーカスOFF等なら無視
        if (!policy.canAcceptResult()) return;

        // 念のため Code39 以外は弾く
        if (!policy.isSymbologyAllowed(aim, denso, displayName)) return;

        if (TextUtils.isEmpty(normalized)) return;

        // 短時間重複ガード
        long now = SystemClock.elapsedRealtime();
        if (normalized.equals(last) && (now - lastAt) < DUP_GUARD_MS) return;
        last = normalized;
        lastAt = now;

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
    // Code39 判定
    // ============================

    public static boolean isCode39(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
        if ("Code39".equals(displayName)) return true;

        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        // AIM：]A は Code39
        if (a.startsWith("]A")) return true;

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
