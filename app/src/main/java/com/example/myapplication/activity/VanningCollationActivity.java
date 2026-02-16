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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.grid.VanningCollationController;
import com.example.myapplication.grid.VanningCollationRow;
import com.example.myapplication.scanner.DensoScannerController;
import com.example.myapplication.scanner.OnScanListener;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//=========================================
//　処理概要　:　VanningCollationActivityクラス
//=========================================

/**
 * 積込照合(バンニング照合)画面のActivity。
 *
 * <p>コンテナ情報と読み取り結果を突き合わせ、
 * 読み取った束の件数/一覧をリアルタイムに表示する。</p>
 *
 * <p>主な処理フロー:</p>
 * <ul>
 *     <li>Intentから対象コンテナ情報を取得して画面に表示。</li>
 *     <li>スキャナ/入力欄から原品番号を受け取り照合処理を実行。</li>
 *     <li>確定で照合結果を保存し、終了で前画面へ戻る。</li>
 * </ul>
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
    private DensoScannerController scanner;
    private String containerId;
    private boolean confirmed;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vanning_collation);

        io = Executors.newSingleThreadExecutor();

        bindViews();
        setupBottomButtons();
        setupRecycler();
        setupInputHandlers();
        initScanner();
        loadFromIntent();
        loadCollationData();

        //表で線を重ねて細く見せる
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
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupBottomButtons() {
        if (btnBlue != null) btnBlue.setText("確定");
        if (btnRed != null) btnRed.setText("");
        if (btnGreen != null) btnGreen.setText("");
        if (btnYellow != null) btnYellow.setText("終了");
        refreshBottomButtonsEnabled();
    }
    //============================
    //　機　能　:　recyclerを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

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
    //================================
    //　機　能　:　input Handlersを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupInputHandlers() {
        if (etGenpinNo == null) return;

        // スキャナ入力を想定し、ソフトキーボードは出さない
        etGenpinNo.setShowSoftInputOnFocus(false);
        etGenpinNo.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                // 物理Enterキーの入力にも対応
                handleGenpinInput();
                return true;
            }
            return false;
        });
    }
    //============================
    //　機　能　:　scannerを初期化する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void initScanner() {
        scanner = new DensoScannerController(this, new OnScanListener() {
            //========================================
            //　機　能　:　スキャン受信時の処理
            //　引　数　:　normalizedData ..... String
            //　　　　　:　aim ..... String
            //　　　　　:　denso ..... String
            //　戻り値　:　[void] ..... なし
            //========================================
            @Override
            public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {
                runOnUiThread(() -> {
                    // スキャン結果を入力欄に入れて同一処理フローに流す
                    if (etGenpinNo != null) {
                        etGenpinNo.setText(normalizedData);
                    }
                    handleGenpinInput();
                });
            }
        });
    }
    //=============================
    //　機　能　:　from Intentを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================

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
    //================================
    //　機　能　:　collation Dataを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

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
    //===================================
    //　機　能　:　ui For Containersを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================

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
    }
    //==============================
    //　機　能　:　genpin Inputを処理する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================

    private void handleGenpinInput() {
        if (controller == null) return;

        // 入力値を取得して整形（空白除去/未入力チェック）
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
    //============================
    //　機　能　:　read Countを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void updateReadCount() {
        int count = controller != null ? controller.getSyougouSumiCount() : 0;
        if (tvReadCount != null) {
            tvReadCount.setText(String.format(Locale.JAPAN, "%2d", count));
        }
    }

    //================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    @Override
    protected void onFunctionBlue() {
        if (confirmed) {
            return;
        }
        procRegister();
    }

    //===============================
    //　機　能　:　on Function Redの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===============================
    @Override
    protected void onFunctionRed() {
        // 今は空（ボタンTextが空なので実行されない想定）
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
    //=============================
    //　機　能　:　proc Registerの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================

    private void procRegister() {
        if (controller == null) return;

        showLoadingShort();
        io.execute(() -> {
            try {
                if (!checkSyougouKanryo()) {
                    runOnUiThread(() -> hideLoadingShort());
                    return;
                }

                registerDb();

                DataSync sync = new DataSync(getApplicationContext());
                boolean sent = sync.sendSyougoOnly();

                runOnUiThread(() -> {
                    hideLoadingShort();
                    if (!sent) {
                        showErrorMsg("照合データの更新に失敗しました", MsgDispMode.MsgBox);
                    }
                    confirmed = true;
                    showInfoMsg("積載束照合を確定しました", MsgDispMode.MsgBox);
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation procRegister", ex);
                });
            }
        });
    }
    //====================================
    //　機　能　:　check Syougou Kanryoの処理
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //====================================

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
    //============================
    //　機　能　:　register Dbの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void registerDb() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        db.runInTransaction(() -> controller.markContainerCollated(db.kakuninContainerDao()));
    }

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) scanner.onResume();
    }

    //============================
    //　機　能　:　画面一時停止時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    //============================
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onDestroy() {
        if (scanner != null) scanner.onDestroy();
        if (io != null) io.shutdownNow();
        super.onDestroy();
    }
    //===============================
    //　機　能　:　safe Strの処理
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... なし
    //===============================

    private String safeStr(String value) {
        return value == null ? "" : value;
    }
    //===============================
    //　機　能　:　trim Sagyou Ymdの処理
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... なし
    //===============================

    private String trimSagyouYmd(String value) {
        if (value == null) return "";
        return value.length() <= 16 ? value : value.substring(0, 16);
    }

    private static class VanningCollationAdapter extends RecyclerView.Adapter<VanningCollationAdapter.ViewHolder> {
        private final List<VanningCollationRow> rows = new ArrayList<>();
        //====================================================
        //　機　能　:　submit Listの処理
        //　引　数　:　newRows ..... List<VanningCollationRow>
        //　戻り値　:　[void] ..... なし
        //====================================================

        void submitList(List<VanningCollationRow> newRows) {
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
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vanning_collation_row, parent, false);
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
            VanningCollationRow row = rows.get(position);
            holder.tvPNo.setText(row.pNo);
            holder.tvBNo.setText(row.bNo);
            holder.tvIndex.setText(row.index);
            holder.tvJyuryo.setText(row.jyuryo);
            holder.tvConfirmed.setText(row.confirmed);
        }

        //============================
        //　機　能　:　item Countを取得する
        //　引　数　:　なし
        //　戻り値　:　[int] ..... なし
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
