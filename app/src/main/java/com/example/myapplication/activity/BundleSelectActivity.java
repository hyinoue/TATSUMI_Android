package com.example.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.evrencoskun.tableview.TableView;
import com.example.myapplication.R;
import com.example.myapplication.barcode.DensoScannerController;
import com.example.myapplication.barcode.OnScanListener;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.grid.BundleSelectController;
import com.example.myapplication.grid.BundleTableViewKit;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BundleSelectActivity extends BaseActivity {

    //============================================================
    //　処理概要　:　積載束選択 / 重量計算 画面
    //　備　　考　:　Normalモードの確定時は MenuActivity に戻らず、
    //　　　　　　:　ContainerInputActivity へ直接遷移する（チラ見え防止）
    //============================================================

    public static final String EXTRA_MODE = "bundle_select_mode";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_JYURYO = "jyuryo_calc";

    private static final int SYSTEM_RENBAN = 1;

    private EditText etContainerKg;
    private EditText etDunnageKg;
    private EditText etGenpinNo;
    private TextView tvBundleCount;
    private TextView tvTotalWeight;
    private TextView tvRemainWeight;
    private TextView tvTitle;
    private TableView tableView;

    private ExecutorService io;
    private BundleSelectController controller;
    private BundleTableViewKit.Binder tableBinder;
    private DensoScannerController scanner;

    private int maxContainerJyuryo = 0;
    private BundleSelectController.Mode mode = BundleSelectController.Mode.Normal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bundle_select);

        io = Executors.newSingleThreadExecutor();

        bindViews();
        setupMode(getIntent());
        setupBottomButtonTexts();
        setupInputHandlers();
        initScanner();

        // DB/Controller 初期化 + 初期値ロード
        initControllerAndDefaults();
    }

    //============================================================
    //　機　能　:　View取得
    //============================================================
    private void bindViews() {
        etContainerKg = findViewById(R.id.etContainerKg);
        etDunnageKg = findViewById(R.id.etDunnageKg);
        etGenpinNo = findViewById(R.id.etGenpinNo);
        tvBundleCount = findViewById(R.id.tvBundleCount);
        tvTotalWeight = findViewById(R.id.tvTotalWeight);
        tvRemainWeight = findViewById(R.id.tvRemainWeight);
        tvTitle = findViewById(R.id.tvTitle);
        tableView = findViewById(R.id.tableViewBundles);
    }

    //============================================================
    //　機　能　:　起動モード判定（Normal / JyuryoCalc）
    //============================================================
    private void setupMode(@Nullable Intent intent) {
        String modeExtra = intent != null ? intent.getStringExtra(EXTRA_MODE) : null;
        if (MODE_JYURYO.equals(modeExtra)) {
            mode = BundleSelectController.Mode.JyuryoCalc;
            if (tvTitle != null) tvTitle.setText("重量計算");
        } else {
            mode = BundleSelectController.Mode.Normal;
            if (tvTitle != null) tvTitle.setText("積載束選択");
        }
    }

    //============================================================
    //　機　能　:　入力イベント設定（重量変更/現品入力）
    //============================================================
    private void setupInputHandlers() {
        if (etContainerKg != null) etContainerKg.addTextChangedListener(weightWatcher);
        if (etDunnageKg != null) etDunnageKg.addTextChangedListener(weightWatcher);

        if (etGenpinNo != null) {
            etGenpinNo.setShowSoftInputOnFocus(false);
            etGenpinNo.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                    handleGenpinInput(v.getText() != null ? v.getText().toString() : "");
                    return true;
                }
                return false;
            });
            etGenpinNo.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    handleGenpinInput(etGenpinNo.getText() != null ? etGenpinNo.getText().toString() : "");
                    return true;
                }
                return false;
            });
        }
    }

    //============================================================
    //　機　能　:　スキャナ初期化
    //============================================================
    private void initScanner() {
        scanner = new DensoScannerController(this, new OnScanListener() {
            @Override
            public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {
                if (etGenpinNo != null) etGenpinNo.setText(normalizedData);
                handleGenpinInput(normalizedData);
            }
        });
        scanner.onCreate();
    }

    //============================================================
    //　機　能　:　Controller初期化＋初期重量読み込み＋テーブル表示
    //============================================================
    private void initControllerAndDefaults() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        io.execute(() -> {
            try {
                controller = new BundleSelectController(
                        db.syukkaMeisaiDao(),
                        db.syukkaMeisaiWorkDao(),
                        mode
                );

                SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);
                int defaultContainer = resolveDefaultContainerWeight(system);
                int defaultDunnage = resolveDefaultDunnageWeight(system);
                maxContainerJyuryo = resolveMaxContainerWeight(system);

                runOnUiThread(() -> {
                    if (etContainerKg != null)
                        etContainerKg.setText(String.valueOf(defaultContainer));
                    if (etDunnageKg != null) etDunnageKg.setText(String.valueOf(defaultDunnage));
                    setupTable();
                    updateFooter();
                    if (etGenpinNo != null) etGenpinNo.requestFocus();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> errorProcess("BundleSelect initControllerAndDefaults", ex));
            }
        });
    }

    //============================================================
    //　機　能　:　コンテナ重量の初期値取得（SystemEntity優先）
    //============================================================
    private int resolveDefaultContainerWeight(@Nullable SystemEntity system) {
        // ※必要に応じて system.defaultContainerJyuryo 等の実装に置き換え
        return 0;
    }

    //============================================================
    //　機　能　:　ダンネージ重量の初期値取得
    //============================================================
    private int resolveDefaultDunnageWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultDunnageJyuryo != null) {
            return system.defaultDunnageJyuryo;
        }
        return 0;
    }

    //============================================================
    //　機　能　:　最大積載重量の取得
    //　説　明　:　SystemEntity があればそれを優先、なければ Spinner の 20/40ft から算出
    //============================================================
    private int resolveMaxContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.maxContainerJyuryo != null && system.maxContainerJyuryo > 0) {
            return system.maxContainerJyuryo;
        }
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 30000 : 24000;
    }

    //============================================================
    //　機　能　:　テーブル表示初期化
    //============================================================
    private void setupTable() {
        if (tableView == null || controller == null) return;
        tableBinder = new BundleTableViewKit.Binder(this, tableView, controller, this::updateFooter);
        tableBinder.bind();
    }

    private final TextWatcher weightWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateFooter();
        }
    };

    //============================================================
    //　機　能　:　下ボタン文言設定（画面ごと）
    //============================================================
    private void setupBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (mode == BundleSelectController.Mode.Normal) {
            if (red != null) red.setText("束クリア");
            if (blue != null) blue.setText("確定");
        } else {
            if (red != null) red.setText("");
            if (blue != null) blue.setText("確定");
        }
        if (green != null) green.setText("");
        if (yellow != null) yellow.setText("終了");

        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　4色ボタン（赤＝束クリア）
    //============================================================
    @Override
    protected void onFunctionRed() {
        if (controller == null) return;

        showQuestion("一覧の内容を全て削除します。よろしいですか？", yes -> {
            if (!yes) return;
            io.execute(() -> {
                try {
                    controller.deleteBundles();
                    runOnUiThread(() -> {
                        if (tableBinder != null) tableBinder.refresh();
                        updateFooter();
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> errorProcess("BundleSelect deleteBundles", ex));
                }
            });
        });
    }

    //============================================================
    //　機　能　:　4色ボタン（青＝確定）
    //　説　明　:　Normalモード確定時は、メニューに戻らず ContainerInput へ直接遷移
    //============================================================
    @Override
    protected void onFunctionBlue() {
        if (!validateBeforeConfirm()) {
            return;
        }

        if (mode == BundleSelectController.Mode.Normal) {
            // ★ここで直接遷移（MenuActivity を経由しない）
            openContainerInputAndFinish();
        } else {
            // 重量計算モード：結果だけ返して戻る
            setResult(RESULT_OK);
            finish();
        }
    }

    //============================================================
    //　機　能　:　確定前チェック（入力必須・束選択必須・重量超過チェック）
    //　戻り値　:　[boolean] ..... true=OK / false=NG
    //============================================================
    private boolean validateBeforeConfirm() {
        if (controller == null) return false;

        // Normalのみ、従来通り厳密チェック（重量計算は要件に応じて調整）
        if (mode != BundleSelectController.Mode.Normal) {
            return true;
        }

        if (isEmptyOrZero(etContainerKg)) {
            showErrorMsg("コンテナ重量が未入力です", MsgDispMode.Label);
            if (etContainerKg != null) etContainerKg.requestFocus();
            return false;
        }
        if (isEmptyOrZero(etDunnageKg)) {
            showErrorMsg("ダンネージ重量が未入力です", MsgDispMode.Label);
            if (etDunnageKg != null) etDunnageKg.requestFocus();
            return false;
        }
        if (controller.getBundles().isEmpty()) {
            showErrorMsg("対象束が未選択です", MsgDispMode.Label);
            if (etGenpinNo != null) etGenpinNo.requestFocus();
            return false;
        }
        if (getRemainingWeight() < 0) {
            showErrorMsg("積載重量が超過しています", MsgDispMode.Label);
            if (etGenpinNo != null) etGenpinNo.requestFocus();
            return false;
        }
        return true;
    }

    //============================================================
    //　機　能　:　Normal確定後、コンテナ情報入力へ直接遷移して終了する
    //　説　明　:　MenuActivity に戻る瞬間を作らないため（チラ見え防止）
    //============================================================
    private void openContainerInputAndFinish() {
        Intent intent = new Intent(this, ContainerInputActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onFunctionGreen() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    //============================================================
    //　機　能　:　4色ボタン（黄＝終了）
    //============================================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //============================================================
    //　機　能　:　現品番号入力処理（バーコード/手入力）
    //============================================================
    private void handleGenpinInput(String rawInput) {
        if (controller == null) return;

        String input = rawInput != null ? rawInput.trim() : "";
        if (TextUtils.isEmpty(input)) {
            if (etGenpinNo != null) etGenpinNo.requestFocus();
            return;
        }

        showLoadingShort();
        io.execute(() -> {
            String heatNo;
            String sokuban;
            String bundleNo = null;

            try {
                if (input.length() == 13) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 13);
                } else if (input.length() == 14) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14);
                } else if (input.length() == 18) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14).trim();
                    bundleNo = input.substring(14, 18);
                    controller.addBundleNo(heatNo, sokuban, bundleNo);
                } else {
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg("現品番号は13桁か14桁か18桁で入力してください", MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                int container = getIntFromEdit(etContainerKg);
                int dunnage = getIntFromEdit(etDunnageKg);
                String errMsg = controller.checkBundle(heatNo, sokuban, container, dunnage, maxContainerJyuryo);

                if (!TextUtils.isEmpty(errMsg)) {
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg(errMsg, MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                controller.addBundle(heatNo, sokuban);

                runOnUiThread(() -> {
                    hideLoadingShort();
                    if (tableBinder != null) tableBinder.refresh();
                    updateFooter();
                    if (etGenpinNo != null) {
                        etGenpinNo.setText("");
                        etGenpinNo.requestFocus();
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("BundleSelect handleGenpinInput", ex);
                });
            }
        });
    }

    //============================================================
    //　機　能　:　フッター集計表示更新（束数/合計/残）
    //============================================================
    private void updateFooter() {
        if (controller == null) return;

        int bundleCount = controller.getBundles().size();
        int total = controller.getJyuryoSum()
                + getIntFromEdit(etContainerKg)
                + getIntFromEdit(etDunnageKg)
                + bundleCount;
        int remaining = maxContainerJyuryo - total;

        if (tvBundleCount != null) {
            tvBundleCount.setText(String.format(java.util.Locale.JAPAN, "%,d", bundleCount));
        }
        if (tvTotalWeight != null) {
            tvTotalWeight.setText(String.format(java.util.Locale.JAPAN, "%,d", total));
        }
        if (tvRemainWeight != null) {
            tvRemainWeight.setText(String.format(java.util.Locale.JAPAN, "%,d", remaining));
        }
    }

    //============================================================
    //　機　能　:　残重量取得
    //============================================================
    private int getRemainingWeight() {
        if (controller == null) return 0;

        int bundleCount = controller.getBundles().size();
        int total = controller.getJyuryoSum()
                + getIntFromEdit(etContainerKg)
                + getIntFromEdit(etDunnageKg)
                + bundleCount;
        return maxContainerJyuryo - total;
    }

    //============================================================
    //　機　能　:　EditText数値取得（カンマ除去）
    //============================================================
    private int getIntFromEdit(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) return 0;

        String raw = editText.getText().toString();
        if (TextUtils.isEmpty(raw)) return 0;

        String cleaned = raw.replace(",", "").trim();
        if (TextUtils.isEmpty(cleaned)) return 0;

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    //============================================================
    //　機　能　:　EditTextが未入力または0か判定
    //============================================================
    private boolean isEmptyOrZero(@Nullable EditText editText) {
        return getIntFromEdit(editText) <= 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) scanner.onResume();
        if (etGenpinNo != null) etGenpinNo.requestFocus();
    }

    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) scanner.onDestroy();
        if (io != null) io.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
