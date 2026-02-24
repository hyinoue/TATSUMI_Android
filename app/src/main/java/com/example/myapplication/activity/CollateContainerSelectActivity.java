package com.example.myapplication.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.grid.CollateContainerSelectController;
import com.example.myapplication.grid.CollateContainerSelectRow;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//============================================================
//　処理概要　:　積込照合用のコンテナ選択画面（Activity）
//　　　　　　:　DBからコンテナ一覧を取得して表示し、照合対象№入力で選択状態を更新する。
//　　　　　　:　決定で次画面へ遷移、終了で画面を閉じる。
//　関　　数　:　onCreate ................. 画面生成/初期化（部品取得/イベント設定/一覧読込）
//　　　　　　:　bindViews ............... 画面部品のバインド
//　　　　　　:　setupBottomButtons ...... 下部ボタン設定
//　　　　　　:　setupRecycler ........... RecyclerView設定（Adapter/レイアウト/フォーカス制御）
//　　　　　　:　setupInputHandlers ...... 入力欄イベント設定（Enterで処理）
//　　　　　　:　loadContainers .......... コンテナ一覧をDBから読込
//　　　　　　:　updateUiForContainers ... 一覧有無に応じたUI制御
//　　　　　　:　handleSelectedNoInput ... 入力された照合対象№のチェック
//　　　　　　:　onFunctionBlue .......... 決定（入力チェック→選択確定→次画面へ）
//　　　　　　:　onFunctionRed ........... 未使用
//　　　　　　:　onFunctionGreen ......... 未使用
//　　　　　　:　onFunctionYellow ........ 終了
//　　　　　　:　validateAndSelect ....... 入力チェック＋選択確定
//　　　　　　:　onDestroy ............... 終了処理（スレッド停止）
//　クラス　　:　CollateContainerAdapter . 一覧表示用Adapter
//============================================================

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

        // 画面レイアウト設定
        setContentView(R.layout.activity_collate_container_select);

        // DB読込用スレッド
        io = Executors.newSingleThreadExecutor();

        // 画面初期化
        bindViews();
        setupBottomButtons();
        setupRecycler();
        setupInputHandlers();

        // 一覧読込
        loadContainers();

        // 表で線を重ねて細く見せる（罫線が二重に見えるのを抑える）
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
                // 2行目以降は上方向に詰めて線を重ねる
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
        // 照合対象№入力欄（※ID名は画面側の都合でetContainerKgになっている想定）
        etSelectedNo = findViewById(R.id.etContainerKg);

        // 一覧（※ID名はrvBundlesを流用している想定）
        rvContainers = findViewById(R.id.rvBundles);

        // 下部ボタン
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
        // ボタン文言設定
        if (btnBlue != null) btnBlue.setText("決定");
        if (btnRed != null) btnRed.setText("");
        if (btnGreen != null) btnGreen.setText("");
        if (btnYellow != null) btnYellow.setText("終了");

        // 活性制御（BaseActivity側の共通処理想定）
        refreshBottomButtonsEnabled();
    }

    //============================
    //　機　能　:　recyclerを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void setupRecycler() {
        // Adapter作成
        adapter = new CollateContainerAdapter();

        // レイアウト/Adapter設定
        rvContainers.setLayoutManager(new LinearLayoutManager(this));
        rvContainers.setAdapter(adapter);

        // 一覧側にフォーカスが当たっても、入力欄へ戻す（スキャン入力運用想定）
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

        // ソフトキーボードは表示しない（物理キー/バーコードスキャナ想定）
        etSelectedNo.setShowSoftInputOnFocus(false);

        // Enter押下で入力処理
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
        // ローディング表示
        showLoadingShort();

        io.execute(() -> {
            try {
                // DB取得
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                // コントローラ生成・一覧読込
                controller = new CollateContainerSelectController(db.kakuninContainerDao());
                controller.loadContainers();

                runOnUiThread(() -> {
                    // ローディング非表示
                    hideLoadingShort();

                    // 一覧表示へ反映
                    adapter.submitList(controller.getDisplayRows());

                    // UI制御更新
                    updateUiForContainers();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    // ローディング非表示
                    hideLoadingShort();

                    // エラー処理
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
        // 一覧が存在するか
        boolean hasContainers = controller != null && !controller.getContainers().isEmpty();

        // 入力欄の活性/フォーカス
        if (etSelectedNo != null) {
            etSelectedNo.setEnabled(hasContainers);
            if (hasContainers) {
                etSelectedNo.requestFocus();
            }
        }

        // 一覧が無い場合は「決定」を表示しない
        if (btnBlue != null) {
            btnBlue.setText(hasContainers ? "決定" : "");
        }

        // 活性制御（BaseActivity側の共通処理想定）
        refreshBottomButtonsEnabled();
    }

    //===================================
    //　機　能　:　selected No Inputを処理する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================
    private void handleSelectedNoInput() {
        if (controller == null) return;

        // 入力取得
        String input = (etSelectedNo != null && etSelectedNo.getText() != null)
                ? etSelectedNo.getText().toString().trim()
                : "";

        // 未入力チェック
        if (TextUtils.isEmpty(input)) {
            showWarningMsg("照合対照№が未入力です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return;
        }

        // 数値チェック
        int selectedNo;
        try {
            selectedNo = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showWarningMsg("照合対象№が不正です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return;
        }

        // コントローラ側のチェック処理（OK以外なら警告）
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
        // 入力チェック＋選択確定
        if (!validateAndSelect()) {
            return;
        }

        // 次画面へ遷移（選択したコンテナ情報を渡す）
        Intent intent = new Intent(this, VanningCollationActivity.class);
        intent.putExtra(VanningCollationActivity.EXTRA_CONTAINER_ID, controller.getSelectedContainerId());
        intent.putExtra(VanningCollationActivity.EXTRA_CONTAINER_NO, controller.getSelectedContainerNo());
        intent.putExtra(VanningCollationActivity.EXTRA_BUNDLE_CNT, controller.getSelectedBundleCnt());
        intent.putExtra(VanningCollationActivity.EXTRA_SAGYOU_YMD, controller.getSelectedSagyouYmd());
        startActivity(intent);

        // 本画面終了
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
        // 終了
        finish();
    }

    //===================================
    //　機　能　:　validate And Selectの処理
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... True:選択成功、False:失敗
    //===================================
    private boolean validateAndSelect() {
        if (controller == null) return false;

        // 入力取得
        String input = (etSelectedNo != null && etSelectedNo.getText() != null)
                ? etSelectedNo.getText().toString().trim()
                : "";

        // 未入力チェック
        if (TextUtils.isEmpty(input)) {
            showWarningMsg("照合対照№が未入力です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return false;
        }

        // 数値チェック
        int selectedNo;
        try {
            selectedNo = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showWarningMsg("照合対象№が不正です", MsgDispMode.MsgBox);
            if (etSelectedNo != null) etSelectedNo.requestFocus();
            return false;
        }

        // 選択確定
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
        // スレッド停止
        if (io != null) io.shutdownNow();

        super.onDestroy();
    }

    //============================================================
    //　処理概要　:　コンテナ一覧表示用Adapter
    //============================================================
    private static class CollateContainerAdapter extends RecyclerView.Adapter<CollateContainerAdapter.ViewHolder> {

        private final List<CollateContainerSelectRow> rows = new ArrayList<>();

        //====================================================
        //　機　能　:　submit Listの処理
        //　引　数　:　newRows ..... List<CollateContainerSelectRow>
        //　戻り値　:　[void] ..... なし
        //====================================================
        void submitList(List<CollateContainerSelectRow> newRows) {
            // データ入れ替え
            rows.clear();
            if (newRows != null) rows.addAll(newRows);

            // 一括更新（簡易実装）
            notifyDataSetChanged();
        }

        //================================================
        //　機　能　:　on Create View Holderの処理
        //　引　数　:　parent ..... android.view.ViewGroup
        //　　　　　:　viewType ..... int
        //　戻り値　:　[ViewHolder] ..... 生成したViewHolder
        //================================================
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            // 行レイアウトを生成
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_collate_container_select_row, parent, false);
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
            // 表示データ取得
            CollateContainerSelectRow row = rows.get(position);

            // 各列へセット
            holder.tvIndex.setText(row.index);
            holder.tvContainerNo.setText(row.containerNo);
            holder.tvBundleCnt.setText(row.bundleCnt);
            holder.tvSagyouYmd.setText(row.sagyouYmd);
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

        //============================================================
        //　処理概要　:　行表示用ViewHolder
        //============================================================
        static class ViewHolder extends RecyclerView.ViewHolder {

            final TextView tvIndex;
            final TextView tvContainerNo;
            final TextView tvBundleCnt;
            final TextView tvSagyouYmd;

            ViewHolder(android.view.View itemView) {
                super(itemView);

                // 行内View取得
                tvIndex = itemView.findViewById(R.id.tvRowIndex);
                tvContainerNo = itemView.findViewById(R.id.tvRowContainerNo);
                tvBundleCnt = itemView.findViewById(R.id.tvRowBundleCnt);
                tvSagyouYmd = itemView.findViewById(R.id.tvRowSagyouYmd);
            }
        }
    }
}
