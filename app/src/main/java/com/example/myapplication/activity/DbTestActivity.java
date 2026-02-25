package com.example.myapplication.activity;

import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//============================================================
//　処理概要　:　DB参照テスト画面
//　　　　　　:　Room(DB)のテーブル一覧をSpinnerに表示し、選択テーブルの内容をRecyclerViewに表示する。
//　　　　　　:　ヘッダ行は固定表示し、RecyclerView本体は横スクロール可能なため、
//　　　　　　:　列幅はヘッダ/セルの最大文字幅から算出して揃える。
//　関　　数　:　onCreate               ..... 画面初期化/テーブル一覧読込/イベント設定
//　　　　　　:　onDestroy              ..... スレッド停止/生存フラグOFF
//　　　　　　:　loadTableNames         ..... sqlite_master からテーブル一覧を取得
//　　　　　　:　loadTableData          ..... 指定テーブルのデータを取得して表示
//　　　　　　:　updateHeaderRow        ..... ヘッダ行を生成し、列幅に合わせて配置
//　　　　　　:　buildHeaderCell        ..... ヘッダセル(TextView)生成
//　　　　　　:　dp                     ..... dp→px 変換
//　　　　　　:　setupBottomButtons     ..... 下部ボタン文字設定
//　　　　　　:　setTableVisible        ..... 表示領域の表示/非表示
//　　　　　　:　applyCellLayoutParams  ..... セルのLayoutParams適用
//　　　　　　:　onFunctionYellow       ..... (黄)終了
//　クラス　　:　DbTableAdapter         ..... DBテーブル表示用RecyclerViewアダプタ
//============================================================

public class DbTestActivity extends BaseActivity {

    private Spinner spTables; // テーブル選択スピナー

    private LinearLayout headerRow; // 固定ヘッダ行

    private RecyclerView rvDbTable; // DBテーブル表示一覧

    private HorizontalScrollView hsvDbTable; // 横スクロール領域

    private AppDatabase roomDb; // Room DBインスタンス

    private ExecutorService executor; // DB読み込み用スレッド

    private DbTableAdapter tableAdapter; // DBテーブル一覧アダプター

