package com.example.myapplication.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.grid.BundleSelectController;
import com.example.myapplication.grid.BundleSelectRow;
import com.example.myapplication.scanner.DensoScannerController;
import com.example.myapplication.scanner.OnScanListener;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//============================================================
//　処理概要　:　積載束選択／重量計算 画面Activity
//　　　　　　:　束(現品番号)の追加/削除、重量の合計/残量計算を行う。
//　　　　　　:　スキャナは「etGenpinNoフォーカス中のみ Code39 を許可」し、
//　　　　　　:　スキャン結果は入力欄へ反映して通常の入力処理へ流す。
//　関　　数　:　onCreate .................. 画面生成/初期化（UI/モード/値読込/イベント/初期ロード）
//　　　　　　:　bindViews ................ 画面部品取得
//　　　　　　:　setupMode ................ 画面モード設定（通常/重量計算）
//　　　　　　:　loadBundleValues ......... 前画面からの束関連値読込
//　　　　　　:　loadContainerValues ...... 前画面からのコンテナ関連値読込
//　　　　　　:　initScanner .............. スキャナ初期化（フォーカス中だけCode39）
//　　　　　　:　setupInputHandlers ....... 入力監視/Enter移動/フォーカス時プロファイル更新
//　　　　　　:　isEnterAction ............ Enter判定（物理/IME）
//　　　　　　:　initControllerAndDefaults  Controller初期化 + 初期値反映
//　　　　　　:　resolveDefaultContainerWeight 既定コンテナ重量取得
//　　　　　　:　resolveDefaultDunnageWeight  既定ダンネージ重量取得
//　　　　　　:　resolveMaxContainerWeight     最大積載重量取得
//　　　　　　:　setupRecycler ............ RecyclerView初期化
//　　　　　　:　refreshRows .............. 一覧表示更新
//　　　　　　:　confirmDeleteRow ......... 行削除確認ダイアログ
//　　　　　　:　deleteBundleRow .......... 行削除実処理
//　　　　　　:　persistContainerWeights .. 重量入力をSharedPreferencesへ保存
//　　　　　　:　setupBottomButtonTexts ... 下部ボタン文言設定
//　　　　　　:　onFunctionRed ............ 束クリア（全削除）
//　　　　　　:　onFunctionBlue ........... 確定（入力検証→次画面へ）
//　　　　　　:　validateBeforeConfirm .... 確定前入力チェック
//　　　　　　:　openContainerInputAndFinish 次画面起動＆終了
//　　　　　　:　syncContainerValuesFromBundle bundleValues→containerValues同期
//　　　　　　:　onFunctionGreen .......... 未使用
//　　　　　　:　onFunctionYellow ......... 終了
//　　　　　　:　handleGenpinInput ........ 現品番号入力/スキャン処理（解析→チェック→追加）
//　　　　　　:　getRemainingWeight ....... 残量計算
//　　　　　　:　getTotalWeight ........... 合計計算
//　　　　　　:　getIntValue .............. EditText数値取得（カンマ除去）
//　　　　　　:　isEmptyOrZero ............ 未入力/0判定
//　　　　　　:　updateFooter ............. フッター表示更新（束数/合計/残）
//　　　　　　:　formatNumber ............. 数値フォーマット
//　　　　　　:　onResume ................. スキャナ初期化/再開/プロファイル反映
//　　　　　　:　onPause .................. スキャナ一時停止
//　　　　　　:　finish ................... 入力値保存して結果返却
//　　　　　　:　onDestroy ................ スキャナ破棄/スレッド停止
//　　　　　　:　dispatchKeyEvent ......... SCANキー等をスキャナへ委譲
//　　　　　　:　saveBundleInputValues .... 重量入力をbundleValuesへ保存
//　クラス　　:　BundleRowAdapter .......... 一覧表示Adapter（削除クリック対応）
//============================================================

public class BundleSelectActivity extends BaseActivity {

    public static final String EXTRA_MODE = "bundle_select_mode";                  // 画面モード受け渡しキー
    public static final String EXTRA_BUNDLE_VALUES = "bundle_select_values";        // 束入力値受け渡しキー
    public static final String MODE_NORMAL = "normal";                              // 通常モード
    public static final String MODE_JYURYO = "jyuryo_calc";                         // 重量計算モード

    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo";          // コンテナ重量キー
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";              // ダンネージ重量キー
    private static final String PREFS_CONTAINER_JYURYO = "prefs_container_jyuryo";  // コンテナ重量設定キー
    private static final String PREFS_DUNNAGE_JYURYO = "prefs_dunnage_jyuryo";      // ダンネージ重量設定キー

    private static final int SYSTEM_RENBAN = 1;                                       // システム連番

