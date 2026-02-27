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


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　DensoScannerController ..... DENSO BHT SDK スキャナ制御（照射可否/受信/バーコード種別ー制御）
//　　　　　　:　createFocusCode39Policy ..... フォーカス中のみCode39許可の標準ポリシー生成
//　　　　　　:　onCreate ..... BarcodeManager生成
//　　　　　　:　onResume ..... 再開フラグ設定＋プロファイル適用
//　　　　　　:　onPause ..... 停止処理（照射停止/無効化/Listener解除/close）
//　　　　　　:　onDestroy ..... 破棄処理（照射停止/無効化/Scanner/Manager破棄）
//　　　　　　:　refreshProfile ..... フォーカス変化などでプロファイルを再適用
//　　　　　　:　handleDispatchKeyEvent ..... SCANキー押下を握り、照射可否を制御
//　　　　　　:　onBarcodeManagerCreated ..... Manager作成完了時にScanner取得＋初期設定
//　　　　　　:　applyProfileIfReady ..... ポリシーに従い照射可能/不可能を切替（claim/close等）
//　　　　　　:　disableScannerHard ..... 強制無効化（claim解除/close/Listener解除）
//　　　　　　:　onBarcodeDataReceived ..... バーコード受信（重複ガード/バーコード種別ー判定/通知）
//　　　　　　:　isCode39 ..... Code39判定
//　　　　　　:　getBarcodeDisplayName ..... AIM/DENSOコードから表示名を推定
//　　　　　　:　applySymbology ..... バーコード種別ーON/OFF適用（reflection）
//　　　　　　:　setBoolean ..... 設定オブジェクトへboolean設定（reflection）
//　　　　　　:　resolveOwnerAndField ..... パスからフィールド所有者とフィールド解決
//　　　　　　:　findField ..... フィールド検索（継承階層含む）
//============================================================