    private volatile boolean isAlive = false; // 画面生存フラグ

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_test);

        // Activity生存フラグON（非同期戻りでUI更新可否を判断）
        isAlive = true;

        // ---- View取得 ----
        spTables = findViewById(R.id.spContainerSize);
        headerRow = findViewById(R.id.rowTableHeader);
        rvDbTable = findViewById(R.id.rvDbTable);
        hsvDbTable = findViewById(R.id.hsvDbTable);

        // Viewが見つからない場合は即例外（レイアウトミスを早期検知）
        if (spTables == null) throw new IllegalStateException("View is null: spContainerSize");
        if (headerRow == null) throw new IllegalStateException("View is null: rowTableHeader");
        if (rvDbTable == null) throw new IllegalStateException("View is null: rvDbTable");
        if (hsvDbTable == null) throw new IllegalStateException("View is null: hsvDbTable");

        // ---- RecyclerView初期化 ----
        tableAdapter = new DbTableAdapter();
        rvDbTable.setLayoutManager(new LinearLayoutManager(this));
        rvDbTable.setAdapter(tableAdapter);

        // 横スクロールバー表示（HorizontalScrollView側で横スクロール）
        rvDbTable.setHorizontalScrollBarEnabled(true);

        // 行間の境界線が重なって太く見えないように、上方向を少し詰める
        rvDbTable.addItemDecoration(new RecyclerView.ItemDecoration() {
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
                // 先頭行以外は上方向に-2pxして境界線を重ねる
                int position = parent.getChildAdapterPosition(view);
                if (position > 0) {
                    outRect.top = -2;
                }
            }
        });

        // 初期表示ではテーブル領域を非表示（未選択状態）
        setTableVisible(false);

        // 下部ボタン表示
        setupBottomButtons();

        // ---- DB/スレッド準備 ----
        roomDb = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Spinnerへテーブル名一覧を読み込む
        loadTableNames();

        // ---- Spinner選択イベント ----
        spTables.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            //========================================
            //　機　能　:　on Item Selectedの処理
            //　引　数　:　parent ..... AdapterView<?>
            //　　　　　:　view ..... View
            //　　　　　:　position ..... int
            //　　　　　:　id ..... long
            //　戻り値　:　[void] ..... なし
            //========================================
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Spinnerの選択値（テーブル名）を取得
                String table = (String) parent.getItemAtPosition(position);

                // 未選択（空文字）なら表示をクリアして非表示に戻す
                if (table == null || table.trim().isEmpty()) {
                    tableAdapter.setTable(new ArrayList<>(), new ArrayList<>());
                    updateHeaderRow(
                            new ArrayList<>(),
                            tableAdapter.getColumnWidthsPx(),
                            tableAdapter.getRowHeaderWidthPx(),
                            tableAdapter.getRowHeightPx()
                    );
                    setTableVisible(false);
                    return;
                }

                // 選択されたテーブルのデータを読み込んで表示
                loadTableData(table);
            }

            //========================================
            //　機　能　:　on Nothing Selectedの処理
            //　引　数　:　parent ..... AdapterView<?>
            //　戻り値　:　[void] ..... なし
            //========================================
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 特に処理なし（必要ならクリア処理を入れる）
            }
        });
    }

    //============================
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onDestroy() {
        // 非同期の戻りでUI更新しないようにする
        isAlive = false;

        // 実行中タスクがあれば中断（DB参照を止める）
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    // =============================================================================================
    // DB
    // =============================================================================================

    //=============================
    //　機　能　:　table Namesを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================
    private void loadTableNames() {
        // DBアクセスはバックグラウンドで行う
        executor.execute(() -> {
            List<String> list = new ArrayList<>();

            // Roomの下層SQLite DBを取得（read-only）
            SupportSQLiteDatabase db = roomDb.getOpenHelper().getReadableDatabase();

            // sqlite_masterからユーザテーブル名だけを抽出
            // sqlite_ 系や room の管理テーブル等は除外
            try (Cursor c = db.query(
                    "SELECT name FROM sqlite_master " +
                            "WHERE type='table' " +
                            "AND name NOT LIKE 'sqlite_%' " +
                            "AND name NOT IN ('android_metadata', 'room_master_table') " +
                            "ORDER BY name ASC"
            )) {
                while (c.moveToNext()) {
                    list.add(c.getString(0));
                }
            }

            // UI反映はメインスレッドで行う
            runOnUiThread(() -> {
                // 画面終了後に戻ってきた場合は何もしない
                if (!isAlive || isFinishing() || isDestroyed()) return;

                // Spinnerには「未選択用の空文字」を先頭に入れる
                List<String> displayList = new ArrayList<>();
                displayList.add("");
                displayList.addAll(list);

                // Spinnerアダプタ設定
                ArrayAdapter<String> ad = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        displayList
                );
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTables.setAdapter(ad);

                // 初期選択は未選択
                spTables.setSelection(0);
            });
        });
    }

    //===============================
    //　機　能　:　table Dataを読み込む
    //　引　数　:　table ..... String
    //　戻り値　:　[void] ..... なし
    //===============================
    private void loadTableData(String table) {
        // DBアクセスはバックグラウンドで行う
        executor.execute(() -> {
            // カラム名
            List<String> cols = new ArrayList<>();
            // 行データ（文字列化して保持）
            List<List<String>> rows = new ArrayList<>();

            SupportSQLiteDatabase db = roomDb.getOpenHelper().getReadableDatabase();

            // SELECT * で全件取得（デバッグ用途のため制限なし）
            // ※テーブルが大きい場合は重くなるため注意
            try (Cursor c = db.query("SELECT * FROM " + table)) {
                // カラム名を保持
                for (String s : c.getColumnNames()) cols.add(s);

                // 各行を文字列化して保持（NULLは"NULL"表示）
                while (c.moveToNext()) {
                    List<String> r = new ArrayList<>();
                    for (int i = 0; i < cols.size(); i++) {
                        r.add(c.isNull(i) ? "NULL" : c.getString(i));
                    }
                    rows.add(r);
                }
            }

            // UI反映はメインスレッド
            runOnUiThread(() -> {
                if (!isAlive || isFinishing() || isDestroyed()) return;

                // アダプタに設定（列幅の自動調整もここで実行される）
                tableAdapter.setTable(cols, rows);

                // ヘッダ行を列幅に合わせて再構築
                updateHeaderRow(
                        cols,
                        tableAdapter.getColumnWidthsPx(),
                        tableAdapter.getRowHeaderWidthPx(),
                        tableAdapter.getRowHeightPx()
                );

                // 表示領域を表示
                setTableVisible(true);
            });
        });
    }

    // =============================================================================================
    // Header / Layout
    // =============================================================================================

    //=========================================
    //　機　能　:　header Rowを更新する
    //　引　数　:　columns ..... List<String>
    //　　　　　:　widthsPx ..... List<Integer>
    //　　　　　:　rowHeaderWidthPx ..... int
    //　　　　　:　rowHeightPx ..... int
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void updateHeaderRow(List<String> columns, List<Integer> widthsPx, int rowHeaderWidthPx, int rowHeightPx) {
        // 既存ヘッダをクリア
        headerRow.removeAllViews();

        // 先頭の行番号用ヘッダ（空文字）
        TextView rowHeader = buildHeaderCell("", rowHeaderWidthPx, rowHeightPx, 0);
        headerRow.addView(rowHeader);

        // 各カラムのヘッダセルを作成し追加
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);

            // アダプタ計算済みの列幅があれば使う。無ければデフォルト幅
            int widthPx = (i < widthsPx.size()) ? widthsPx.get(i) : dp(120);

            // 罫線の重なりを意図して左マージンを-2px
            TextView tv = buildHeaderCell(column, widthPx, rowHeightPx, -dp(2));
            headerRow.addView(tv);
        }
    }

    //===================================
    //　機　能　:　header Cellを生成する
    //　引　数　:　label ..... String
    //　　　　　:　widthPx ..... int
    //　　　　　:　heightPx ..... int
    //　　　　　:　leftMarginPx ..... int
    //　戻り値　:　[TextView] ..... ヘッダセル
    //===================================
    private TextView buildHeaderCell(String label, int widthPx, int heightPx, int leftMarginPx) {
        TextView tv = new TextView(this);

        // 表示文字
        tv.setText(label);

        // 見た目（固定ヘッダ用）
        tv.setTextColor(0xFF000000);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.bg_text_readonly);
        tv.setPadding(dp(6), dp(4), dp(6), dp(4));

        // サイズ/マージンを適用
        applyCellLayoutParams(tv, widthPx, heightPx, leftMarginPx);
        return tv;
    }

    //===========================
    //　機　能　:　dpの処理
    //　引　数　:　v ..... int
    //　戻り値　:　[int] ..... px
    //===========================
    private int dp(int v) {
        // densityを掛けてdp→pxに変換
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    private void setupBottomButtons() {
        // 黄：終了のみ使用
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        if (yellow != null) {
            yellow.setText("終了");
        }

        // その他は空にして無効化（BaseActivityでdisable＋薄表示）
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");

        refreshBottomButtonsEnabled();
    }

    //====================================
    //　機　能　:　table Visibleを設定する
    //　引　数　:　isVisible ..... boolean
    //　戻り値　:　[void] ..... なし
    //====================================
    private void setTableVisible(boolean isVisible) {
        // テーブル領域全体の表示/非表示を切り替え
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        hsvDbTable.setVisibility(visibility);
    }

    //========================================
    //　機　能　:　apply Cell Layout Paramsの処理
    //　引　数　:　cell ..... TextView
    //　　　　　:　widthPx ..... int
    //　　　　　:　heightPx ..... int
    //　　　　　:　leftMarginPx ..... int
    //　戻り値　:　[void] ..... なし
    //========================================
    private void applyCellLayoutParams(TextView cell, int widthPx, int heightPx, int leftMarginPx) {
        // 横並びセルのサイズ/左マージンを設定
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, heightPx);
        lp.setMargins(leftMarginPx, 0, 0, 0);
        cell.setLayoutParams(lp);
    }

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        // 画面終了
        finish();
    }

    //============================================================
    // Adapter
    //============================================================
    private class DbTableAdapter extends RecyclerView.Adapter<DbTableAdapter.RowViewHolder> {

        // 見た目（dp/spはAdapter内で統一）
        private final int rowHeaderWidthDp = 48;  // 行番号列の幅
        private final int rowHeightDp = 28;       // 1行の高さ
        private final int textSp = 15;            // 文字サイズ

        // 列幅（px）自動調整結果を保持（ヘッダも同じ幅を使う）
        private final List<Integer> colWidthsPx = new ArrayList<>();

        // 表示データ保持（列幅計算/表示用）
        private final List<String> currentColumns = new ArrayList<>();
        private final List<List<String>> currentRows = new ArrayList<>();

        //==============================
        //　機　能　:　normの処理
        //　引　数　:　s ..... String
        //　戻り値　:　[String] ..... 変換後文字列
        //==============================
        private String norm(String s) {
            // 改行があるとTextView計測/表示が崩れるためスペースへ置換
            if (s == null) return "";
            return s.replace("\n", " ").replace("\r", " ");
        }

        //================================
        //　機　能　:　item View Typeを取得する
        //　引　数　:　position ..... int
        //　戻り値　:　[int] ..... 列数（セル生成数に利用）
        //================================
        @Override
        public int getItemViewType(int position) {
            // viewTypeに列数を渡し、onCreateViewHolderでその数だけセルを生成する
            return currentColumns.size();
        }

        //==========================================
        //　機　能　:　tableを設定する
        //　引　数　:　cols ..... List<String>
        //　　　　　:　data ..... List<List<String>>
        //　戻り値　:　[void] ..... なし
        //==========================================
        void setTable(List<String> cols, List<List<String>> data) {
            // 既存データをクリアして差し替え
            currentColumns.clear();
            currentRows.clear();
            currentColumns.addAll(cols);
            currentRows.addAll(data);

            // 列幅を再計算
            autoAdjustColumnWidths();

            // 一括更新（DiffUtil未使用・デバッグ用途）
            notifyDataSetChanged();
        }

        /**
         * 列幅を「ヘッダ（太字） + セル（通常）」の最大文字幅に合わせて決める（上限なし）
         * ★最後の1文字見切れ対策で safety を少し足す
         */
        //=========================================
        //　機　能　:　auto Adjust Column Widthsの処理
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //=========================================
        private void autoAdjustColumnWidths() {
            colWidthsPx.clear();

            // 計測用TextView（太字＝ヘッダ、通常＝セル）
            TextView headerMeasure = new TextView(DbTestActivity.this);
            headerMeasure.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            headerMeasure.setTypeface(headerMeasure.getTypeface(), android.graphics.Typeface.BOLD);

            TextView cellMeasure = new TextView(DbTestActivity.this);
            cellMeasure.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);

            int pad = dp(12);       // 左右padding相当
            int min = dp(60);       // 最小幅（短い値でも潰れないように）
            int safety = dp(4);     // 見切れ防止（端末レンダ差対策）

            // 各列について最大幅を計算
            for (int c = 0; c < currentColumns.size(); c++) {
                float w = 0f;

                // ヘッダ文字幅（太字）
                String header = norm(currentColumns.get(c));
                w = Math.max(w, headerMeasure.getPaint().measureText(header));

                // セル文字幅（通常）
                for (List<String> r : currentRows) {
                    if (c >= r.size()) continue;
                    String v = norm(r.get(c));
                    w = Math.max(w, cellMeasure.getPaint().measureText(v));
                }

                // 幅（文字幅 + padding + safety）をpxで確定
                int wPx = (int) Math.ceil(w) + pad + safety;
                if (wPx < min) wPx = min;

                colWidthsPx.add(wPx);
            }
        }

        //=====================================
        //　機　能　:　column Widths Pxを取得する
        //　引　数　:　なし
        //　戻り値　:　[List<Integer>] ..... 列幅(px)のコピー
        //=====================================
        List<Integer> getColumnWidthsPx() {
            // 外部から変更されないようにコピーを返す
            return new ArrayList<>(colWidthsPx);
        }

        //=====================================
        //　機　能　:　row Header Width Pxを取得する
        //　引　数　:　なし
        //　戻り値　:　[int] ..... 行番号列幅(px)
        //=====================================
        int getRowHeaderWidthPx() {
            return dp(rowHeaderWidthDp);
        }

        //===============================
        //　機　能　:　row Height Pxを取得する
        //　引　数　:　なし
        //　戻り値　:　[int] ..... 行高さ(px)
        //===============================
        int getRowHeightPx() {
            return dp(rowHeightDp);
        }

        //=====================================
        //　機　能　:　col Width Pxを取得する
        //　引　数　:　columnPosition ..... int
        //　戻り値　:　[int] ..... 列幅(px)
        //=====================================
        private int getColWidthPx(int columnPosition) {
            // 計算済みがあればそれを返す。無い場合はデフォルト
            if (columnPosition >= 0 && columnPosition < colWidthsPx.size()) {
                return colWidthsPx.get(columnPosition);
            }
            return dp(120);
        }

        //=====================================
        //　機　能　:　on Create View Holderの処理
        //　引　数　:　parent ..... ViewGroup
        //　　　　　:　viewType ..... int
        //　戻り値　:　[RowViewHolder] ..... 行ViewHolder
        //=====================================
        @Override
        public RowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 1行分のコンテナを横並びLinearLayoutで作る
            LinearLayout row = new LinearLayout(DbTestActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(rowHeightDp)
            ));

            // セル参照を保持して、bind時にまとめて更新できるようにする
            List<TextView> cells = new ArrayList<>();

            // 先頭：行番号セル
            TextView rowHeader = buildRowHeader();
            row.addView(rowHeader);
            cells.add(rowHeader);

            // 以降：列数(viewType)分のセルを生成
            for (int i = 0; i < viewType; i++) {
                TextView cell = buildCell();
                row.addView(cell);
                cells.add(cell);
            }

            return new RowViewHolder(row, cells);
        }

        //=======================================
        //　機　能　:　on Bind View Holderの処理
        //　引　数　:　holder ..... RowViewHolder
        //　　　　　:　position ..... int
        //　戻り値　:　[void] ..... なし
        //=======================================
        @Override
        public void onBindViewHolder(RowViewHolder holder, int position) {
            List<TextView> cells = holder.cells;
            if (cells.isEmpty()) return;

            // 先頭セル：行番号（1始まり）
            TextView rowHeader = cells.get(0);
            rowHeader.setText(String.valueOf(position + 1));
            applyCellLayoutParams(rowHeader, dp(rowHeaderWidthDp), dp(rowHeightDp), 0);

            // データ行を取り出し、各セルに設定
            List<String> row = currentRows.get(position);
            for (int i = 0; i < currentColumns.size(); i++) {
                int cellIndex = i + 1;     // cells[0]は行番号のため+1
                if (cellIndex >= cells.size()) break;

                TextView tv = cells.get(cellIndex);

                // 行データの範囲外なら空文字
                String value = (i < row.size()) ? row.get(i) : "";

                // 改行除去して表示
                tv.setText(norm(value));

                // 列幅適用（罫線重なり用に左マージン-2px）
                applyCellLayoutParams(tv, getColWidthPx(i), dp(rowHeightDp), -dp(2));
            }
        }

        //============================
        //　機　能　:　item Countを取得する
        //　引　数　:　なし
        //　戻り値　:　[int] ..... 行数
        //============================
        @Override
        public int getItemCount() {
            return currentRows.size();
        }

        //================================
        //　機　能　:　row Headerを生成する
        //　引　数　:　なし
        //　戻り値　:　[TextView] ..... 行番号セル
        //================================
        private TextView buildRowHeader() {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setTextColor(0xFF000000);
            tv.setBackgroundResource(R.drawable.bg_table_cell);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));
            return tv;
        }

        //================================
        //　機　能　:　cellを生成する
        //　引　数　:　なし
        //　戻り値　:　[TextView] ..... データセル
        //================================
        private TextView buildCell() {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setTextColor(0xFF000000);
            tv.setBackgroundResource(R.drawable.bg_table_cell);

            // 1行表示（長い場合は横に伸びる前提）
            tv.setSingleLine(true);
            tv.setEllipsize(null);

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));
            return tv;
        }

        // 行ViewHolder（セル配列を保持してbindを高速化）
        class RowViewHolder extends RecyclerView.ViewHolder {
            final List<TextView> cells;

            RowViewHolder(View itemView, List<TextView> cells) {
                super(itemView);
                this.cells = cells;
            }
        }
    }
}
