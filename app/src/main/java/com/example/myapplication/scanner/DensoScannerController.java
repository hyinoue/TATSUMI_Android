package com.example.myapplication.scanner;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

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
 * DENSO BHT SDK スキャナ制御（最小・統一版）
 * <p>
 * 方針：
 * - 光/マーカー制御はしない（端末既定）
 * - SCANキー制御もしない（端末既定）
 * - 「アプリに入ってくるかどうか」だけを decode 設定で制御する
 * <p>
 * 使い分け：
 * - 何でも読める画面（テスト画面など）: ALLOW_ALL_POLICY
 * - 特定EditTextフォーカス時だけCode39: createCode39OnFocusPolicy(etGenpinNo)
 */
public class DensoScannerController
        implements BarcodeManager.BarcodeManagerListener, BarcodeScanner.BarcodeDataListener {

    private static final String TAG = "DensoScannerMin";

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
         * （ここが NONE ならアプリに基本入ってこない）
         */
        @NonNull
        SymbologyProfile getSymbologyProfile();

        /**
         * 念のためのフィルタ（Code39以外を弾く等）
         */
        boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName);
    }

    /**
     * 何でもOK（テスト画面等）
     */
    public static final ScanPolicy ALLOW_ALL_POLICY = new ScanPolicy() {
        @Override
        public boolean canAcceptResult() {
            return true;
        }

        @NonNull
        @Override
        public SymbologyProfile getSymbologyProfile() {
            return SymbologyProfile.ALL;
        }

        @Override
        public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
            return true;
        }
    };

    /**
     * 全拒否（decodeもOFF）
     */
    public static final ScanPolicy DENY_ALL_POLICY = new ScanPolicy() {
        @Override
        public boolean canAcceptResult() {
            return false;
        }

        @NonNull
        @Override
        public SymbologyProfile getSymbologyProfile() {
            return SymbologyProfile.NONE;
        }

        @Override
        public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
            return false;
        }
    };

    /**
     * EditText がフォーカス中のときだけ Code39 をアプリ処理するポリシーを作る
     * - フォーカス外：decode=NONE（アプリに入れない）
     * - フォーカス中：decode=CODE39_ONLY（アプリに入れる）
     */
    @NonNull
    public static ScanPolicy createCode39OnFocusPolicy(@NonNull final android.view.View focusView) {
        return new ScanPolicy() {
            @Override
            public boolean canAcceptResult() {
                return focusView.hasFocus() && focusView.isEnabled();
            }

            @NonNull
            @Override
            public SymbologyProfile getSymbologyProfile() {
                return canAcceptResult() ? SymbologyProfile.CODE39_ONLY : SymbologyProfile.NONE;
            }

            @Override
            public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                if ("Code39".equals(displayName)) return true;
                String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
                String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);
                return a.startsWith("]A") || a.contains("CODE39") || d.contains("CODE39");
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
        try {
            if (scanner != null) {
                try {
                    scanner.removeDataListener(this);
                } catch (Exception ignored) {
                }
                scanner.close();
            }
        } catch (Exception ignored) {
        }
    }

    public void onDestroy() {
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
     * 例：etGenpinNo の onFocusChange で呼ぶ
     */
    public void refreshProfile(@NonNull String from) {
        applyProfileIfReady(from);
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

    private void applyProfileIfReady(@NonNull String from) {
        if (!resumed) return;
        if (scanner == null || settings == null) return;

        try {
            try {
                scanner.removeDataListener(this);
            } catch (Exception ignored) {
            }
            scanner.addDataListener(this);

            // ★ここが肝：フォーカス状態に応じて decode を NONE / CODE39_ONLY / ALL に切替
            applySymbology(settings, policy.getSymbologyProfile());

            scanner.setSettings(settings);
            scanner.claim();

            Log.d(TAG, "applyProfile " + policy.getSymbologyProfile() + " from=" + from);

        } catch (Exception e) {
            Log.e(TAG, "applyProfileIfReady failed from=" + from, e);
        }
    }

    @Override
    public void onBarcodeDataReceived(BarcodeDataReceivedEvent event) {
        List<BarcodeDataReceivedEvent.BarcodeData> list = event.getBarcodeData();
        if (list == null || list.isEmpty()) return;

        String data = list.get(0).getData();
        if (data == null) data = "";
        final String normalized = normalize(data);

        final String aim = safeToString(list.get(0).getSymbologyAim());
        final String denso = safeToString(list.get(0).getSymbologyDenso());
        final String displayName = getBarcodeDisplayName(aim, denso);

        // フォーカスOFF等なら無視
        if (!policy.canAcceptResult()) return;
        if (!policy.isSymbologyAllowed(aim, denso, displayName)) return;

        // 任意：重複ガード
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

    /**
     * ★public に変更：画面側で種別表示に使える
     */
    @Nullable
    public String getBarcodeDisplayName(@Nullable String aim, @Nullable String denso) {
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
