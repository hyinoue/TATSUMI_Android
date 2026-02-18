package com.example.myapplication.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.grid.VanningCollationController;
import com.example.myapplication.grid.VanningCollationRow;
import com.example.myapplication.scanner.DensoScannerController;
import com.example.myapplication.scanner.OnScanListener;
import com.example.myapplication.settings.HandyUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 積込照合(バンニング照合)画面のActivity。
 * <p>
 * 仕様変更：
 * - SCANキーの全画面制御はしない
 * - この画面だけ、etGenpinNoフォーカス中は Code39 のみデコードして受け取る
 */
public class VanningCollationActivity extends BaseActivity {

    public static final String EXTRA_CONTAINER_ID = "extra_container_id";
    public static final String EXTRA_CONTAINER_NO = "extra_container_no";
    public static final String EXTRA_BUNDLE_CNT = "extra_bundle_cnt";
    public static final String EXTRA_SAGYOU_YMD = "extra_sagyou_ymd";

    private EditText etContainerNo;
    private EditText etBundleCount;
    private EditText etSagyouYmd;
    private EditText etGenpinNo;
    private TextView tvReadCount;
    private RecyclerView rvBundles;
    private MaterialButton btnBlue;
    private MaterialButton btnRed;
    private MaterialButton btnGreen;
    private MaterialButton btnYellow;

    private ExecutorService io;
    private VanningCollationController controller;
    private VanningCollationAdapter adapter;

    // ★この画面専用スキャナ
    private DensoScannerController scanner;
    private boolean scannerCreated = false;

