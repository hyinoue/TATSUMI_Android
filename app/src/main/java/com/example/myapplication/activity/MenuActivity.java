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

/**
 * メインメニュー画面のActivity。
 *
 * <p>役割:</p>
 * <ul>
 *     <li>各画面への遷移と戻り時の表示更新</li>
 *     <li>束選定/コンテナ入力の値保持と同期</li>
 *     <li>受信状況・作業状況の表示</li>
 *     <li>下部ファンクションボタンの設定</li>
 * </ul>
 */
public class MenuActivity extends BaseActivity {

    // メインメニュー画面
    // NOTE: 積載束選定（Normal確定時）は BundleSelectActivity 側で
    //       ContainerInputActivity に直接遷移する（メニュー経由しない）

    private static final String TAG = "MENU";

    private static final String DENSO_POWER_MANAGER_PACKAGE = "com.densowave.powermanagerservice";
    private static final String DENSO_POWER_MANAGER_SERVICE = "com.densowave.powermanagerservice.PowerManagerService";
    private static final String DENSO_REBOOT_ACTION = "com.densowave.powermanagerservice.action.REBOOT";
    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo";
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";

    private static final String PREFS_CONTAINER_JYURYO = "prefs_container_jyuryo";
    private static final String PREFS_DUNNAGE_JYURYO = "prefs_dunnage_jyuryo";

    private ExecutorService io;

    private ActivityResultLauncher<Intent> bundleSelectLauncher;
    private ActivityResultLauncher<Intent> containerInputLauncher;

    // 積載束選定の値保持
    private final Map<String, String> bundleValues = new HashMap<>();
    // コンテナ情報入力の値保持
    private final Map<String, String> containerValues = new HashMap<>();

    // ===== Views =====
    private TextView tvCenterStatus;
    private Spinner spContainerSize;

    private Button btnDataReceive;
    private Button btnBundleSelect;
    private Button btnContainerInput;
    private Button btnWeightCalc;
    private Button btnCollateContainerSelect;
    private final AtomicBoolean isDataSyncRunning = new AtomicBoolean(false);

