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

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.grid.BundleGridRow;
import com.example.myapplication.grid.BundleSelectController;
import com.example.myapplication.scanner.DensoScannerController;
import com.example.myapplication.scanner.OnScanListener;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private RecyclerView rvBundles;

    private ExecutorService io;
    private BundleSelectController controller;
    private BundleRowAdapter adapter;
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
        setupRecycler();
        initScanner();

        // DB/Controller 初期化 + 初期値ロード
        initControllerAndDefaults();

        //表で線を重ねて細く見せる
        RecyclerView rvBundles = findViewById(R.id.rvBundles);
        rvBundles.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position > 0) {
                    outRect.top = -2;
                }
            }
        });
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
        rvBundles = findViewById(R.id.rvBundles);
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
                    refreshRows();
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
    //　機　能　:　一覧表示初期化
    //============================================================
    private void setupRecycler() {
        if (rvBundles == null) return;
        adapter = new BundleRowAdapter(this::confirmDeleteRow);
        rvBundles.setLayoutManager(new LinearLayoutManager(this));
        rvBundles.setAdapter(adapter);
    }

    private void refreshRows() {
        if (adapter == null || controller == null) return;
        adapter.submitList(controller.getDisplayRows());
    }

    private void confirmDeleteRow(int row) {
        new AlertDialog.Builder(this)
                .setMessage("行を削除します。よろしいですか？")
                .setPositiveButton("いいえ", null)
                .setNegativeButton("はい", (d, w) -> deleteBundleRow(row))
                .show();
    }

    private void deleteBundleRow(int row) {
        if (controller == null) return;
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
            if (blue != null) blue.setText("確定");
        } else {
            if (blue != null) blue.setText("");
        }

        if (red != null) red.setText("束クリア");
        if (green != null) green.setText("");
        if (yellow != null) yellow.setText("終了");
        refreshBottomButtonsEnabled();
    }

    @Override
    protected void onFunctionRed() {
        if (controller == null) return;

        showQuestion("一覧の内容を全て削除します。よろしいですか？", yes -> {
            if (!yes) return;
            io.execute(() -> {
                try {
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

    //============================================================
    //　機　能　:　4色ボタン（青＝確定）
    //　説　明　:　Normalモード確定時は、メニューに戻らず ContainerInput へ直接遷移
    //============================================================
    @Override
    protected void onFunctionBlue() {
        if (!validateBeforeConfirm()) {
            return;
        }
        // ★ここで直接遷移（MenuActivity を経由しない）
        openContainerInputAndFinish();
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
    //　説　明　:　MenuActivity に戻る瞬間を作らないため（ラ見え防止）
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

            try {
                if (input.length() == 13) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 13);
                } else if (input.length() == 14) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14);
                } else {
                    throw new IllegalArgumentException("現品番号の形式が不正です");
                }

                controller.addBundle(heatNo, sokuban);
                runOnUiThread(() -> {
                    refreshRows();
                    updateFooter();
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

    //============================================================
    //　機　能　:　残重量の計算
    //============================================================
    private int getRemainingWeight() {
        int total = getTotalWeight();
        return maxContainerJyuryo - total;
    }

    private int getTotalWeight() {
        int bundle = controller != null ? controller.getJyuryoSum() : 0;
        int bundleCount = controller != null ? controller.getBundles().size() : 0;
        int container = getIntValue(etContainerKg);
        int dunnage = getIntValue(etDunnageKg);
        return bundle + bundleCount + container + dunnage;
    }

    private int getIntValue(EditText et) {
        if (et == null) return 0;
        String s = et.getText() != null ? et.getText().toString() : "";
        if (TextUtils.isEmpty(s)) return 0;
        String cleaned = s.replace(",", "").trim();
        if (TextUtils.isEmpty(cleaned)) return 0;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isEmptyOrZero(EditText et) {
        return getIntValue(et) <= 0;
    }

    private void updateFooter() {
        int count = controller != null ? controller.getBundles().size() : 0;
        int total = getTotalWeight();
        int remain = getRemainingWeight();

        if (tvBundleCount != null) tvBundleCount.setText(String.valueOf(count));
        if (tvTotalWeight != null) tvTotalWeight.setText(formatNumber(total));
        if (tvRemainWeight != null) tvRemainWeight.setText(formatNumber(remain));
    }

    //============================================================
    //　機　能　:　数値表示（DecimalFormat禁止 → String.formatで統一）
    //============================================================
    private String formatNumber(int value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) scanner.onResume();
    }

    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) scanner.onDestroy();
        if (io != null) io.shutdownNow();
        super.onDestroy();
    }

    private static class BundleRowAdapter extends RecyclerView.Adapter<BundleRowAdapter.ViewHolder> {
        interface DeleteHandler {
            void delete(int row);
        }

        private final List<BundleGridRow> rows = new ArrayList<>();
        private final DeleteHandler deleteHandler;

        BundleRowAdapter(DeleteHandler deleteHandler) {
            this.deleteHandler = deleteHandler;
        }

        void submitList(List<BundleGridRow> newRows) {
            rows.clear();
            if (newRows != null) rows.addAll(newRows);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bundle_select_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BundleGridRow row = rows.get(position);
            holder.tvPNo.setText(row.pNo);
            holder.tvBNo.setText(row.bNo);
            holder.tvIndex.setText(row.index);
            holder.tvJyuryo.setText(row.jyuryo);
            holder.tvJyuryo.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            holder.tvDelete.setText(row.cancelText);

            holder.tvDelete.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && deleteHandler != null) {
                    deleteHandler.delete(adapterPosition);
                }
            });
        }


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
