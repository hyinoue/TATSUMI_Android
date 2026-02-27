package com.example.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.YoteiEntity;
import com.example.myapplication.settings.HandyUtil;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

//==================================================================================
//　処理概要　:　メインメニュー画面Activity
//　　　　　　:　各機能画面への遷移、作業状況/受信状況の表示更新、
//　　　　　　:　束選定/コンテナ入力の値保持と同期、下部ボタン設定を行う。
//　　　　　　:　黄ボタンはデバッグ時は終了、本番時は端末再起動を行う。
//　関　　数　:　onCreate                      ..... 画面生成/初期化
//　　　　　　:　setupActivityLaunchers         ..... ActivityResultLauncher設定
//　　　　　　:　initViews                     ..... View取得
//　　　　　　:　setupContainerSizeSpinner      ..... コンテナサイズスピナー設定/保存
//　　　　　　:　setupBottomButtonTexts         ..... 下部ボタン表示設定
//　　　　　　:　wireActions                   ..... ボタン押下イベント設定
//　　　　　　:　onFunctionYellow              ..... (黄)終了/再起動
//　　　　　　:　onRestartMenu                 ..... 再起動確認/処理分岐
//　　　　　　:　requestDeviceReboot           ..... 端末再起動サービス呼び出し
//　　　　　　:　onResume                      ..... 画面再表示時(重量同期/表示更新)
//　　　　　　:　goServiceMenu                 ..... サービスメニューへ遷移
//　　　　　　:　openBundleSelect              ..... 積載束選定画面へ遷移
//　　　　　　:　syncContainerValuesFromBundle ..... 束→コンテナの値同期
//　　　　　　:　syncBundleValuesFromContainer ..... コンテナ→束の値同期
//　　　　　　:　syncContainerWeightsFromPrefs  ..... Prefs重量→保持Mapへ同期
//　　　　　　:　openContainerInputIfWorkExists ..... 作業有無チェック後にコンテナ入力へ遷移
//　　　　　　:　openCollateContainerSelect     ..... 照合コンテナ選択へ遷移
//　　　　　　:　onKeyDown                     ..... 物理キーでメニュー操作
//　　　　　　:　setCenterStatus               ..... 中央ステータス表示設定
//　　　　　　:　startDataSync                 ..... データ送受信開始(二重実行防止)
//　　　　　　:　runDataSync                   ..... データ送受信実処理
//　　　　　　:　showSyncErrorAndWait          ..... 同期エラー表示/OK待ち
//　　　　　　:　refreshInformation            ..... DB集計/画面表示更新
//　　　　　　:　intOrZero                     ..... null→0変換
//　　　　　　:　formatNumber                  ..... 数値表示整形
//　　　　　　:　formatRemaining               ..... 残数表示整形
//　　　　　　:　readStringMap                 ..... IntentからMap取得
//==================================================================================

public class MenuActivity extends BaseActivity {

    // ============================
    // 定数
    // ============================
    private static final String TAG = "MENU"; // ログタグ
    private static final String DENSO_POWER_MANAGER_PACKAGE = "com.densowave.powermanagerservice"; // 電源管理サービスパッケージ
    private static final String DENSO_POWER_MANAGER_SERVICE = "com.densowave.powermanagerservice.PowerManagerService"; // 電源管理サービス名
    private static final String DENSO_REBOOT_ACTION = "com.densowave.powermanagerservice.action.REBOOT"; // 再起動アクション
    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo"; // コンテナ重量キー
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";     // ダンネージ重量キー
    private static final String PREFS_CONTAINER_JYURYO = "prefs_container_jyuryo"; // コンテナ重量設定キー
    private static final String PREFS_DUNNAGE_JYURYO = "prefs_dunnage_jyuryo";     // ダンネージ重量設定キー

    // ============================
    // メンバ
    // ============================
    private ExecutorService io; // I/O処理スレッド
    private ActivityResultLauncher<Intent> bundleSelectLauncher; // 束選定画面ランチャー
    private ActivityResultLauncher<Intent> containerInputLauncher; // コンテナ入力画面ランチャー
    private final Map<String, String> bundleValues = new HashMap<>(); // 束入力値保持
    private final Map<String, String> containerValues = new HashMap<>(); // コンテナ入力値保持
    private final AtomicBoolean isDataSyncRunning = new AtomicBoolean(false); // 送受信処理中フラグ