    private EditText etContainerKg;    // コンテナ重量
    private EditText etDunnageKg;      // ダンネージ重量
    private EditText etGenpinNo;       // 現品No
    private TextView tvBundleCount;    // 束本数
    private TextView tvTotalWeight;    // 総重量
    private TextView tvRemainWeight;   // 残重量
    private TextView tvTitle;          // タイトル
    private RecyclerView rvBundles;    // 束一覧

    private ExecutorService io;             // I/O処理スレッド
    private BundleSelectController controller; // 画面制御ロジック
    private BundleRowAdapter adapter;       // 束一覧アダプター

    // ★この画面専用：フォーカス中だけCode39
    private DensoScannerController scanner; // DENSOスキャナ制御
    private boolean scannerCreated = false; // スキャナ初期化済みフラグ

    private final Map<String, String> bundleValues = new HashMap<>();    // 束入力値保持
    private final Map<String, String> containerValues = new HashMap<>(); // コンテナ入力値保持

    private int maxContainerJyuryo = 0; // 最大積載可能重量
    private BundleSelectController.Mode mode = BundleSelectController.Mode.Normal; // 画面モード

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウト設定
        setContentView(R.layout.activity_bundle_select);

        // DBアクセス用の単一スレッド
        io = Executors.newSingleThreadExecutor();

        // 画面部品取得
        bindViews();

        // 画面モード判定（通常/重量計算）
        setupMode(getIntent());

        // 前画面から渡された値を復元
        loadBundleValues(getIntent());
        loadContainerValues(getIntent());

        // 下部ボタン文言設定（モードで確定表示が変わる）
        setupBottomButtonTexts();

        // 入力イベント設定（重量の即時再計算、Enter移動、フォーカス時スキャナプロファイル更新）
        setupInputHandlers();

        // 一覧（RecyclerView）設定
        setupRecycler();

        // DB/Controller 初期化 + 初期値ロード（重量初期値、最大積載、一覧復元など）
        initControllerAndDefaults();