    private String containerId;
    private boolean confirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vanning_collation);

        io = Executors.newSingleThreadExecutor();

        bindViews();
        setupBottomButtons();
        setupRecycler();
        setupInputHandlers();   // ★フォーカスイベントでprofile更新
        loadFromIntent();
        loadCollationData();

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
        etContainerNo = findViewById(R.id.etContainerNo);
        etBundleCount = findViewById(R.id.etSekisaiBundleCount);
        etSagyouYmd = findViewById(R.id.etSagyouDateTime);
        etGenpinNo = findViewById(R.id.etGenpinNo);
        tvReadCount = findViewById(R.id.tvReadCount);
        rvBundles = findViewById(R.id.rvBundles);
        btnBlue = findViewById(R.id.btnBottomBlue);
        btnRed = findViewById(R.id.btnBottomRed);
        btnGreen = findViewById(R.id.btnBottomGreen);
        btnYellow = findViewById(R.id.btnBottomYellow);

        if (etContainerNo != null) etContainerNo.setEnabled(false);
        if (etBundleCount != null) etBundleCount.setEnabled(false);
        if (etSagyouYmd != null) etSagyouYmd.setEnabled(false);
    }

    private void setupBottomButtons() {
        if (btnBlue != null) btnBlue.setText("確定");
        if (btnRed != null) btnRed.setText("");
        if (btnGreen != null) btnGreen.setText("");
        if (btnYellow != null) btnYellow.setText("終了");
        refreshBottomButtonsEnabled();
    }

    private void setupRecycler() {
        adapter = new VanningCollationAdapter();
        rvBundles.setLayoutManager(new LinearLayoutManager(this));
        rvBundles.setAdapter(adapter);
        rvBundles.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etGenpinNo != null) {
                etGenpinNo.requestFocus();
            }
        });
    }

    // ★この画面だけCode39受け取り
    private void initScanner() {
        if (scannerCreated) return;
        scanner = new DensoScannerController(
                this,
                new OnScanListener() {
                    @Override
                    public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {
                        runOnUiThread(() -> {
                            if (etGenpinNo != null) etGenpinNo.setText(normalizedData);
                            handleGenpinInput();
                        });
                    }
                },
                DensoScannerController.createFocusCode39Policy(etGenpinNo)
        );

        scanner.onCreate();
        scannerCreated = true;
    }

    private void setupInputHandlers() {
        if (etGenpinNo == null) return;

        // スキャナ入力を想定し、ソフトキーボードは出さない
        etGenpinNo.setShowSoftInputOnFocus(false);

        // ★フォーカス変化時に、プロファイルを即時反映（NONE⇔CODE39_ONLY）
        etGenpinNo.setOnFocusChangeListener((v, hasFocus) -> {
            if (scanner != null) scanner.refreshProfile("GenpinFocus=" + hasFocus);
        });

        etGenpinNo.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                handleGenpinInput();
                return true;
            }
            return false;
        });
    }

    private void loadFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        containerId = intent.getStringExtra(EXTRA_CONTAINER_ID);
        String containerNo = intent.getStringExtra(EXTRA_CONTAINER_NO);
        int bundleCnt = intent.getIntExtra(EXTRA_BUNDLE_CNT, 0);
        String sagyouYmd = intent.getStringExtra(EXTRA_SAGYOU_YMD);

        if (etContainerNo != null) etContainerNo.setText(safeStr(containerNo));
        if (etBundleCount != null) etBundleCount.setText(String.valueOf(bundleCnt));
        if (etSagyouYmd != null) etSagyouYmd.setText(trimSagyouYmd(sagyouYmd));
    }

    private void loadCollationData() {
        showLoadingShort();
        io.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                controller = new VanningCollationController(db.kakuninMeisaiDao(), db.kakuninMeisaiWorkDao());
                controller.load(containerId);

                runOnUiThread(() -> {
                    hideLoadingShort();
                    adapter.submitList(controller.getDisplayRows());
                    updateReadCount();
                    updateUiForContainers();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation loadCollationData", ex);
                });
            }
        });
    }

    private void updateUiForContainers() {
        boolean hasRows = controller != null && !controller.getDetails().isEmpty();
        if (!hasRows) {
            showWarningMsg("照合対象の積載束情報がありません。", MsgDispMode.MsgBox);
            if (etGenpinNo != null) etGenpinNo.setEnabled(false);
            if (btnBlue != null) btnBlue.setText("");
        } else {
            if (etGenpinNo != null) {
                etGenpinNo.setEnabled(true);
                etGenpinNo.requestFocus();
            }
            if (btnBlue != null) btnBlue.setText("確定");
        }
        refreshBottomButtonsEnabled();

        // ★有効/無効が変わったのでprofile再反映
        if (scanner != null) scanner.refreshProfile("updateUiForContainers");
    }

    private void handleGenpinInput() {
        if (controller == null) return;

        String input = etGenpinNo != null && etGenpinNo.getText() != null
                ? etGenpinNo.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(input)) {
            showWarningMsg("現品番号が未入力です", MsgDispMode.MsgBox);
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
                } else if (input.length() == 18) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14).trim();
                } else {
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg("現品番号は13桁か14桁か18桁で入力してください", MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                String errMsg = controller.checkSokuDtl(heatNo, sokuban);
                if (!TextUtils.isEmpty(errMsg) && !"OK".equals(errMsg)) {
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg(errMsg, MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                controller.updateSyougo(heatNo, sokuban);

                runOnUiThread(() -> {
                    hideLoadingShort();
                    adapter.submitList(controller.getDisplayRows());
                    updateReadCount();
                    if (etGenpinNo != null) {
                        etGenpinNo.setText("");
                        etGenpinNo.requestFocus();
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation handleGenpinInput", ex);
                });
            }
        });
    }

    private void updateReadCount() {
        int count = controller != null ? controller.getSyougouSumiCount() : 0;
        if (tvReadCount != null) {
            tvReadCount.setText(String.format(Locale.JAPAN, "%2d", count));
        }
    }

    @Override
    protected void onFunctionBlue() {
        if (confirmed) return;
        procRegister();
    }

    @Override
    protected void onFunctionRed() {
    }

    @Override
    protected void onFunctionGreen() {
    }

    @Override
    protected void onFunctionYellow() {
        if (confirmed) {
            finish();
            return;
        }

        showQuestion("確定処理が行われていません。現在の内容は破棄されます。画面を終了してもよろしいですか？",
                yes -> {
                    if (yes) {
                        finish();
                    } else if (etGenpinNo != null) {
                        etGenpinNo.requestFocus();
                    }
                });
    }

    private void procRegister() {
        if (controller == null) return;

        showLoadingShort();
        io.execute(() -> {
            try {
                if (!checkSyougouKanryo()) {
                    runOnUiThread(this::hideLoadingShort);
                    return;
                }

                registerDb();

                DataSync sync = new DataSync(getApplicationContext());
                boolean sent = sync.sendSyougoOnly();

                runOnUiThread(() -> {
                    hideLoadingShort();
                    if (!sent) {
                        showRegisterCompleteFlow("照合データの更新に失敗しました");
                        return;
                    }
                    showRegisterCompleteFlow(null);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation procRegister", ex);
                });
            }
        });
    }

    private void showRegisterCompleteFlow(@Nullable String sendErrorMessage) {
        if (TextUtils.isEmpty(sendErrorMessage)) {
            showRegisterCompleteInfoAndFinish();
            return;
        }

        HandyUtil.playErrorBuzzer(this);
        HandyUtil.playVibrater(this);
        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage(sendErrorMessage)
                .setCancelable(false)
                .setPositiveButton("OK", (d1, w1) -> {
                    d1.dismiss();
                    getWindow().getDecorView().post(this::showRegisterCompleteInfoAndFinish);
                })
                .show();
    }

    private void showRegisterCompleteInfoAndFinish() {
        HandyUtil.playSuccessBuzzer(this);
        HandyUtil.playVibrater(this);
        new AlertDialog.Builder(this)
                .setTitle("情報")
                .setMessage("積載束照合を確定しました")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    confirmed = true;
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private boolean checkSyougouKanryo() {
        int remaining = controller != null ? controller.getUncollatedCount() : 0;
        if (remaining != 0) {
            runOnUiThread(() -> {
                showWarningMsg("照合が完了していません", MsgDispMode.MsgBox);
                if (etGenpinNo != null) etGenpinNo.requestFocus();
            });
            return false;
        }
        return true;
    }

    private void registerDb() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        db.runInTransaction(() -> controller.markContainerCollated(db.kakuninContainerDao()));
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

    private String safeStr(String value) {
        return value == null ? "" : value;
    }

    private String trimSagyouYmd(String value) {
        if (value == null) return "";
        return value.length() <= 16 ? value : value.substring(0, 16);
    }

    private static class VanningCollationAdapter extends RecyclerView.Adapter<VanningCollationAdapter.ViewHolder> {
        private final List<VanningCollationRow> rows = new ArrayList<>();

        void submitList(List<VanningCollationRow> newRows) {
            rows.clear();
            if (newRows != null) rows.addAll(newRows);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vanning_collation_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            VanningCollationRow row = rows.get(position);
            holder.tvPNo.setText(row.pNo);
            holder.tvBNo.setText(row.bNo);
            holder.tvIndex.setText(row.index);
            holder.tvJyuryo.setText(row.jyuryo);
            holder.tvConfirmed.setText(row.confirmed);
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
            final TextView tvConfirmed;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                tvPNo = itemView.findViewById(R.id.tvRowPNo);
                tvBNo = itemView.findViewById(R.id.tvRowBNo);
                tvIndex = itemView.findViewById(R.id.tvRowIndex);
                tvJyuryo = itemView.findViewById(R.id.tvRowJyuryo);
                tvConfirmed = itemView.findViewById(R.id.tvRowConfirmed);
            }
        }
    }
}