    private TextView lblDataReceiveTime;
    private TextView lblDataReceive;
    private TextView lblContainerInput;
    private TextView lblContainerPlan;
    private TextView lblContainerFin;
    private TextView lblBundlePlan;
    private TextView lblBundleFin;
    private TextView lblWeightPlan;
    private TextView lblWeightFin;
    private TextView lblZanContainer;
    private TextView lblZanBundle;
    private TextView lblZanWeight;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        io = Executors.newSingleThreadExecutor();

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());

        io.execute(() -> {
            try {
                Log.d("DBCHK", "start");
                db.getOpenHelper().getReadableDatabase();
                Log.d("DBCHK", "opened");
            } catch (Exception e) {
                Log.e("DBCHK", "failed", e);
            }
        });

        setupActivityLaunchers();
        initViews();
        setupContainerSizeSpinner();
        setupBottomButtonTexts();
        wireActions();

        refreshInformation();
    }

    //====================================
    //　機　能　:　activity Launchersを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //====================================
    private void setupActivityLaunchers() {
        bundleSelectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 遷移はしない。表示更新のみ。
                    if (result.getResultCode() == RESULT_OK) {
                        Map<String, String> resultMap = readStringMap(
                                result.getData(),
                                BundleSelectActivity.EXTRA_BUNDLE_VALUES
                        );
                        if (resultMap != null) {
                            // 受け取った束情報を保持し、表示用に同期
                            bundleValues.clear();
                            bundleValues.putAll(resultMap);
                            syncContainerValuesFromBundle();
                        }
                    }
                    // 表示の再計算/更新
                    refreshInformation();
                }
        );

        containerInputLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Map<String, String> resultMap = readStringMap(
                                result.getData(),
                                ContainerInputActivity.EXTRA_CONTAINER_VALUES
                        );
                        if (resultMap != null) {
                            // コンテナ入力で確定した値を保持し、束情報側へ同期
                            containerValues.clear();
                            containerValues.putAll(resultMap);
                            syncBundleValuesFromContainer();
                        }
                    }
                    // 画面ラベル/件数等を更新
                    refreshInformation();
                }
        );
    }

    //============================
    //　機　能　:　viewsを初期化する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void initViews() {
        spContainerSize = findViewById(R.id.spContainerSize);

        btnDataReceive = findViewById(R.id.btnDataReceive);
        btnBundleSelect = findViewById(R.id.btnBundleSelect);
        btnContainerInput = findViewById(R.id.btnContainerInput);
        btnWeightCalc = findViewById(R.id.btnWeightCalc);
        btnCollateContainerSelect = findViewById(R.id.btnCollateContainerSelect);

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

    //========================================
    //　機　能　:　container Size Spinnerを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================================
    private void setupContainerSizeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.container_sizes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spContainerSize.setAdapter(adapter);

        final SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // 保存済みを復元（デフォルト 20ft）
        String savedSize = prefs.getString("container_size", "20ft");
        int pos = adapter.getPosition(savedSize);
        if (pos >= 0) spContainerSize.setSelection(pos);

        // 変更されたら保存
        spContainerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                prefs.edit().putString("container_size", selected).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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

        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        if (yellow != null) {
            yellow.setText(BuildConfig.DEBUG ? "終了" : "再起動");
        }

        // 空文字は無効＋薄く（BaseActivity機能）
        refreshBottomButtonsEnabled();
    }

    //============================
    //　機　能　:　wire Actionsの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void wireActions() {

        // データ送受信
        btnDataReceive.setOnClickListener(v -> startDataSync());

        // 画面遷移（タップ）
        btnBundleSelect.setOnClickListener(v -> openBundleSelect(BundleSelectActivity.MODE_NORMAL));
        btnContainerInput.setOnClickListener(v -> openContainerInputIfWorkExists());
        btnWeightCalc.setOnClickListener(v -> openBundleSelect(BundleSelectActivity.MODE_JYURYO));
        btnCollateContainerSelect.setOnClickListener(v -> openCollateContainerSelect());
    }

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        onRestartMenu();
    }

    //===============================
    //　機　能　:　on Restart Menuの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===============================
    private void onRestartMenu() {
        if (BuildConfig.DEBUG) {
            finish();
            return;
        }
        showQuestion("端末の再起動を実施します。\nよろしいですか？", yes -> {
            if (!yes) return;
            if (!requestDeviceReboot()) {
                showErrorMsg("再起動に失敗しました。", MsgDispMode.Label);
            }
        });
    }

    //==========================================
    //　機　能　:　端末再起動サービスを呼び出す
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 起動要求成功ならtrue
    //==========================================
    private boolean requestDeviceReboot() {
        Intent intent = new Intent();
        intent.setClassName(DENSO_POWER_MANAGER_PACKAGE, DENSO_POWER_MANAGER_SERVICE);
        intent.setAction(DENSO_REBOOT_ACTION);
        try {
            startService(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to request device reboot via DENSO power manager service", e);
            return false;
        }
    }

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();
        syncContainerWeightsFromPrefs();
        refreshInformation();
    }

    //===============================
    //　機　能　:　go Service Menuの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===============================
    private void goServiceMenu() {
        startActivity(new Intent(this, ServiceMenuActivity.class));
    }

    //==============================
    //　機　能　:　bundle Selectを開く
    //　引　数　:　mode ..... String
    //　戻り値　:　[void] ..... なし
    //==============================
    private void openBundleSelect(String mode) {
        Intent intent = new Intent(this, BundleSelectActivity.class);
        intent.putExtra(BundleSelectActivity.EXTRA_MODE, mode);
        intent.putExtra(BundleSelectActivity.EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
        intent.putExtra(ContainerInputActivity.EXTRA_CONTAINER_VALUES, new HashMap<>(containerValues));

        if (bundleSelectLauncher != null) {
            bundleSelectLauncher.launch(intent);
        } else {
            startActivity(intent);
        }
    }

    //==============================================
    //　機　能　:　container Values From Bundleを同期する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================================
    private void syncContainerValuesFromBundle() {
        if (bundleValues.containsKey(KEY_CONTAINER_JYURYO)) {
            containerValues.put(KEY_CONTAINER_JYURYO, bundleValues.get(KEY_CONTAINER_JYURYO));
        } else {
            containerValues.remove(KEY_CONTAINER_JYURYO);
        }
        if (bundleValues.containsKey(KEY_DUNNAGE_JYURYO)) {
            containerValues.put(KEY_DUNNAGE_JYURYO, bundleValues.get(KEY_DUNNAGE_JYURYO));
        } else {
            containerValues.remove(KEY_DUNNAGE_JYURYO);
        }
    }

    //==============================================
    //　機　能　:　bundle Values From Containerを同期する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================================
    private void syncBundleValuesFromContainer() {
        String container = containerValues.get(KEY_CONTAINER_JYURYO);
        String dunnage = containerValues.get(KEY_DUNNAGE_JYURYO);
        if (container != null) {
            bundleValues.put(KEY_CONTAINER_JYURYO, container);
        }
        if (dunnage != null) {
            bundleValues.put(KEY_DUNNAGE_JYURYO, dunnage);
        }
    }

    //==============================================
    //　機　能　:　container Weights From Prefsを同期する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================================
    private void syncContainerWeightsFromPrefs() {
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

    //==============================================
    //　機　能　:　container Input If Work Existsを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================================
    private void openContainerInputIfWorkExists() {
        io.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                boolean hasWork = !db.syukkaMeisaiWorkDao().findAll().isEmpty();
                runOnUiThread(() -> {
                    if (!hasWork) {
                        showErrorMsg("積載束選定が行われていません。先に積載束選定を実施してください。", MsgDispMode.Label);
                        return;
                    }
                    syncContainerWeightsFromPrefs();
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
                runOnUiThread(() -> showErrorMsg(
                        "コンテナ情報入力の起動に失敗しました。\n"
                                + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        MsgDispMode.MsgBox
                ));
            }
        });
    }

    //========================================
    //　機　能　:　collate Container Selectを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================================
    private void openCollateContainerSelect() {
        startActivity(new Intent(this, CollateContainerSelectActivity.class));
    }

    //=================================
    //　機　能　:　キー押下時の処理
    //　引　数　:　keyCode ..... int
    //　　　　　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... なし
    //=================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // ★長押し連打防止
        if (event.getRepeatCount() > 0) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                goServiceMenu();
                return true;

            case KeyEvent.KEYCODE_1:
                startDataSync();
                return true;

            case KeyEvent.KEYCODE_2:
                openBundleSelect(BundleSelectActivity.MODE_NORMAL);
                return true;

            case KeyEvent.KEYCODE_3:
                openContainerInputIfWorkExists();
                return true;

            case KeyEvent.KEYCODE_4:
                openBundleSelect(BundleSelectActivity.MODE_JYURYO);
                return true;

            case KeyEvent.KEYCODE_5:
                openCollateContainerSelect();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //===============================
    //　機　能　:　center Statusを設定する
    //　引　数　:　text ..... String
    //　戻り値　:　[void] ..... なし
    //===============================
    private void setCenterStatus(String text) {
        if (tvCenterStatus != null) tvCenterStatus.setText(text);
    }

    //============================
    //　機　能　:　data Syncを開始する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void startDataSync() {
        if (!isDataSyncRunning.compareAndSet(false, true)) {
            return;
        }
        setCenterStatus("データ送受信中...");
        io.execute(this::runDataSync);
    }

    //=============================
    //　機　能　:　run Data Syncの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================
    private void runDataSync() {
        showLoadingLong();
        try {
            DataSync sync = new DataSync(getApplicationContext(), this::showSyncErrorAndWait);
            boolean success = sync.runSync();
            runOnUiThread(() -> {
                if (success) {
                    setCenterStatus("データ送受信完了");
                    showInfoMsg("データ送受信完了", MsgDispMode.Label);
                } else {
                    setCenterStatus("NG データ送受信に失敗しました");
                }
            });
            if (success) {
                refreshInformation();
            }
        } catch (Exception ex) {
            Log.e(TAG, "DataSync failed", ex);
            String msg = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();
            runOnUiThread(() -> {
                setCenterStatus("NG " + msg);
                showErrorMsg(msg, MsgDispMode.MsgBox);
            });
        } finally {
            isDataSyncRunning.set(false);
            hideLoadingLong();
        }
    }

    //=============================
    //　機　能　:　data Syncのエラー表示
    //　引　数　:　message ..... String
    //　戻り値　:　[void] ..... なし
    //=============================
    private void showSyncErrorAndWait(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        CountDownLatch waitForOk = new CountDownLatch(1);
        runOnUiThread(() -> {
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("エラー")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, which) -> waitForOk.countDown())
                    .create();
            dialog.setOnShowListener(d -> {
                if (dialog.getWindow() != null) {
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                }
            });
            dialog.show();
        });

        try {
            waitForOk.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted while waiting for error dialog confirmation", ex);
        }
    }

    //=============================
    //　機　能　:　informationを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================
    private void refreshInformation() {
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SystemEntity system = db.systemDao().findById(1);
            String dataRecv = (system != null) ? system.dataRecvYmdhms : null;

            boolean hasUnsentSyukka = !db.syukkaContainerDao().findUnsent().isEmpty();
            boolean hasUnsentKakunin = false;
            if (!hasUnsentSyukka) {
                hasUnsentKakunin = !db.kakuninContainerDao().findUnsentCompleted().isEmpty();
            }

            boolean hasWork = !db.syukkaMeisaiWorkDao().findAll().isEmpty();
            List<YoteiEntity> yoteiRows = db.yoteiDao().findAll();

            long kanryoContainer = 0;
            long kanryoBundole = 0;
            long kanryoJyuryo = 0;
            long containerCount = 0;
            long goukeiBundole = 0;
            long goukeiJyuryo = 0;

            for (YoteiEntity row : yoteiRows) {
                kanryoContainer += intOrZero(row.kanryoContainer);
                kanryoBundole += intOrZero(row.kanryoBundole);
                kanryoJyuryo += intOrZero(row.kanryoJyuryo);
                containerCount += intOrZero(row.containerCount);
                goukeiBundole += intOrZero(row.goukeiBundole);
                goukeiJyuryo += intOrZero(row.goukeiJyuryo);
            }

            long kanryoJyuryoTon = kanryoJyuryo / 1000;
            long goukeiJyuryoTon = goukeiJyuryo / 1000;

            long zanContainer = containerCount - kanryoContainer;
            long zanBundle = goukeiBundole - kanryoBundole;
            long zanWeight = goukeiJyuryoTon - kanryoJyuryoTon;

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

            runOnUiThread(() -> {
                if (lblDataReceiveTime != null) {
                    lblDataReceiveTime.setText("最終受信　" + recvText);
                }
                if (lblDataReceive != null) {
                    lblDataReceive.setVisibility(showUnsent ? View.VISIBLE : View.INVISIBLE);
                }
                if (lblContainerInput != null) {
                    lblContainerInput.setVisibility(hasWorkFinal ? View.VISIBLE : View.INVISIBLE);
                }

                if (lblContainerFin != null)
                    lblContainerFin.setText(formatNumber(kanryoContainerFinal));
                if (lblBundleFin != null) lblBundleFin.setText(formatNumber(kanryoBundoleFinal));
                if (lblWeightFin != null) lblWeightFin.setText(formatNumber(kanryoJyuryoTonFinal));

                if (lblContainerPlan != null)
                    lblContainerPlan.setText(formatNumber(containerCountFinal));
                if (lblBundlePlan != null) lblBundlePlan.setText(formatNumber(goukeiBundoleFinal));
                if (lblWeightPlan != null)
                    lblWeightPlan.setText(formatNumber(goukeiJyuryoTonFinal));

                if (lblZanContainer != null)
                    lblZanContainer.setText(formatRemaining(zanContainerFinal));
                if (lblZanBundle != null) lblZanBundle.setText(formatRemaining(zanBundleFinal));
                if (lblZanWeight != null) lblZanWeight.setText(formatRemaining(zanWeightFinal));
            });
        });
    }

    //================================
    //　機　能　:　int Or Zeroの処理
    //　引　数　:　value ..... Integer
    //　戻り値　:　[long] ..... なし
    //================================
    private long intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    //==============================
    //　機　能　:　numberを整形する
    //　引　数　:　value ..... long
    //　戻り値　:　[String] ..... なし
    //==============================
    private String formatNumber(long value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    //==============================
    //　機　能　:　残数表示用の整形
    //　引　数　:　value ..... long
    //　戻り値　:　[String] ..... なし
    //==============================
    private String formatRemaining(long value) {
        return value == 0 ? "" : formatNumber(value);
    }

    //============================================
    //　機　能　:　String Mapを取り出す
    //　引　数　:　data ..... Intent
    //　　　　　:　key ..... String
    //　戻り値　:　[Map<String, String>] ..... Map
    //============================================
    private Map<String, String> readStringMap(Intent data, String key) {
        if (data == null) {
            return null;
        }
        java.io.Serializable extra = data.getSerializableExtra(key);
        if (!(extra instanceof Map)) {
            return null;
        }
        Map<?, ?> raw = (Map<?, ?>) extra;
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            Object rawKey = entry.getKey();
            Object rawValue = entry.getValue();
            if (rawKey != null && rawValue != null) {
                result.put(rawKey.toString(), rawValue.toString());
            }
        }
        return result;
    }
}