    // ============================
    // Views
    // ============================
    private TextView tvCenterStatus; // 中央ステータス表示
    private Spinner spContainerSize; // コンテナサイズ選択
    private Button btnDataReceive; // 送受信ボタン
    private Button btnBundleSelect; // 束選定ボタン
    private Button btnContainerInput; // コンテナ入力ボタン
    private Button btnWeightCalc; // 重量計算ボタン
    private Button btnCollateContainerSelect; // 照合コンテナ選択ボタン
    private TextView lblDataReceiveTime; // 最終受信時刻ラベル
    private TextView lblDataReceive; // 未送信ありラベル
    private TextView lblContainerInput; // 作業中ありラベル
    private TextView lblContainerPlan; // 計画コンテナラベル
    private TextView lblBundlePlan;    // 計画束ラベル
    private TextView lblWeightPlan;    // 計画重量ラベル
    private TextView lblContainerFin; // 完了コンテナラベル
    private TextView lblBundleFin;    // 完了束ラベル
    private TextView lblWeightFin;    // 完了重量ラベル
    private TextView lblZanContainer; // 残コンテナラベル
    private TextView lblZanBundle;    // 残束ラベル
    private TextView lblZanWeight;    // 残重量ラベル

    //============================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... 画面再生成時の保存状態
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // 単一スレッドのExecutor（DB/通信系を順序通りに処理するため）
        io = Executors.newSingleThreadExecutor();