public class DensoScannerController
        implements BarcodeManager.BarcodeManagerListener, BarcodeScanner.BarcodeDataListener {

    private static final String TAG = "DensoScannerM70"; // ログタグ
    private static final long DUP_GUARD_MS = 300L; // 重複読取ガード時間(ms)

    // 端末によってSCANトリガーのキーコードが違うので列挙（必要に応じて追加）
    private static final int[] SCAN_TRIGGER_KEY_CODES = new int[]{501, 230, 233, 234}; // スキャン起動キーコード

    //==============================
    //　処理概要　:　読み取り可能なバーコード種別を表す列挙型
    //==============================
    public enum SymbologyProfile {
        NONE,
        CODE39_ONLY,
        ALL
    }

    //=================================
    //　処理概要　:　スキャナの読み取り制御ルールを定義するインターフェース
    //=================================
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

    //=========================================================
    //　機　能　:　フォーカス中のみCode39許可のポリシーを生成する
    //　引　数　:　target ..... EditText（フォーカス判定対象）
    //　戻り値　:　[ScanPolicy] ..... ポリシー
    //=========================================================
    @NonNull
    public static ScanPolicy createFocusCode39Policy(@Nullable EditText target) {
        return new ScanPolicy() {
            @Override
            public boolean canAcceptResult() {
                // フォーカス＋有効時のみ受信/照射許可
                return target != null && target.hasFocus() && target.isEnabled();
            }

            @NonNull
            @Override
            public SymbologyProfile getSymbologyProfile() {
                // 受信可能時だけCode39を有効化（それ以外は全無効）
                return canAcceptResult() ? SymbologyProfile.CODE39_ONLY : SymbologyProfile.NONE;
            }

            @Override
            public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                // 念のため Code39 以外は弾く
                return isCode39(aim, denso, displayName);
            }
        };
    }

    private final Activity activity;     // ホストActivity
    private final OnScanListener listener; // 読取結果コールバック
    private final ScanPolicy policy;     // 受入/バーコード種別ー制御ポリシー

    private BarcodeManager manager;          // DENSOバーコードマネージャ
    private BarcodeScanner scanner;          // DENSOスキャナ本体
    private BarcodeScannerSettings settings; // スキャナ設定

    // Activityのライフサイクル（onResume/onPause）連動用
    private boolean resumed = false; // onResume反映フラグ

    /**
     * 現在 claim しているか
     */
    private boolean claimed = false; // スキャナClaim済みフラグ

    /**
     * 最後に適用したプロファイル（無駄なclose/claimを減らす）
     */
    @Nullable
    private SymbologyProfile appliedProfile = null; // 現在適用中プロファイル

    // 重複ガード（短時間のみ）
    private String last = ""; // 直近読取データ
    private long lastAt = 0L; // 直近読取時刻(ms)

    //=========================================================
    //　機　能　:　スキャナ制御クラスを初期化する
    //　引　数　:　activity ..... Activity
    //　　　　　:　listener ..... OnScanListener（読み取り結果通知先）
    //　　　　　:　policy ..... ScanPolicy（照射/受信ポリシー）
    //　戻り値　:　[DensoScannerController] ..... なし
    //=========================================================
    public DensoScannerController(@NonNull Activity activity,
                                  @NonNull OnScanListener listener,
                                  @NonNull ScanPolicy policy) {
        this.activity = activity;
        this.listener = listener;
        this.policy = policy;
    }

    //============================
    //　機　能　:　生成時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public void onCreate() {
        // BarcodeManagerを生成（非同期で onBarcodeManagerCreated が呼ばれる）
        try {
            BarcodeManager.create(activity, this);
        } catch (Exception e) {
            Log.e(TAG, "BarcodeManager.create failed", e);
        }
    }

    //============================
    //　機　能　:　再開時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public void onResume() {
        // 再開フラグを立て、現在のフォーカス状態に応じてプロファイル適用
        resumed = true;
        applyProfileIfReady("onResume");
    }

    //============================
    //　機　能　:　一時停止時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public void onPause() {
        // 停止フラグを下げ、照射停止＋強制無効化
        resumed = false;
        stopTimedScan();
        disableScannerHard("onPause");

        // listener解除＋close（例外は握りつぶし）
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

    //============================
    //　機　能　:　破棄時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public void onDestroy() {
        // 照射停止＋無効化（安全側）
        stopTimedScan();
        disableScannerHard("onDestroy");

        // Scannerの破棄
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

        // Managerの破棄
        try {
            if (manager != null) {
                manager.destroy();
                manager = null;
            }
        } catch (Exception ignored) {
        }
    }

    //=================================================
    //　機　能　:　プロファイルを再適用する
    //　引　数　:　from ..... String（呼び出し元識別）
    //　戻り値　:　[void] ..... なし
    //=================================================
    public void refreshProfile(@NonNull String from) {
        // フォーカス変化などで即時反映したい場合に呼び出す
        applyProfileIfReady(from);
    }

    //=========================================================
    //　機　能　:　キーイベント処理を制御する
    //　引　数　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... true:イベントを握る、false:握らない
    //=========================================================
    public boolean handleDispatchKeyEvent(@NonNull KeyEvent event) {

        int keyCode = event.getKeyCode();

        // SCANキー以外は対象外
        if (!isScanTriggerKey(keyCode)) {
            return false;
        }

        int action = event.getAction();

        // フォーカス外：端末の照射を許さない（強制OFF）
        if (!policy.canAcceptResult()) {

            // 押下開始時：念のため無効プロファイル反映＋照射停止
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                applyProfileIfReady("SCAN_DOWN_noAccept");
                stopTimedScan();
            }

            // 離した時：プログラム側からも停止命令を投げる
            if (action == KeyEvent.ACTION_UP) {
                stopScanProgrammatically();
            }

            // ★イベントを握って端末側の押しっぱなし照射を防ぐ
            return true;
        }

        // フォーカス中：5秒照射（※実際の照射は端末側、ここは握って制御する方針）
        if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            return true; // ★握る
        }

        // 離した時は停止
        if (action == KeyEvent.ACTION_UP) {
            stopScanProgrammatically();
            return true;
        }

        // その他も握る
        return true;
    }

    //============================
    //　機　能　:　タイマー経由のスキャン停止
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void stopTimedScan() {
        // 現状は即時停止のみ（将来的にタイマー制御を入れても呼び出し口を変えない）
        stopScanProgrammatically();
    }

    //============================
    //　機　能　:　スキャナの読み取りを強制停止する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void stopScanProgrammatically() {
        if (scanner == null) return;

        // SDK/機種で停止メソッド名が異なるため、存在するものを総当たりで呼ぶ
        invokeNoArgAny(scanner,
                "softTriggerOff", "releaseTrigger", "triggerOff", "stopScan", "stopRead");
    }

    //=================================================
    //　機　能　:　引数なしメソッドを総当たりで呼び出す
    //　引　数　:　target ..... Object
    //　　　　　:　methodNames ..... String...（候補メソッド名）
    //　戻り値　:　[boolean] ..... true:どれか呼べた、false:全滅
    //=================================================
    private boolean invokeNoArgAny(@NonNull Object target, @NonNull String... methodNames) {
        Class<?> cls = target.getClass();

        // 先頭から順に試して成功したら終了
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

    //============================
    //　機　能　:　スキャントリガーキー判定
    //　引　数　:　keyCode ..... int
    //　戻り値　:　[boolean] ..... true:SCANキー、false:それ以外
    //============================
    private boolean isScanTriggerKey(int keyCode) {
        for (int code : SCAN_TRIGGER_KEY_CODES) {
            if (code == keyCode) return true;
        }
        return false;
    }

    //=================================================
    //　機　能　:　スキャナ初期化完了時にScannerを取得し設定を適用する
    //　引　数　:　barcodeManager ..... BarcodeManager
    //　戻り値　:　[void] ..... なし
    //=================================================
    @Override
    public void onBarcodeManagerCreated(BarcodeManager barcodeManager) {
        this.manager = barcodeManager;

        try {
            // 使用可能なScanner一覧を取得
            List<BarcodeScanner> list = manager.getBarcodeScanners();
            if (list == null || list.isEmpty()) return;

            // 先頭のScannerを使用
            scanner = list.get(0);
            settings = scanner.getSettings();

            // 現在のポリシーでプロファイル適用
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
    //=================================================
    //　機　能　:　プロファイルを適用する（準備完了時のみ）
    //　引　数　:　from ..... String（呼び出し元識別）
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void applyProfileIfReady(@NonNull String from) {

        // onResume前は適用しない（Activity停止中の操作を避ける）
        if (!resumed) return;

        // Scanner未準備なら何もしない
        if (scanner == null || settings == null) return;

        // 現在の許可状態と希望プロファイル
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

            // フォーカス中：既に同じプロファイルでclaim済なら何もしない
            if (want == appliedProfile && claimed) {
                return;
            }

            // 受信リスナーを付け直す（重複登録・取り漏れ対策）
            try {
                scanner.removeDataListener(this);
            } catch (Exception ignored) {
            }
            try {
                scanner.addDataListener(this);
            } catch (Exception ignored) {
            }

            // 機種依存で設定反映が効かない対策：close→setSettings→claim
            try {
                scanner.close();
            } catch (Exception ignored) {
            }

            // バーコード種別ーON/OFFを適用
            applySymbology(settings, want);

            // 設定をScannerへ反映
            try {
                scanner.setSettings(settings);
            } catch (Exception ignored) {
            }

            // claimして照射/受信可能にする
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
    //=================================================
    //　機　能　:　スキャナを強制無効化する
    //　引　数　:　from ..... String（呼び出し元識別）
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void disableScannerHard(@NonNull String from) {
        try {
            // 念のため照射停止
            stopTimedScan();

            if (scanner != null) {
                // claim解除系（SDK/機種で名前が違うことがあるので総当たり）
                invokeNoArgAny(scanner, "release", "unclaim", "releaseClaim");

                // 受信リスナー解除
                try {
                    scanner.removeDataListener(this);
                } catch (Exception ignored) {
                }

                // closeして照射できない状態へ寄せる
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

    //=================================================
    //　機　能　:　バーコード受信時処理
    //　引　数　:　event ..... BarcodeDataReceivedEvent
    //　戻り値　:　[void] ..... なし
    //=================================================
    @Override
    public void onBarcodeDataReceived(BarcodeDataReceivedEvent event) {

        // 受信データ一覧を取得
        List<BarcodeDataReceivedEvent.BarcodeData> list = event.getBarcodeData();
        if (list == null || list.isEmpty()) return;

        // 先頭のみ使用
        BarcodeDataReceivedEvent.BarcodeData first = list.get(0);

        // データ本体を取得し正規化
        String data = first.getData();
        if (data == null) data = "";
        final String normalized = normalize(data);

        // バーコード種別ー情報を取得
        final String aim = safeToString(first.getSymbologyAim());
        final String denso = safeToString(first.getSymbologyDenso());
        final String displayName = getBarcodeDisplayName(aim, denso);

        // フォーカスOFF等なら無視
        if (!policy.canAcceptResult()) return;

        // 念のため許可バーコード種別ー以外は弾く
        if (!policy.isSymbologyAllowed(aim, denso, displayName)) return;

        // 空データは無視
        if (TextUtils.isEmpty(normalized)) return;

        // 短時間重複ガード（同一データ連射対策）
        long now = SystemClock.elapsedRealtime();
        if (normalized.equals(last) && (now - lastAt) < DUP_GUARD_MS) return;
        last = normalized;
        lastAt = now;

        // UIスレッドで通知
        activity.runOnUiThread(() -> listener.onScan(normalized, aim, denso));
    }

    //============================
    //　機　能　:　受信データを正規化する
    //　引　数　:　s ..... String
    //　戻り値　:　[String] ..... 正規化後文字列
    //============================
    private String normalize(String s) {
        // 改行等を除去してトリム
        return s.replace("\r", "").replace("\n", "").trim();
    }

    //============================
    //　機　能　:　null安全に文字列化する
    //　引　数　:　v ..... Object
    //　戻り値　:　[String] ..... 文字列
    //============================
    @NonNull
    private String safeToString(@Nullable Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    // ============================
    // Code39 判定
    // ============================

    //=================================================
    //　機　能　:　Code39か判定する
    //　引　数　:　aim ..... String（AIM識別）
    //　　　　　:　denso ..... String（DENSO識別）
    //　　　　　:　displayName ..... String（推定表示名）
    //　戻り値　:　[boolean] ..... true:Code39、false:それ以外
    //=================================================
    public static boolean isCode39(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
        // 既に表示名がある場合はそれを優先
        if (displayName != null) {
            return "Code39".equals(displayName);
        }

        // 判定ロジックは共通Resolverを利用
        return "Code39".equals(resolveBarcodeDisplayName(aim, denso));
    }

    //=================================================
    //　機　能　:　バーコード種別の表示名を推定する（共通）
    //　引　数　:　aim ..... String
    //　　　　　:　denso ..... String
    //　戻り値　:　[String] ..... 表示名（不明ならnull）
    //=================================================
    @Nullable
    public static String resolveBarcodeDisplayName(@Nullable String aim, @Nullable String denso) {
        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        // AIM優先（AIM Code Identifier 先頭で判定）
        if (a.startsWith("]A")) return "Code39";
        if (a.startsWith("]G")) return "Code93";
        if (a.startsWith("]C")) return "Code128";
        if (a.startsWith("]F")) return "Codabar(NW7)";
        if (a.startsWith("]I")) return "ITF(2of5)";
        if (a.startsWith("]E")) return "EAN/UPC";

        // fallback（文字列含有で判定）
        if (a.contains("CODE39") || d.contains("CODE39")) return "Code39";
        if (a.contains("CODE93") || d.contains("CODE93")) return "Code93";
        if (a.contains("CODE128") || d.contains("CODE128")) return "Code128";
        if (a.contains("QR") || d.contains("QR")) return "QR";
        if (a.contains("DATAMATRIX") || d.contains("DATAMATRIX")) return "DataMatrix";
        if (a.contains("PDF") || d.contains("PDF")) return "PDF417";
        return null;
    }

    //=================================================
    //　機　能　:　バーコード種別の表示名を推定する
    //　引　数　:　aim ..... String
    //　　　　　:　denso ..... String
    //　戻り値　:　[String] ..... 表示名（不明ならnull）
    //=================================================
    @Nullable
    private String getBarcodeDisplayName(@Nullable String aim, @Nullable String denso) {
        return resolveBarcodeDisplayName(aim, denso);
    }

    //=================================================
    //　機　能　:　バーコード種別の有効/無効を適用する
    //　引　数　:　settingsRoot ..... Object（Settingsルート）
    //　　　　　:　profile ..... SymbologyProfile
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void applySymbology(Object settingsRoot, @NonNull SymbologyProfile profile) {

        // BarcodeScannerSettings の内部構造に依存するためパス指定で反映
        final String root = "decode.symbologies";

        // 代表的なバーコード種別ー一覧（機種/SDKで差があるため存在するものだけ設定される）
        final String[] all = new String[]{
                "code39",
                "ean8", "ean13UpcA", "upcE",
                "itf", "stf",
                "code93", "code128", "codabar",
                "qr", "qrCode", "dataMatrix", "pdf417"
        };

        // まず全OFF
        for (String s : all) {
            setBoolean(settingsRoot, root + "." + s + ".enabled", false);
        }

        // NONEの場合はここで終了
        if (profile == SymbologyProfile.NONE) return;

        // Code39のみON
        if (profile == SymbologyProfile.CODE39_ONLY) {
            setBoolean(settingsRoot, root + ".code39.enabled", true);
            return;
        }

        // ALLの場合は全ON
        for (String s : all) {
            setBoolean(settingsRoot, root + "." + s + ".enabled", true);
        }
    }

    //=================================================
    //　機　能　:　指定パスのbooleanフィールドへ値を設定する
    //　引　数　:　root ..... Object（探索開始オブジェクト）
    //　　　　　:　path ..... String（例：decode.symbologies.code39.enabled）
    //　　　　　:　value ..... boolean
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void setBoolean(Object root, String path, boolean value) {
        try {
            // パスから「所有者」と「最終フィールド」を解決
            FieldAndOwner fo = resolveOwnerAndField(root, path);
            if (fo == null) return;

            Field f = fo.field;
            Object owner = fo.owner;
            f.setAccessible(true);

            // boolean/Booleanのみ設定
            Class<?> t = f.getType();
            if (t == boolean.class || t == Boolean.class) {
                f.set(owner, value);
            }
        } catch (Exception ignored) {
        }
    }

    //=================================================
    //　機　能　:　パス文字列から所有者とフィールドを解決する
    //　引　数　:　root ..... Object
    //　　　　　:　path ..... String
    //　戻り値　:　[FieldAndOwner] ..... 解決結果（失敗はnull）
    //=================================================
    @Nullable
    private FieldAndOwner resolveOwnerAndField(Object root, String path) {
        String[] parts = path.split("\\.");
        if (parts.length < 2) return null;

        Object cur = root;

        // 最終フィールドの一つ手前まで辿る
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

        // 最終フィールドを取得
        Field last = findField(cur.getClass(), parts[parts.length - 1]);
        if (last == null) return null;

        return new FieldAndOwner(cur, last);
    }

    //=================================================
    //　機　能　:　フィールドを検索する（継承階層を辿る）
    //　引　数　:　cls ..... Class<?>
    //　　　　　:　name ..... String
    //　戻り値　:　[Field] ..... フィールド（見つからなければnull）
    //=================================================
    @Nullable
    private Field findField(Class<?> cls, String name) {
        Class<?> cur = cls;

        // declaredField を基点に親クラスへ遡って検索
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    //====================================
    //　処理概要　:　フィールドとその所属オブジェクトをまとめて保持
    //====================================
    private static final class FieldAndOwner {
        final Object owner;
        final Field field;

        //=====================================
        //　機　能　:　FieldAndOwnerの初期化処理
        //　引　数　:　owner ..... Object
        //　　　　　:　field ..... Field
        //　戻り値　:　[FieldAndOwner] ..... なし
        //=====================================
        FieldAndOwner(Object owner, Field field) {
            this.owner = owner;
            this.field = field;
        }
    }
}
