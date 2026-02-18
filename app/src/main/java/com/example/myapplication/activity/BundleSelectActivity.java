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

/**
 * 積載束の選択および重量計算を行う画面Activity。
 * <p>
 * 仕様：
 * - SCANキーの全画面制御はしない
 * - etGenpinNoフォーカス中だけ Code39 をデコードし、アプリ処理する（それ以外はNONE）
 */
public class BundleSelectActivity extends BaseActivity {

    public static final String EXTRA_MODE = "bundle_select_mode";
    public static final String EXTRA_BUNDLE_VALUES = "bundle_select_values";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_JYURYO = "jyuryo_calc";

    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo";
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";
    private static final String PREFS_CONTAINER_JYURYO = "prefs_container_jyuryo";
    private static final String PREFS_DUNNAGE_JYURYO = "prefs_dunnage_jyuryo";

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

    // ★この画面専用：フォーカス中だけCode39
    private DensoScannerController scanner;
    private boolean scannerCreated = false;

    private final Map<String, String> bundleValues = new HashMap<>();
    private final Map<String, String> containerValues = new HashMap<>();

    private int maxContainerJyuryo = 0;
    private BundleSelectController.Mode mode = BundleSelectController.Mode.Normal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bundle_select);

        io = Executors.newSingleThreadExecutor();

        bindViews();
        setupMode(getIntent());
        loadBundleValues(getIntent());
        loadContainerValues(getIntent());
        setupBottomButtonTexts();

        setupInputHandlers(); // ★フォーカスイベントでprofile更新
        setupRecycler();

        // DB/Controller 初期化 + 初期値ロード
        initControllerAndDefaults();

        // 表で線を重ねて細く見せる
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

    private void setupMode(@Nullable Intent intent) {
        String modeExtra = intent != null ? intent.getStringExtra(EXTRA_MODE) : null;
        if (MODE_JYURYO.equals(modeExtra)) {
            mode = BundleSelectController.Mode.JyuryoCalc;
            if (tvTitle != null) tvTitle.setText("重量計算");
        } else {
            mode = BundleSelectController.Mode.Normal;
            if (tvTitle != null) tvTitle.setText("積載束選定");
        }
    }

    private void loadBundleValues(@Nullable Intent intent) {
        if (intent == null) return;
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

    private void loadContainerValues(@Nullable Intent intent) {
        if (intent == null) return;
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
    // スキャナ：フォーカス中だけCode39
    //============================
    private void initScanner() {
        if (scannerCreated) return;
        scanner = new DensoScannerController(
                this,
                new OnScanListener() {
                    @Override
                    public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {
                        // スキャン結果を入力欄に反映して同じ処理経路へ流す
                        runOnUiThread(() -> {
                            if (etGenpinNo != null) etGenpinNo.setText(normalizedData);
                            handleGenpinInput(normalizedData);
                        });
                    }
                },
                DensoScannerController.createFocusCode39Policy(etGenpinNo)
        );
        scanner.onCreate();
        scannerCreated = true;
    }

    private void setupInputHandlers() {
        // 重量入力が変わったら即時に再計算するため、監視を付ける
        if (etContainerKg != null) etContainerKg.addTextChangedListener(weightWatcher);
        if (etDunnageKg != null) etDunnageKg.addTextChangedListener(weightWatcher);

        if (etContainerKg != null) {
            etContainerKg.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && etDunnageKg != null) {
                    etDunnageKg.requestFocus();
                }
                return true;
            });
            etContainerKg.setOnEditorActionListener((v, actionId, event) -> {
                if (!isEnterAction(actionId, event)) return false;
                if (etDunnageKg != null) etDunnageKg.requestFocus();
                return true;
            });
        }

        if (etDunnageKg != null) {
            etDunnageKg.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && etGenpinNo != null) {
                    etGenpinNo.requestFocus();
                }
                return true;
            });
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

            etGenpinNo.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // 物理Enterキーにも対応
                    handleGenpinInput(etGenpinNo.getText() != null ? etGenpinNo.getText().toString() : "");
                }
                return true;
            });
            etGenpinNo.setOnEditorActionListener((v, actionId, event) -> {
                if (!isEnterAction(actionId, event)) return false;
                handleGenpinInput(etGenpinNo.getText() != null ? etGenpinNo.getText().toString() : "");
                return true;
            });
        }
    }

    private boolean isEnterAction(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }

    private void initControllerAndDefaults() {
        showLoadingShort();
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
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    String prefContainer = prefs.getString(PREFS_CONTAINER_JYURYO, "");
                    String prefDunnage = prefs.getString(PREFS_DUNNAGE_JYURYO, "");
                    boolean hasContainer = bundleValues.containsKey(KEY_CONTAINER_JYURYO);
                    boolean hasDunnage = bundleValues.containsKey(KEY_DUNNAGE_JYURYO);
                    if (hasContainer || hasDunnage) {
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
                    refreshRows();
                    updateFooter();
                    if (etGenpinNo != null) etGenpinNo.requestFocus();
                    hideLoadingShort();

                    // ★初期表示後もプロファイル反映
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

    private int resolveDefaultContainerWeight(@Nullable SystemEntity system) {
        // ※必要に応じて system.defaultContainerJyuryo 等の実装に置き換え
        return 0;
    }

    private int resolveDefaultDunnageWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultDunnageJyuryo != null) {
            return system.defaultDunnageJyuryo;
        }
        return 0;
    }

    private int resolveMaxContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.maxContainerJyuryo != null && system.maxContainerJyuryo > 0) {
            return system.maxContainerJyuryo;
        }
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 30000 : 24000;
    }

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
            persistContainerWeights();
        }
    };

    private void persistContainerWeights() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String container = etContainerKg != null && etContainerKg.getText() != null
                ? etContainerKg.getText().toString().trim()
                : "";
        String dunnage = etDunnageKg != null && etDunnageKg.getText() != null
                ? etDunnageKg.getText().toString().trim()
                : "";
        prefs.edit()
                .putString(PREFS_CONTAINER_JYURYO, container)
                .putString(PREFS_DUNNAGE_JYURYO, dunnage)
                .apply();
    }

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

    @Override
    protected void onFunctionBlue() {
        if (!validateBeforeConfirm()) {
            return;
        }
        openContainerInputAndFinish();
    }

    private boolean validateBeforeConfirm() {
        if (controller == null) return false;

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

    private void openContainerInputAndFinish() {
        saveBundleInputValues();
        syncContainerValuesFromBundle();
        Intent intent = new Intent(this, ContainerInputActivity.class);
        intent.putExtra(ContainerInputActivity.EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
        intent.putExtra(ContainerInputActivity.EXTRA_CONTAINER_VALUES, new HashMap<>(containerValues));
        startActivity(intent);
        finish();
    }

    private void syncContainerValuesFromBundle() {
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

    @Override
    protected void onFunctionGreen() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    @Override
    protected void onFunctionYellow() {
        finish();
    }

    private void handleGenpinInput(String rawInput) {
        if (controller == null) return;

        String input = rawInput != null ? rawInput.trim() : "";
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

                String errMsg = controller.checkBundle(
                        heatNo,
                        sokuban,
                        getIntValue(etContainerKg),
                        getIntValue(etDunnageKg),
                        maxContainerJyuryo
                );

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

    private String formatNumber(int value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(() -> {
            initScanner();
            if (scanner != null) scanner.onResume();
            if (scanner != null) scanner.refreshProfile("onResume");
        });
    }

    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    @Override
    public void finish() {
        saveBundleInputValues();
        Intent result = new Intent();
        result.putExtra(EXTRA_BUNDLE_VALUES, new HashMap<>(bundleValues));
        setResult(RESULT_OK, result);
        super.finish();
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) scanner.onDestroy();
        scannerCreated = false;
        if (io != null) io.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void saveBundleInputValues() {
        String container = etContainerKg != null && etContainerKg.getText() != null
                ? etContainerKg.getText().toString().trim()
                : "";
        String dunnage = etDunnageKg != null && etDunnageKg.getText() != null
                ? etDunnageKg.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(container) && TextUtils.isEmpty(dunnage)) {
            bundleValues.clear();
            return;
        }
        bundleValues.put(KEY_CONTAINER_JYURYO, container);
        bundleValues.put(KEY_DUNNAGE_JYURYO, dunnage);
    }

    private static class BundleRowAdapter extends RecyclerView.Adapter<BundleRowAdapter.ViewHolder> {
        interface DeleteHandler {
            void delete(int row);
        }

        private final List<BundleSelectRow> rows = new ArrayList<>();
        private final DeleteHandler deleteHandler;

        BundleRowAdapter(DeleteHandler deleteHandler) {
            this.deleteHandler = deleteHandler;
        }

        void submitList(List<BundleSelectRow> newRows) {
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
            BundleSelectRow row = rows.get(position);
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
