package com.example.myapplication.activity;


import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.grid.CollateContainerRow;
import com.example.myapplication.grid.CollateContainerSelectController;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//===============================================
//　処理概要　:　CollateContainerSelectActivityクラス
//===============================================

/**
 * 積込照合用のコンテナ選択画面。
 *
 * <p>主な責務:</p>
 * <ul>
 *     <li>コンテナ一覧を取得してRecyclerViewに表示。</li>
 *     <li>コンテナ番号の入力/スキャンを受けて選択状態を切り替える。</li>
 *     <li>決定時に選択結果を返却し、終了ボタンで前画面に戻る。</li>
 * </ul>
 */
public class CollateContainerSelectActivity extends BaseActivity {

    private EditText etSelectedNo;
    private RecyclerView rvContainers;
    private MaterialButton btnBlue;
    private MaterialButton btnRed;
    private MaterialButton btnGreen;
    private MaterialButton btnYellow;

    private CollateContainerSelectController controller;
    private CollateContainerAdapter adapter;
    private ExecutorService io;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collate_container_select);

        io = Executors.newSingleThreadExecutor();

        bindViews();
        setupBottomButtons();
        setupRecycler();
        setupInputHandlers();
        loadContainers();

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
        etSelectedNo = findViewById(R.id.etContainerKg);
        rvContainers = findViewById(R.id.rvBundles);
        btnBlue = findViewById(R.id.btnBottomBlue);
        btnRed = findViewById(R.id.btnBottomRed);
        btnGreen = findViewById(R.id.btnBottomGreen);
        btnYellow = findViewById(R.id.btnBottomYellow);
    }
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupBottomButtons() {
        if (btnBlue != null) btnBlue.setText("決定");
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
        adapter = new CollateContainerAdapter();
        rvContainers.setLayoutManager(new LinearLayoutManager(this));
        rvContainers.setAdapter(adapter);
        rvContainers.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etSelectedNo != null) {
                etSelectedNo.requestFocus();
            }
        });
    }
    //================================
    //　機　能　:　input Handlersを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupInputHandlers() {
        if (etSelectedNo == null) return;

        etSelectedNo.setShowSoftInputOnFocus(false);
        etSelectedNo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                handleSelectedNoInput();
                return true;
            }
            return false;
        });
        etSelectedNo.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                handleSelectedNoInput();
                return true;
            }
            return false;
        });
    }
    //============================
    //　機　能　:　containersを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void loadContainers() {
        showLoadingShort();
        io.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                controller = new CollateContainerSelectController(db.kakuninContainerDao());
                controller.loadContainers();

                runOnUiThread(() -> {
                    hideLoadingShort();
                    adapter.submitList(controller.getDisplayRows());
                    updateUiForContainers();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("CollateContainerSelect loadContainers", ex);
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
        boolean hasContainers = controller != null && !controller.getContainers().isEmpty();
        if (etSelectedNo != null) {
            etSelectedNo.setEnabled(hasContainers);
            if (hasContainers) {
                etSelectedNo.requestFocus();
            }
        }
        if (!hasContainers && btnBlue != null) {
            btnBlue.setText("");
        } else if (hasContainers && btnBlue != null) {
            btnBlue.setText("決定");
        }
        refreshBottomButtonsEnabled();
    }
    //===================================
    //　機　能　:　selected No Inputを処理する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================

    private void handleSelectedNoInput() {
        if (controller == null) return;

        String input = etSelectedNo != null && etSelectedNo.getText() != null
                ? etSelectedNo.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(input)) {
            showWarningMsg("照合対照№が未入力です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return;
        }

        int selectedNo;
        try {
            selectedNo = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showWarningMsg("照合対象№が不正です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return;
        }

        String err = controller.checkSelectedNo(selectedNo);
        if (!TextUtils.isEmpty(err) && !"OK".equals(err)) {
            showWarningMsg(err, MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
        }
    }

    //================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    @Override
    protected void onFunctionBlue() {
        if (!validateAndSelect()) {
            return;
        }

        Intent intent = new Intent(this, VanningCollationActivity.class);
        intent.putExtra(VanningCollationActivity.EXTRA_CONTAINER_ID, controller.getSelectedContainerId());
        intent.putExtra(VanningCollationActivity.EXTRA_CONTAINER_NO, controller.getSelectedContainerNo());
        intent.putExtra(VanningCollationActivity.EXTRA_BUNDLE_CNT, controller.getSelectedBundleCnt());
        intent.putExtra(VanningCollationActivity.EXTRA_SAGYOU_YMD, controller.getSelectedSagyouYmd());
        startActivity(intent);
        finish();
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
        finish();
    }
    //===================================
    //　機　能　:　validate And Selectの処理
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //===================================

    private boolean validateAndSelect() {
        if (controller == null) return false;

        String input = etSelectedNo != null && etSelectedNo.getText() != null
                ? etSelectedNo.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(input)) {
            showWarningMsg("照合対照№が未入力です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return false;
        }

        int selectedNo;
        try {
            selectedNo = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showWarningMsg("照合対象№が不正です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return false;
        }

        String err = controller.selectContainer(selectedNo);
        if (!TextUtils.isEmpty(err) && !"OK".equals(err)) {
            showWarningMsg(err, MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return false;
        }

        return true;
    }

    //============================
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onDestroy() {
        if (io != null) io.shutdownNow();
        super.onDestroy();
    }

    private static class CollateContainerAdapter extends RecyclerView.Adapter<CollateContainerAdapter.ViewHolder> {
        private final List<CollateContainerRow> rows = new ArrayList<>();
        //====================================================
        //　機　能　:　submit Listの処理
        //　引　数　:　newRows ..... List<CollateContainerRow>
        //　戻り値　:　[void] ..... なし
        //====================================================

        void submitList(List<CollateContainerRow> newRows) {
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
                    .inflate(R.layout.item_collate_container_row, parent, false);
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
            CollateContainerRow row = rows.get(position);
            holder.tvIndex.setText(row.index);
            holder.tvContainerNo.setText(row.containerNo);
            holder.tvBundleCnt.setText(row.bundleCnt);
            holder.tvSagyouYmd.setText(row.sagyouYmd);
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
            final TextView tvIndex;
            final TextView tvContainerNo;
            final TextView tvBundleCnt;
            final TextView tvSagyouYmd;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                tvIndex = itemView.findViewById(R.id.tvRowIndex);
                tvContainerNo = itemView.findViewById(R.id.tvRowContainerNo);
                tvBundleCnt = itemView.findViewById(R.id.tvRowBundleCnt);
                tvSagyouYmd = itemView.findViewById(R.id.tvRowSagyouYmd);
            }
        }
    }
}