        // 表で線を重ねて細く見せる（行の境界線を重ねる見せ方）
        RecyclerView rvBundles = findViewById(R.id.rvBundles);
        rvBundles.addItemDecoration(new RecyclerView.ItemDecoration() {
            //===========================================
            //　機　能　:　item Offsetsを取得する
            //　引　数　:　outRect ..... Rect
            //　　　　　:　view ..... View
            //　　　　　:　parent ..... RecyclerView
            //　　　　　:　state ..... RecyclerView.State
            //　戻り値　:　[void] ..... なし
            //===========================================
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position > 0) {
                    // 上余白をマイナスにして線を重ねる
                    outRect.top = -2;
                }
            }
        });
    }

    //============================
    //　機　能　:　bind Viewsの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void bindViews() {
        etContainerKg = findViewById(R.id.etContainerKg);
        etDunnageKg = findViewById(R.id.etDunnageKg);
        etGenpinNo = findViewById(R.id.etGenpinNo);
        tvBundleCount = findViewById(R.id.tvBundleCount);
        tvTotalWeight = findViewById(R.id.tvTotalWeight);
        tvRemainWeight = findViewById(R.id.tvRemainWeight);
        tvTitle = findViewById(R.id.tvTitle);
        rvBundles = findViewById(R.id.rvBundles);
    }

    //===============================
    //　機　能　:　modeを設定する
    //　引　数　:　intent ..... Intent
    //　戻り値　:　[void] ..... なし
    //===============================
    private void setupMode(@Nullable Intent intent) {
        // Extraから画面モードを判定
        String modeExtra = intent != null ? intent.getStringExtra(EXTRA_MODE) : null;

        if (MODE_JYURYO.equals(modeExtra)) {
            // 重量計算モード
            mode = BundleSelectController.Mode.JyuryoCalc;
            if (tvTitle != null) tvTitle.setText("重量計算");
        } else {
            // 通常（積載束選定）モード
            mode = BundleSelectController.Mode.Normal;
            if (tvTitle != null) tvTitle.setText("積載束選定");
        }
    }

    //================================
    //　機　能　:　bundle Valuesを読み込む
    //　引　数　:　intent ..... Intent
    //　戻り値　:　[void] ..... なし
    //================================
    private void loadBundleValues(@Nullable Intent intent) {
        if (intent == null) return;

        // Serializableで受け取ったMapをString Mapへ変換
        java.io.Serializable extra = intent.getSerializableExtra(EXTRA_BUNDLE_VALUES);
        if (!(extra instanceof Map)) return;

        bundleValues.clear();
        Map<?, ?> raw = (Map<?, ?>) extra;
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                bundleValues.put(key.toString(), value.toString());
            }
        }
    }

    //====================================
    //　機　能　:　container Valuesを読み込む
    //　引　数　:　intent ..... Intent
    //　戻り値　:　[void] ..... なし
    //====================================
    private void loadContainerValues(@Nullable Intent intent) {
        if (intent == null) return;

        // ContainerInputActivityからの戻り値を復元
        java.io.Serializable extra = intent.getSerializableExtra(ContainerInputActivity.EXTRA_CONTAINER_VALUES);
        if (!(extra instanceof Map)) return;

        containerValues.clear();
        Map<?, ?> raw = (Map<?, ?>) extra;
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                containerValues.put(key.toString(), value.toString());
            }
        }
    }

    //============================
    //　機　能　:　scannerを初期化する（フォーカス中だけCode39）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void initScanner() {
        if (scannerCreated) return;

        // スキャナ入力の受け口を作成
        scanner = new DensoScannerController(
                this,
                new OnScanListener() {
                    //=========================================
                    //　機　能　:　scan結果受信時の処理
                    //　引　数　:　normalizedData ..... String（正規化済み）
                    //　　　　　:　aim ..... String（AIM識別子）
                    //　　　　　:　denso ..... String（DENSO識別子）
                    //　戻り値　:　[void] ..... なし
                    //=========================================
                    @Override
                    public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {
                        // スキャン結果を入力欄に反映し、同じ処理経路（handleGenpinInput）に流す
                        runOnUiThread(() -> {
                            if (etGenpinNo != null) etGenpinNo.setText(normalizedData);
                            handleGenpinInput(normalizedData);
                        });
                    }
                },
                new DensoScannerController.ScanPolicy() {
                    //=========================================
                    //　機　能　:　scan結果を受け付けるか判定する
                    //　引　数　:　なし
                    //　戻り値　:　[boolean] ..... True:受け付ける
                    //=========================================
                    @Override
                    public boolean canAcceptResult() {
                        // etGenpinNoが有効かつフォーカス中のみ受け付ける
                        return etGenpinNo != null
                                && etGenpinNo.isEnabled()
                                && etGenpinNo.hasFocus()
                                && getCurrentFocus() == etGenpinNo;
                    }

                    //====================================================
                    //　機　能　:　symbology Profileを取得する
                    //　引　数　:　なし
                    //　戻り値　:　[SymbologyProfile] ..... 許可するシンボロジ
                    //====================================================
                    @NonNull
                    @Override
                    public DensoScannerController.SymbologyProfile getSymbologyProfile() {
                        // 受け付けるときだけCODE39、それ以外はNONE
                        return canAcceptResult()
                                ? DensoScannerController.SymbologyProfile.CODE39_ONLY
                                : DensoScannerController.SymbologyProfile.NONE;
                    }

                    //====================================================
                    //　機　能　:　対象シンボロジが許可か判定する
                    //　引　数　:　aim ..... String
                    //　　　　　:　denso ..... String
                    //　　　　　:　displayName ..... String
                    //　戻り値　:　[boolean] ..... True:許可
                    //====================================================
                    @Override
                    public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                        // Code39かどうかの判定を共通関数へ委譲
                        return DensoScannerController.isCode39(aim, denso, displayName);
                    }
                }
        );

        // スキャナライフサイクル開始
        scanner.onCreate();
        scannerCreated = true;
    }

    //================================
    //　機　能　:　input Handlersを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    private void setupInputHandlers() {
        // 重量入力が変わったら即時に再計算するため、監視を付ける
        if (etContainerKg != null) etContainerKg.addTextChangedListener(weightWatcher);
        if (etDunnageKg != null) etDunnageKg.addTextChangedListener(weightWatcher);

        if (etContainerKg != null) {
            // フォーカス変更時にスキャナプロファイル反映（主にデバッグ/状態同期用途）
            etContainerKg.setOnFocusChangeListener((v, hasFocus) -> {
                if (scanner != null) scanner.refreshProfile("ContainerFocus=" + hasFocus);
            });

            // 物理Enterキーで次へ
            etContainerKg.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && etDunnageKg != null) {
                    etDunnageKg.requestFocus();
                }
                return true;
            });

            // IMEのNext/Doneでも次へ
            etContainerKg.setOnEditorActionListener((v, actionId, event) -> {
                if (!isEnterAction(actionId, event)) return false;
                if (etDunnageKg != null) etDunnageKg.requestFocus();
                return true;
            });
        }

        if (etDunnageKg != null) {
            // フォーカス変更時にスキャナプロファイル反映
            etDunnageKg.setOnFocusChangeListener((v, hasFocus) -> {
                if (scanner != null) scanner.refreshProfile("DunnageFocus=" + hasFocus);
            });

            // Enterで次（現品番号へ）
            etDunnageKg.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && etGenpinNo != null) {
                    etGenpinNo.requestFocus();
                }
                return true;
            });

            // IMEでも次へ
            etDunnageKg.setOnEditorActionListener((v, actionId, event) -> {
                if (!isEnterAction(actionId, event)) return false;
                if (etGenpinNo != null) etGenpinNo.requestFocus();
                return true;
            });
        }

        if (etGenpinNo != null) {
            // キーボードは出さず、スキャナ入力を前提にする
            etGenpinNo.setShowSoftInputOnFocus(false);

            // ★フォーカスが変わったらプロファイルを即反映（NONE⇔CODE39_ONLY）
            etGenpinNo.setOnFocusChangeListener((v, hasFocus) -> {
                if (scanner != null) scanner.refreshProfile("GenpinFocus=" + hasFocus);
            });

            // 物理Enterで確定処理へ
            etGenpinNo.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    handleGenpinInput(etGenpinNo.getText() != null ? etGenpinNo.getText().toString() : "");
                }
                return true;
            });

            // IMEのNext/Done等でも確定処理へ
            etGenpinNo.setOnEditorActionListener((v, actionId, event) -> {
                if (!isEnterAction(actionId, event)) return false;
                handleGenpinInput(etGenpinNo.getText() != null ? etGenpinNo.getText().toString() : "");
                return true;
            });
        }
    }

    //=====================================
    //　機　能　:　Enter操作か判定する
    //　引　数　:　actionId ..... int
    //　　　　　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... True:Enter相当
    //=====================================
    private boolean isEnterAction(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }

    //=========================================
    //　機　能　:　controllerと初期値を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void initControllerAndDefaults() {
        showLoadingShort();

        // DBはUIスレッドでインスタンス取得（参照保持）
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());

        // DB処理はバックグラウンドスレッドで実行
        io.execute(() -> {
            try {
                // コントローラ生成（DAOとモードを渡す）
                controller = new BundleSelectController(
                        db.syukkaMeisaiDao(),
                        db.syukkaMeisaiWorkDao(),
                        mode
                );

                // システム設定取得
                SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);

                // 既定重量・最大積載を解決
                int defaultContainer = resolveDefaultContainerWeight(system);
                int defaultDunnage = resolveDefaultDunnageWeight(system);
                maxContainerJyuryo = resolveMaxContainerWeight(system);

                runOnUiThread(() -> {
                    // 前回入力値（SharedPreferences）を取得
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    String prefContainer = prefs.getString(PREFS_CONTAINER_JYURYO, "");
                    String prefDunnage = prefs.getString(PREFS_DUNNAGE_JYURYO, "");

                    // 画面復元値（bundleValues）優先で初期設定
                    boolean hasContainer = bundleValues.containsKey(KEY_CONTAINER_JYURYO);
                    boolean hasDunnage = bundleValues.containsKey(KEY_DUNNAGE_JYURYO);

                    if (hasContainer || hasDunnage) {
                        // bundleValuesに値がある場合はそちらを優先
                        if (etContainerKg != null) {
                            String savedContainer = bundleValues.get(KEY_CONTAINER_JYURYO);
                            etContainerKg.setText(
                                    TextUtils.isEmpty(savedContainer)
                                            ? String.valueOf(defaultContainer)
                                            : savedContainer
                            );
                        }
                        if (etDunnageKg != null) {
                            String savedDunnage = bundleValues.get(KEY_DUNNAGE_JYURYO);
                            etDunnageKg.setText(
                                    TextUtils.isEmpty(savedDunnage)
                                            ? String.valueOf(defaultDunnage)
                                            : savedDunnage
                            );
                        }
                    } else {
                        // 無ければSharedPreferences→無ければデフォルト
                        if (etContainerKg != null) {
                            etContainerKg.setText(
                                    TextUtils.isEmpty(prefContainer)
                                            ? String.valueOf(defaultContainer)
                                            : prefContainer
                            );
                        }
                        if (etDunnageKg != null) {
                            etDunnageKg.setText(
                                    TextUtils.isEmpty(prefDunnage)
                                            ? String.valueOf(defaultDunnage)
                                            : prefDunnage
                            );
                        }
                    }

                    // 一覧更新・フッター更新
                    refreshRows();
                    updateFooter();

                    // 入力フォーカスを現品番号へ
                    if (etGenpinNo != null) etGenpinNo.requestFocus();

                    hideLoadingShort();

                    // ★初期表示後もプロファイル反映（フォーカス状態に合わせる）
                    if (scanner != null) scanner.refreshProfile("initControllerAndDefaults");
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("BundleSelect initControllerAndDefaults", ex);
                });
            }
        });
    }

    //====================================================
    //　機　能　:　default Container Weightを取得する
    //　引　数　:　system ..... SystemEntity
    //　戻り値　:　[int] ..... 既定コンテナ重量
    //====================================================
    private int resolveDefaultContainerWeight(@Nullable SystemEntity system) {
        // ※必要に応じて system.defaultContainerJyuryo 等の実装に置き換え
        return 0;
    }

    //===================================================
    //　機　能　:　default Dunnage Weightを取得する
    //　引　数　:　system ..... SystemEntity
    //　戻り値　:　[int] ..... 既定ダンネージ重量
    //===================================================
    private int resolveDefaultDunnageWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultDunnageJyuryo != null) {
            return system.defaultDunnageJyuryo;
        }
        return 0;
    }

    //===================================================
    //　機　能　:　max Container Weightを取得する
    //　引　数　:　system ..... SystemEntity
    //　戻り値　:　[int] ..... 最大積載重量
    //===================================================
    private int resolveMaxContainerWeight(@Nullable SystemEntity system) {
        // System設定があれば優先
        if (system != null && system.maxContainerJyuryo != null && system.maxContainerJyuryo > 0) {
            return system.maxContainerJyuryo;
        }

        // 無ければコンテナサイズ設定から推定
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 30000 : 24000;
    }

    //============================
    //　機　能　:　recyclerを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void setupRecycler() {
        if (rvBundles == null) return;

        // 削除クリック時のコールバックを渡す
        adapter = new BundleRowAdapter(this::confirmDeleteRow);

        rvBundles.setLayoutManager(new LinearLayoutManager(this));
        rvBundles.setAdapter(adapter);
    }

    //============================
    //　機　能　:　rowsを再表示する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void refreshRows() {
        if (adapter == null || controller == null) return;
        adapter.submitList(controller.getDisplayRows());
    }

    //=================================
    //　機　能　:　行削除確認を表示する
    //　引　数　:　row ..... int
    //　戻り値　:　[void] ..... なし
    //=================================
    private void confirmDeleteRow(int row) {
        new AlertDialog.Builder(this)
                .setMessage("行を削除します。よろしいですか？")
                .setPositiveButton("いいえ", null)
                .setNegativeButton("はい", (d, w) -> deleteBundleRow(row))
                .show();
    }

    //===============================
    //　機　能　:　行削除を行う
    //　引　数　:　row ..... int
    //　戻り値　:　[void] ..... なし
    //===============================
    private void deleteBundleRow(int row) {
        if (controller == null) return;

        // DB/Workテーブル更新があるためバックグラウンドで実行
        io.execute(() -> {
            try {
                controller.removeBundle(row);

                runOnUiThread(() -> {
                    refreshRows();
                    updateFooter();
                    if (etGenpinNo != null) etGenpinNo.requestFocus();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> errorProcess("BundleSelect deleteBundleRow", ex));
            }
        });
    }

    //====================================================
    //　機　能　:　重量入力変更監視（再計算＋保存）
    //　引　数　:　なし
    //　戻り値　:　TextWatcher
    //====================================================
    private final TextWatcher weightWatcher = new TextWatcher() {
        //===================================
        //　機　能　:　before Text Changedの処理
        //　引　数　:　s ..... CharSequence
        //　　　　　:　start ..... int
        //　　　　　:　count ..... int
        //　　　　　:　after ..... int
        //　戻り値　:　[void] ..... なし
        //===================================
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        //=================================
        //　機　能　:　on Text Changedの処理
        //　引　数　:　s ..... CharSequence
        //　　　　　:　start ..... int
        //　　　　　:　before ..... int
        //　　　　　:　count ..... int
        //　戻り値　:　[void] ..... なし
        //=================================
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        //==================================
        //　機　能　:　after Text Changedの処理
        //　引　数　:　s ..... Editable
        //　戻り値　:　[void] ..... なし
        //==================================
        @Override
        public void afterTextChanged(Editable s) {
            // フッター表示（合計/残量）を更新
            updateFooter();

            // 入力値をPrefsへ保存（次回初期値に利用）
            persistContainerWeights();
        }
    };

    //=========================================
    //　機　能　:　重量入力をPrefsへ保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void persistContainerWeights() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // 入力値取得（null安全）
        String container = etContainerKg != null && etContainerKg.getText() != null
                ? etContainerKg.getText().toString().trim()
                : "";
        String dunnage = etDunnageKg != null && etDunnageKg.getText() != null
                ? etDunnageKg.getText().toString().trim()
                : "";

        // 永続化
        prefs.edit()
                .putString(PREFS_CONTAINER_JYURYO, container)
                .putString(PREFS_DUNNAGE_JYURYO, dunnage)
                .apply();
    }

    //=====================================
    //　機　能　:　bottom Button Textsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=====================================
    private void setupBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        // モードに応じて確定ボタンを表示/非表示
        if (mode == BundleSelectController.Mode.Normal) {
            if (blue != null) blue.setText("確定");
        } else {
            if (blue != null) blue.setText("");
        }

        if (red != null) red.setText("束クリア");
        if (green != null) green.setText("");
        if (yellow != null) yellow.setText("終了");

        refreshBottomButtonsEnabled();
    }

    //=================================
    //　機　能　:　on Function Redの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================
    @Override
    protected void onFunctionRed() {
        if (controller == null) return;

        // 全削除の確認ダイアログ
        showQuestion("一覧の内容を全て削除します。よろしいですか？", yes -> {
            if (!yes) return;

            io.execute(() -> {
                try {
                    // コントローラ側で削除（Workテーブル等も想定）
                    controller.deleteBundles();

                    runOnUiThread(() -> {
                        refreshRows();
                        updateFooter();
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> errorProcess("BundleSelect deleteBundles", ex));
                }
            });
        });
    }

    //================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    @Override
    protected void onFunctionBlue() {
        // 確定前チェック
        if (!validateBeforeConfirm()) {
            return;
        }

        // 次画面（コンテナ入力）へ遷移して終了
        openContainerInputAndFinish();
    }

    //=====================================
    //　機　能　:　確定前入力チェックを行う
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... True:OK
    //=====================================
    private boolean validateBeforeConfirm() {
        if (controller == null) return false;

        // 重量計算モードは確定操作を想定しない（チェックは通す）
        if (mode != BundleSelectController.Mode.Normal) {
            return true;
        }

        // コンテナ重量必須
        if (isEmptyOrZero(etContainerKg)) {
            showErrorMsg("コンテナ重量が未入力です", MsgDispMode.Label);
            if (etContainerKg != null) etContainerKg.requestFocus();
            return false;
        }

        // ダンネージ重量必須
        if (isEmptyOrZero(etDunnageKg)) {
            showErrorMsg("ダンネージ重量が未入力です", MsgDispMode.Label);
            if (etDunnageKg != null) etDunnageKg.requestFocus();
            return false;
        }

        // 束が未選択
        if (controller.getBundles().isEmpty()) {
            showErrorMsg("対象束が未選択です", MsgDispMode.Label);
            if (etGenpinNo != null) etGenpinNo.requestFocus();
            return false;
        }

        // 積載超過
        if (getRemainingWeight() < 0) {
            showErrorMsg("積載重量が超過しています", MsgDispMode.Label);
            if (etGenpinNo != null) etGenpinNo.requestFocus();
            return false;
        }

        return true;
    }

    //=========================================
    //　機　能　:　コンテナ入力画面へ遷移して終了する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void openContainerInputAndFinish() {
        // 入力値を保存（bundleValuesへ）
        saveBundleInputValues();

        // 次画面へ渡すcontainerValuesへ同期
        syncContainerValuesFromBundle();

        // 次画面起動
        Intent intent = new Intent(this, ContainerInputActivity.class);
        intent.putExtra(ContainerInputActivity.EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
        intent.putExtra(ContainerInputActivity.EXTRA_CONTAINER_VALUES, new HashMap<>(containerValues));
        startActivity(intent);

        // この画面は終了
        finish();
    }

    //========================================
    //　機　能　:　bundleValues→containerValuesを同期する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================================
    private void syncContainerValuesFromBundle() {
        // bundleValuesの重量をcontainerValuesへ反映（空なら削除）
        String container = bundleValues.get(KEY_CONTAINER_JYURYO);
        String dunnage = bundleValues.get(KEY_DUNNAGE_JYURYO);

        if (TextUtils.isEmpty(container)) {
            containerValues.remove(KEY_CONTAINER_JYURYO);
        } else {
            containerValues.put(KEY_CONTAINER_JYURYO, container);
        }

        if (TextUtils.isEmpty(dunnage)) {
            containerValues.remove(KEY_DUNNAGE_JYURYO);
        } else {
            containerValues.put(KEY_DUNNAGE_JYURYO, dunnage);
        }
    }

    //=================================
    //　機　能　:　on Function Greenの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================
    @Override
    protected void onFunctionGreen() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //=========================================
    //　機　能　:　現品番号入力を処理する
    //　引　数　:　rawInput ..... String
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void handleGenpinInput(String rawInput) {
        if (controller == null) return;

        // 入力トリム
        String input = rawInput != null ? rawInput.trim() : "";

        // 未入力なら入力順を戻す（コンテナ→現品番号）
        if (TextUtils.isEmpty(input)) {
            if (etGenpinNo != null) {
                if (etContainerKg != null) {
                    etContainerKg.requestFocus();
                } else {
                    etGenpinNo.requestFocus();
                }
            }
            return;
        }

        // バックグラウンドで束判定/追加を実施
        showLoadingShort();
        io.execute(() -> {
            String heatNo;
            String sokuban;
            String bundleNo = null;

            try {
                // 現品番号の桁数で切り出しルールを分岐
                if (input.length() == 13) {
                    // 13桁： [1..6]=heatNo, [7..12]=sokuban
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 13);
                } else if (input.length() == 14) {
                    // 14桁： [1..6]=heatNo, [7..13]=sokuban
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14);
                } else if (input.length() == 18) {
                    // 18桁： [1..6]=heatNo, [7..13]=sokuban, [14..17]=bundleNo
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14).trim();
                    bundleNo = input.substring(14, 18);

                    // bundleNo付きの場合は先に追加（仕様に合わせる）
                    controller.addBundleNo(heatNo, sokuban, bundleNo);
                } else {
                    // 想定外桁数
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg("現品番号は13桁か14桁か18桁で入力してください", MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                // 追加前チェック（重量超過や存在チェック等はcontroller側へ委譲）
                String errMsg = controller.checkBundle(
                        heatNo,
                        sokuban,
                        getIntValue(etContainerKg),
                        getIntValue(etDunnageKg),
                        maxContainerJyuryo
                );

                if (!TextUtils.isEmpty(errMsg)) {
                    // エラー表示して終了
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg(errMsg, MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                // 問題なければ束を追加
                controller.addBundle(heatNo, sokuban);

                runOnUiThread(() -> {
                    // 画面反映
                    refreshRows();
                    updateFooter();

                    // 入力欄をクリアして次入力へ
                    if (etGenpinNo != null) {
                        etGenpinNo.setText("");
                        etGenpinNo.requestFocus();
                    }
                    hideLoadingShort();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("BundleSelect handleGenpinInput", ex);
                });
            }
        });
    }

    //==================================
    //　機　能　:　remaining Weightを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 残量（最大-合計）
    //==================================
    private int getRemainingWeight() {
        int total = getTotalWeight();
        return maxContainerJyuryo - total;
    }

    //============================
    //　機　能　:　total Weightを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 合計重量
    //============================
    private int getTotalWeight() {
        // 束重量合計（controller側保持）
        int bundle = controller != null ? controller.getJyuryoSum() : 0;

        // 束数（仕様により個数も重量へ加算）
        int bundleCount = controller != null ? controller.getBundles().size() : 0;

        // コンテナ/ダンネージ重量
        int container = getIntValue(etContainerKg);
        int dunnage = getIntValue(etDunnageKg);

        // 合計（束重量 + 束数 + 自重）
        return bundle + bundleCount + container + dunnage;
    }

    //====================================
    //　機　能　:　EditTextからint値を取得する
    //　引　数　:　et ..... EditText
    //　戻り値　:　[int] ..... 数値（不正/未入力は0）
    //====================================
    private int getIntValue(EditText et) {
        if (et == null) return 0;

        // 入力文字列取得
        String s = et.getText() != null ? et.getText().toString() : "";
        if (TextUtils.isEmpty(s)) return 0;

        // カンマを除去して数値化
        String cleaned = s.replace(",", "").trim();
        if (TextUtils.isEmpty(cleaned)) return 0;

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    //===============================
    //　機　能　:　未入力/0を判定する
    //　引　数　:　et ..... EditText
    //　戻り値　:　[boolean] ..... True:未入力または0以下
    //===============================
    private boolean isEmptyOrZero(EditText et) {
        return getIntValue(et) <= 0;
    }

    //===============================
    //　機　能　:　footerを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===============================
    private void updateFooter() {
        // 束数/合計/残量を計算
        int count = controller != null ? controller.getBundles().size() : 0;
        int total = getTotalWeight();
        int remain = getRemainingWeight();

        // 画面へ反映
        if (tvBundleCount != null) tvBundleCount.setText(String.valueOf(count));
        if (tvTotalWeight != null) tvTotalWeight.setText(formatNumber(total));
        if (tvRemainWeight != null) tvRemainWeight.setText(formatNumber(remain));
    }

    //==============================
    //　機　能　:　numberを整形する
    //　引　数　:　value ..... int
    //　戻り値　:　[String] ..... 3桁区切り
    //==============================
    private String formatNumber(int value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();

        // UIが安定したタイミングでスキャナ初期化/再開
        getWindow().getDecorView().post(() -> {
            initScanner();
            if (scanner != null) scanner.onResume();

            // フォーカス状態に合わせてプロファイル更新
            if (scanner != null) scanner.refreshProfile("onResume");
        });
    }

    //============================
    //　機　能　:　画面非表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    //============================
    //　機　能　:　finishの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    public void finish() {
        // 入力値をbundleValuesへ保存して返却
        saveBundleInputValues();

        Intent result = new Intent();
        result.putExtra(EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
        setResult(RESULT_OK, result);

        super.finish();
    }

    //============================
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onDestroy() {
        // スキャナ破棄
        if (scanner != null) scanner.onDestroy();
        scannerCreated = false;

        // スレッド停止
        if (io != null) io.shutdownNow();

        super.onDestroy();
    }

    //======================================
    //　機　能　:　キーイベントをスキャナへ委譲する
    //　引　数　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... True:消費
    //======================================
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // SCANキー等をスキャナコントローラに渡す（必要ならここで消費）
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    //=========================================
    //　機　能　:　bundle入力値（重量）を保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void saveBundleInputValues() {
        // 入力値取得
        String container = etContainerKg != null && etContainerKg.getText() != null
                ? etContainerKg.getText().toString().trim()
                : "";
        String dunnage = etDunnageKg != null && etDunnageKg.getText() != null
                ? etDunnageKg.getText().toString().trim()
                : "";

        // 両方空なら画面値をクリア扱い（保持しない）
        if (TextUtils.isEmpty(container) && TextUtils.isEmpty(dunnage)) {
            bundleValues.clear();
            return;
        }

        // Mapへ保存（次画面/戻り値で使用）
        bundleValues.put(KEY_CONTAINER_JYURYO, container);
        bundleValues.put(KEY_DUNNAGE_JYURYO, dunnage);
    }

    //============================================================
    //　処理概要　:　束一覧表示用Adapter
    //　　　　　　:　行表示と削除ボタン（tvDelete）クリックを扱う
    //============================================================
    private static class BundleRowAdapter extends RecyclerView.Adapter<BundleRowAdapter.ViewHolder> {
        interface DeleteHandler {
            void delete(int row);
        }

        private final List<BundleSelectRow> rows = new ArrayList<>();
        private final DeleteHandler deleteHandler;

        //===============================
        //　機　能　:　Adapterの初期化処理
        //　引　数　:　deleteHandler ..... DeleteHandler
        //　戻り値　:　[BundleRowAdapter] ..... なし
        //===============================
        BundleRowAdapter(DeleteHandler deleteHandler) {
            this.deleteHandler = deleteHandler;
        }

        //====================================================
        //　機　能　:　submit Listの処理
        //　引　数　:　newRows ..... List<BundleSelectRow>
        //　戻り値　:　[void] ..... なし
        //====================================================
        void submitList(List<BundleSelectRow> newRows) {
            // 表示データを差し替えて再描画
            rows.clear();
            if (newRows != null) rows.addAll(newRows);
            notifyDataSetChanged();
        }

        //================================================
        //　機　能　:　on Create View Holderの処理
        //　引　数　:　parent ..... android.view.ViewGroup
        //　　　　　:　viewType ..... int
        //　戻り値　:　[ViewHolder] ..... なし
        //================================================
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            // 行レイアウトをinflate
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bundle_select_row, parent, false);
            return new ViewHolder(view);
        }

        //====================================
        //　機　能　:　on Bind View Holderの処理
        //　引　数　:　holder ..... ViewHolder
        //　　　　　:　position ..... int
        //　戻り値　:　[void] ..... なし
        //====================================
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // 行データを取得して各ラベルへ反映
            BundleSelectRow row = rows.get(position);
            holder.tvPNo.setText(row.pNo);
            holder.tvBNo.setText(row.bNo);
            holder.tvIndex.setText(row.index);
            holder.tvJyuryo.setText(row.jyuryo);

            // 重量は右寄せ表示
            holder.tvJyuryo.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            // 削除（キャンセル）表示
            holder.tvDelete.setText(row.cancelText);

            // 削除クリック → コールバックへ
            holder.tvDelete.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && deleteHandler != null) {
                    deleteHandler.delete(adapterPosition);
                }
            });
        }

        //============================
        //　機　能　:　item Countを取得する
        //　引　数　:　なし
        //　戻り値　:　[int] ..... 行数
        //============================
        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvPNo;
            final TextView tvBNo;
            final TextView tvIndex;
            final TextView tvJyuryo;
            final TextView tvDelete;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                tvPNo = itemView.findViewById(R.id.tvRowPNo);
                tvBNo = itemView.findViewById(R.id.tvRowBNo);
                tvIndex = itemView.findViewById(R.id.tvRowIndex);
                tvJyuryo = itemView.findViewById(R.id.tvRowJyuryo);
                tvDelete = itemView.findViewById(R.id.tvRowDelete);
            }
        }
    }
}