        // DBインスタンス（初回アクセスでRoom初期化が走る）
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());

        // DBが開けるかログで確認（端末環境差異や初期不具合調査用）
        io.execute(() -> {
            try {
                Log.d("DBCHK", "start");
                db.getOpenHelper().getReadableDatabase();
                Log.d("DBCHK", "opened");
            } catch (Exception e) {
                Log.e("DBCHK", "failed", e);
            }
        });

        // 画面遷移の戻り値受け取りを先に準備
        setupActivityLaunchers();

        // Viewを全取得
        initViews();

        // コンテナサイズの選択UIを初期化（prefs保存/復元）
        setupContainerSizeSpinner();

        // 下部のファンクションボタン文言を設定
        setupBottomButtonTexts();

        // 画面上の各ボタンに処理を紐づけ
        wireActions();

        // DB集計値などを読み込み、画面表示を更新
        refreshInformation();
    }

    //============================================================
    //　機　能　:　別画面入力データ同期＆画面更新
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupActivityLaunchers() {
        // 束選定画面の戻り値受け取り
        bundleSelectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // OKの場合のみ、結果Mapを受け取る
                    if (result.getResultCode() == RESULT_OK) {
                        Map<String, String> resultMap = readStringMap(
                                result.getData(),
                                BundleSelectActivity.EXTRA_BUNDLE_VALUES
                        );
                        if (resultMap != null) {
                            // 受け取った束情報を保持
                            bundleValues.clear();
                            bundleValues.putAll(resultMap);

                            // 束側に含まれる重量情報を、コンテナ側へ同期
                            syncContainerValuesFromBundle();
                        }
                    }

                    // 戻ってきたら必ず画面表示を更新
                    refreshInformation();
                }
        );

        // コンテナ入力画面の戻り値受け取り
        containerInputLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Map<String, String> resultMap = readStringMap(
                                result.getData(),
                                ContainerInputActivity.EXTRA_CONTAINER_VALUES
                        );
                        if (resultMap != null) {
                            // 受け取ったコンテナ情報を保持
                            containerValues.clear();
                            containerValues.putAll(resultMap);

                            // コンテナ側の重量を束側へ同期
                            syncBundleValuesFromContainer();
                        }
                    }

                    // 戻ってきたら必ず画面表示を更新
                    refreshInformation();
                }
        );
    }

    //============================================================
    //　機　能　:　viewsを初期化する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void initViews() {
        // ---- 入力UI ----
        spContainerSize = findViewById(R.id.spContainerSize);

        // ---- 画面ボタン ----
        btnDataReceive = findViewById(R.id.btnDataReceive);
        btnBundleSelect = findViewById(R.id.btnBundleSelect);
        btnContainerInput = findViewById(R.id.btnContainerInput);
        btnWeightCalc = findViewById(R.id.btnWeightCalc);
        btnCollateContainerSelect = findViewById(R.id.btnCollateContainerSelect);

        // ---- 表示ラベル ----
        lblDataReceiveTime = findViewById(R.id.lblDataReceiveTime);
        lblDataReceive = findViewById(R.id.lblDataReceive);
        lblContainerInput = findViewById(R.id.lblContainerInput);

        lblContainerPlan = findViewById(R.id.lblContainerPlan);
        lblContainerFin = findViewById(R.id.lblContainerFin);
        lblBundlePlan = findViewById(R.id.lblBundlePlan);
        lblBundleFin = findViewById(R.id.lblBundleFin);
        lblWeightPlan = findViewById(R.id.lblWeightPlan);
        lblWeightFin = findViewById(R.id.lblWeightFin);

        lblZanContainer = findViewById(R.id.lblZanContainer);
        lblZanBundle = findViewById(R.id.lblZanBundle);
        lblZanWeight = findViewById(R.id.lblZanWeight);
    }

    //============================================================
    //　機　能　:　コンテナサイズのスピナーを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupContainerSizeSpinner() {
        // string-array（R.array.container_sizes）からスピナーの選択肢を生成
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.container_sizes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spContainerSize.setAdapter(adapter);

        // 永続化（container_size）用Prefs
        final SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // 保存済みの選択肢を復元（無ければ20ft）
        String savedSize = prefs.getString("container_size", "20ft");
        int pos = adapter.getPosition(savedSize);
        if (pos >= 0) spContainerSize.setSelection(pos);

        // 選択変更時にPrefsへ保存
        spContainerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                prefs.edit().putString("container_size", selected).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 何もしない
            }
        });
    }

    //============================================================
    //　機　能　:　下部ボタンの表示文言を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        // この画面は黄ボタンのみ使用（他は空表示にして無効化）
        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");

        // Debugは「終了」、本番は「再起動」
        if (yellow != null) {
            yellow.setText(BuildConfig.DEBUG ? "終了" : "再起動");
        }

        // BaseActivity：空文字ボタンはdisable＋薄くする
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　各ボタンにクリック処理を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void wireActions() {
        // データ送受信（サーバ同期）
        if (btnDataReceive != null) {
            btnDataReceive.setOnClickListener(v -> startDataSync());
        }

        // 積載束選定（通常）
        if (btnBundleSelect != null) {
            btnBundleSelect.setOnClickListener(v -> openBundleSelect(BundleSelectActivity.MODE_NORMAL));
        }

        // コンテナ情報入力（作業データがある場合のみ）
        if (btnContainerInput != null) {
            btnContainerInput.setOnClickListener(v -> openContainerInputIfWorkExists());
        }

        // 重量計算（束選定の重量モード）
        if (btnWeightCalc != null) {
            btnWeightCalc.setOnClickListener(v -> openBundleSelect(BundleSelectActivity.MODE_JYURYO));
        }

        // 照合（コンテナ選択へ）
        if (btnCollateContainerSelect != null) {
            btnCollateContainerSelect.setOnClickListener(v -> openCollateContainerSelect());
        }
    }

    //============================================================
    //　機　能　:　黄ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionYellow() {
        onRestartMenu();
    }

    //============================================================
    //　機　能　:　再起動（終了）の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void onRestartMenu() {
        // Debugビルドは単に画面終了
        if (BuildConfig.DEBUG) {
            finish();
            return;
        }

        // 本番ビルドは端末再起動の確認を出す
        showQuestion("端末の再起動を実施します。\nよろしいですか？", yes -> {
            if (!yes) return;

            // DENSO専用サービスで再起動を要求
            if (!requestDeviceReboot()) {
                showErrorMsg("再起動に失敗しました。", MsgDispMode.Label);
            }
        });
    }

    //============================================================
    //　機　能　:　端末再起動サービスを呼び出す
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 起動要求成功ならtrue
    //============================================================
    private boolean requestDeviceReboot() {
        Intent intent = new Intent();
        intent.setClassName(DENSO_POWER_MANAGER_PACKAGE, DENSO_POWER_MANAGER_SERVICE);
        intent.setAction(DENSO_REBOOT_ACTION);

        try {
            // サービス起動により再起動を要求（端末環境によっては例外が出る）
            startService(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to request device reboot via DENSO power manager service", e);
            return false;
        }
    }

    //============================================================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onResume() {
        super.onResume();

        // Prefsに保存している重量を保持Mapに反映（戻り時に最新化）
        syncContainerWeightsFromPrefs();

        // DB集計して表示更新
        refreshInformation();
    }

    //============================================================
    //　機　能　:　サービスメニュー遷移の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void goServiceMenu() {
        startActivity(new Intent(this, ServiceMenuActivity.class));
    }

    //============================================================
    //　機　能　:　積載束選定画面を開く
    //　引　数　:　mode ..... 動作モード
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void openBundleSelect(String mode) {
        // 遷移先へ渡すデータをIntentに詰める（MapはSerializableとして渡す）
        Intent intent = new Intent(this, BundleSelectActivity.class);
        intent.putExtra(BundleSelectActivity.EXTRA_MODE, mode);
        intent.putExtra(BundleSelectActivity.EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
        intent.putExtra(ContainerInputActivity.EXTRA_CONTAINER_VALUES, new HashMap<>(containerValues));

        // launcherがあれば結果を受け取り、無ければ通常遷移
        if (bundleSelectLauncher != null) {
            bundleSelectLauncher.launch(intent);
        } else {
            startActivity(intent);
        }
    }

    //============================================================
    //　機　能　:　重量の値を同期する（束選定画面）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void syncContainerValuesFromBundle() {
        // 束選定で入力した重量（コンテナ重量/ダンネージ重量）をコンテナ入力側へ反映する
        if (bundleValues.containsKey(KEY_CONTAINER_JYURYO)) {
            containerValues.put(KEY_CONTAINER_JYURYO, bundleValues.get(KEY_CONTAINER_JYURYO));
        } else {
            // 束側に無い場合はコンテナ側も削除（古い値を残さない）
            containerValues.remove(KEY_CONTAINER_JYURYO);
        }

        if (bundleValues.containsKey(KEY_DUNNAGE_JYURYO)) {
            containerValues.put(KEY_DUNNAGE_JYURYO, bundleValues.get(KEY_DUNNAGE_JYURYO));
        } else {
            containerValues.remove(KEY_DUNNAGE_JYURYO);
        }
    }

    //============================================================
    //　機　能　:　重量の値を同期する（コンテナ入力画面）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void syncBundleValuesFromContainer() {
        // コンテナ入力で確定した重量を束側にも反映し、画面間の不整合を防ぐ
        String container = containerValues.get(KEY_CONTAINER_JYURYO);
        String dunnage = containerValues.get(KEY_DUNNAGE_JYURYO);

        if (container != null) {
            bundleValues.put(KEY_CONTAINER_JYURYO, container);
        }
        if (dunnage != null) {
            bundleValues.put(KEY_DUNNAGE_JYURYO, dunnage);
        }
    }

    //============================================================
    //　機　能　:　重量の値を同期する（アプリ専用保存ストレージ）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void syncContainerWeightsFromPrefs() {
        // Prefsから重量を読み、両Mapへ反映（画面復帰時に最新化するため）
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        String prefContainer = prefs.getString(PREFS_CONTAINER_JYURYO, null);
        String prefDunnage = prefs.getString(PREFS_DUNNAGE_JYURYO, null);

        if (!TextUtils.isEmpty(prefContainer)) {
            containerValues.put(KEY_CONTAINER_JYURYO, prefContainer);
            bundleValues.put(KEY_CONTAINER_JYURYO, prefContainer);
        }
        if (!TextUtils.isEmpty(prefDunnage)) {
            containerValues.put(KEY_DUNNAGE_JYURYO, prefDunnage);
            bundleValues.put(KEY_DUNNAGE_JYURYO, prefDunnage);
        }
    }

    //============================================================
    //　機　能　:　作業データが存在する場合のみコンテナ入力画面へ遷移する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void openContainerInputIfWorkExists() {
        // DB確認があるため別スレッドでチェック
        io.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                // 作業データ（Work）が無い場合、コンテナ入力はできない
                boolean hasWork = !db.syukkaMeisaiWorkDao().findAll().isEmpty();

                runOnUiThread(() -> {
                    if (!hasWork) {
                        // Work無しならユーザへ案内して中断
                        showErrorMsg("積載束選定が行われていません。先に積載束選定を実施してください。", MsgDispMode.Label);
                        return;
                    }

                    // 遷移前にPrefsの重量を最新化
                    syncContainerWeightsFromPrefs();

                    // コンテナ入力へ遷移（Mapを渡す）
                    Intent intent = new Intent(this, ContainerInputActivity.class);
                    intent.putExtra(ContainerInputActivity.EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
                    intent.putExtra(ContainerInputActivity.EXTRA_CONTAINER_VALUES, new HashMap<>(containerValues));

                    if (containerInputLauncher != null) {
                        containerInputLauncher.launch(intent);
                    } else {
                        startActivity(intent);
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, "openContainerInputIfWorkExists failed", ex);

                // 例外内容を簡易表示（運用で原因が追いやすいように）
                runOnUiThread(() -> showErrorMsg(
                        "コンテナ情報入力の起動に失敗しました。\n"
                                + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        MsgDispMode.MsgBox
                ));
            }
        });
    }

    //============================================================
    //　機　能　:　コンテナ情報入力画面を開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void openCollateContainerSelect() {
        startActivity(new Intent(this, CollateContainerSelectActivity.class));
    }

    //============================================================
    //　機　能　:　キー押下時の処理
    //　引　数　:　keyCode ..... キー値
    //　　　　　:　event ..... イベント情報
    //　戻り値　:　[boolean] ..... なし
    //============================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 長押し連打で同じ処理が走らないように抑止
        if (event.getRepeatCount() > 0) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                // 0キー：サービスメニューへ
                goServiceMenu();
                return true;

            case KeyEvent.KEYCODE_1:
                // 1キー：送受信
                startDataSync();
                return true;

            case KeyEvent.KEYCODE_2:
                // 2キー：束選定（通常）
                openBundleSelect(BundleSelectActivity.MODE_NORMAL);
                return true;

            case KeyEvent.KEYCODE_3:
                // 3キー：コンテナ入力（作業がある場合のみ）
                openContainerInputIfWorkExists();
                return true;

            case KeyEvent.KEYCODE_4:
                // 4キー：束選定（重量）
                openBundleSelect(BundleSelectActivity.MODE_JYURYO);
                return true;

            case KeyEvent.KEYCODE_5:
                // 5キー：照合コンテナ選択
                openCollateContainerSelect();
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    //============================================================
    //　機　能　:　中央ステータス表示の文字を設定する
    //　引　数　:　text ..... テキスト
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setCenterStatus(String text) {
        if (tvCenterStatus != null) tvCenterStatus.setText(text);
    }

    //============================================================
    //　機　能　:　データ送受信を開始する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void startDataSync() {
        // 送受信が走っている間は開始しない（連打/多重実行防止）
        if (!isDataSyncRunning.compareAndSet(false, true)) {
            return;
        }

        // 画面中央に状況表示
        setCenterStatus("データ送受信中...");

        // 実処理は別スレッドで実行
        io.execute(this::runDataSync);
    }

    //============================================================
    //　機　能　:　データ送受信の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void runDataSync() {
        // 長い処理のためローディング表示
        showLoadingLong();

        try {
            // DataSync内部で必要に応じてエラーを表示するため、コールバックを渡す
            DataSync sync = new DataSync(getApplicationContext(), this::showSyncErrorAndWait);

            // 同期実行
            boolean success = sync.runSync();

            // 結果をUIへ反映
            runOnUiThread(() -> {
                if (success) {
                    setCenterStatus("データ送受信完了");
                    showInfoMsg("データ送受信完了", MsgDispMode.Label);
                } else {
                    setCenterStatus("NG データ送受信に失敗しました");
                }
            });

            // 成功時はDB内容が変わるので再集計
            if (success) {
                refreshInformation();
            }

        } catch (Exception ex) {
            // 想定外例外：ログ＋ユーザへ表示
            Log.e(TAG, "DataSync failed", ex);
            String msg = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();

            runOnUiThread(() -> {
                setCenterStatus("NG " + msg);
                showErrorMsg(msg, MsgDispMode.MsgBox);
            });

        } finally {
            // 二重実行防止解除＋ローディング解除
            isDataSyncRunning.set(false);
            hideLoadingLong();
        }
    }

    //============================================================
    //　機　能　:　データ送受信のエラー表示
    //　引　数　:　message ..... メッセージ
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void showSyncErrorAndWait(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        // DataSync側のスレッドで呼ばれる前提のため、OK押下まで待機できるようLatchを使う
        CountDownLatch waitForOk = new CountDownLatch(1);

        runOnUiThread(() -> {
            // エラー通知（音/バイブ）
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this);

            // エラーダイアログ表示
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("エラー")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, which) -> waitForOk.countDown())
                    .create();

            // 背景の暗転を解除（現場で画面が見えにくくなるのを避ける）
            dialog.setOnShowListener(d -> {
                if (dialog.getWindow() != null) {
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                }
            });

            dialog.show();
        });

        // OKが押されるまで待つ（同期処理が勝手に進まないようにする）
        try {
            waitForOk.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted while waiting for error dialog confirmation", ex);
        }
    }

    //============================================================
    //　機　能　:　画面情報を更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void refreshInformation() {
        // DBアクセスがあるので別スレッドで処理
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // --- 最終受信日時を取得 ---
            SystemEntity system = db.systemDao().findById(1);
            String dataRecv = (system != null) ? system.dataRecvYmdhms : null;

            // --- 未送信データの有無を確認 ---
            // 出荷側の未送信があれば優先して表示
            boolean hasUnsentSyukka = !db.syukkaContainerDao().findUnsent().isEmpty();

            // 出荷側に未送信が無い場合のみ、確認（照合）側の未送信を確認
            boolean hasUnsentKakunin = false;
            if (!hasUnsentSyukka) {
                hasUnsentKakunin = !db.kakuninContainerDao().findUnsentCompleted().isEmpty();
            }

            // --- 作業中データの有無（Workテーブル） ---
            boolean hasWork = !db.syukkaMeisaiWorkDao().findAll().isEmpty();

            // --- 予定テーブルを集計（計画/完了/合計） ---
            List<YoteiEntity> yoteiRows = db.yoteiDao().findAll();

            long kanryoContainer = 0;
            long kanryoBundole = 0;
            long kanryoJyuryo = 0;

            long containerCount = 0;
            long goukeiBundole = 0;
            long goukeiJyuryo = 0;

            // 行単位で足し上げ
            for (YoteiEntity row : yoteiRows) {
                kanryoContainer += intOrZero(row.kanryoContainer);
                kanryoBundole += intOrZero(row.kanryoBundole);
                kanryoJyuryo += intOrZero(row.kanryoJyuryo);

                containerCount += intOrZero(row.containerCount);
                goukeiBundole += intOrZero(row.goukeiBundole);
                goukeiJyuryo += intOrZero(row.goukeiJyuryo);
            }

            // --- 重量はkgで保持されている想定のためt表示に変換 ---
            long kanryoJyuryoTon = kanryoJyuryo / 1000;
            long goukeiJyuryoTon = goukeiJyuryo / 1000;

            // --- 残数算出 ---
            long zanContainer = containerCount - kanryoContainer;
            long zanBundle = goukeiBundole - kanryoBundole;
            long zanWeight = goukeiJyuryoTon - kanryoJyuryoTon;

            // --- 表示用整形 ---
            final String recvText = (dataRecv == null || dataRecv.trim().isEmpty())
                    ? "----/--/-- --:--"
                    : dataRecv;

            final boolean showUnsent = hasUnsentSyukka || hasUnsentKakunin;
            final boolean hasWorkFinal = hasWork;

            final long kanryoContainerFinal = kanryoContainer;
            final long kanryoBundoleFinal = kanryoBundole;
            final long kanryoJyuryoTonFinal = kanryoJyuryoTon;

            final long containerCountFinal = containerCount;
            final long goukeiBundoleFinal = goukeiBundole;
            final long goukeiJyuryoTonFinal = goukeiJyuryoTon;

            final long zanContainerFinal = zanContainer;
            final long zanBundleFinal = zanBundle;
            final long zanWeightFinal = zanWeight;

            // --- UI反映はメインスレッドで実施 ---
            runOnUiThread(() -> {
                // 最終受信時刻
                if (lblDataReceiveTime != null) {
                    lblDataReceiveTime.setText("最終受信　" + recvText);
                }

                // 未送信があれば表示（無ければ非表示）
                if (lblDataReceive != null) {
                    lblDataReceive.setVisibility(showUnsent ? View.VISIBLE : View.INVISIBLE);
                }

                // 作業中があれば表示（無ければ非表示）
                if (lblContainerInput != null) {
                    lblContainerInput.setVisibility(hasWorkFinal ? View.VISIBLE : View.INVISIBLE);
                }

                // 完了
                if (lblContainerFin != null)
                    lblContainerFin.setText(formatNumber(kanryoContainerFinal));
                if (lblBundleFin != null) lblBundleFin.setText(formatNumber(kanryoBundoleFinal));
                if (lblWeightFin != null) lblWeightFin.setText(formatNumber(kanryoJyuryoTonFinal));

                // 計画
                if (lblContainerPlan != null)
                    lblContainerPlan.setText(formatNumber(containerCountFinal));
                if (lblBundlePlan != null) lblBundlePlan.setText(formatNumber(goukeiBundoleFinal));
                if (lblWeightPlan != null)
                    lblWeightPlan.setText(formatNumber(goukeiJyuryoTonFinal));

                // 残（0なら空表示）
                if (lblZanContainer != null)
                    lblZanContainer.setText(formatRemaining(zanContainerFinal));
                if (lblZanBundle != null) lblZanBundle.setText(formatRemaining(zanBundleFinal));
                if (lblZanWeight != null) lblZanWeight.setText(formatRemaining(zanWeightFinal));
            });
        });
    }

    //============================================================
    //　機　能　:　Null→0の処理
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[long] ..... なし
    //============================================================
    private long intOrZero(Integer value) {
        // DB列がNULLの場合を0として扱う
        return value == null ? 0 : value;
    }

    //============================================================
    //　機　能　:　数値文字列を整形する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... なし
    //============================================================
    private String formatNumber(long value) {
        // 3桁カンマ区切り
        return String.format(Locale.JAPAN, "%,d", value);
    }

    //============================================================
    //　機　能　:　残数表示用の整形
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... なし
    //============================================================
    private String formatRemaining(long value) {
        // 0は空（表示をスッキリさせる）
        return value == 0 ? "" : formatNumber(value);
    }

    //============================================================
    //　機　能　:　画面遷移で渡されたデータを安全に取り出して整形する処理
    //　引　数　:　data ..... データ
    //　　　　　:　key ..... キー値
    //　戻り値　:　[Map<String, String>] ..... Map
    //============================================================
    private Map<String, String> readStringMap(Intent data, String key) {
        // Intentが無い場合は取得不可
        if (data == null) {
            return null;
        }

        // EXTRAからSerializableとして取得
        java.io.Serializable extra = data.getSerializableExtra(key);

        // 型がMapでなければ不正
        if (!(extra instanceof Map)) {
            return null;
        }

        // raw型をString-Stringへ詰め直す（安全化）
        Map<?, ?> raw = (Map<?, ?>) extra;
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            Object rawKey = entry.getKey();
            Object rawValue = entry.getValue();

            // nullは除外（想定外データ対策）
            if (rawKey != null && rawValue != null) {
                result.put(rawKey.toString(), rawValue.toString());
            }
        }

        return result;
    }
}
