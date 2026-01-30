package com.example.myapplication.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
//　処理概要　:　DbTestActivityクラス
//============================================================

public class DbTestActivity extends BaseActivity {

    private Spinner spTables;
    private LinearLayout headerRow;
    private RecyclerView rvDbTable;

    private AppDatabase roomDb;
    private ExecutorService executor;

    private DbTableAdapter tableAdapter;
    private volatile boolean isAlive = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_test);

        isAlive = true;

        spTables = findViewById(R.id.spContainerSize);
        headerRow = findViewById(R.id.rowTableHeader);
        rvDbTable = findViewById(R.id.rvDbTable);

        if (spTables == null) throw new IllegalStateException("View is null: spContainerSize");
        if (headerRow == null) throw new IllegalStateException("View is null: rowTableHeader");
        if (rvDbTable == null) throw new IllegalStateException("View is null: rvDbTable");

        tableAdapter = new DbTableAdapter();
        rvDbTable.setLayoutManager(new LinearLayoutManager(this));
        rvDbTable.setAdapter(tableAdapter);
        rvDbTable.setHorizontalScrollBarEnabled(true);

        setupBottomButtons();

        roomDb = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        loadTableNames();

        spTables.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String table = (String) parent.getItemAtPosition(position);
                if (table == null || table.trim().isEmpty()) {
                    tableAdapter.setTable(new ArrayList<>(), new ArrayList<>());
                    updateHeaderRow(new ArrayList<>(), tableAdapter.getColumnWidthsPx(),
                            tableAdapter.getRowHeaderWidthPx(), tableAdapter.getRowHeightPx());
                    return;
                }
                loadTableData(table);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        isAlive = false;
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    // =============================================================================================
    // DB
    // =============================================================================================

    private void loadTableNames() {
        executor.execute(() -> {
            List<String> list = new ArrayList<>();
            SupportSQLiteDatabase db = roomDb.getOpenHelper().getReadableDatabase();

            try (Cursor c = db.query(
                    "SELECT name FROM sqlite_master " +
                            "WHERE type='table' " +
                            "AND name NOT LIKE 'sqlite_%' " +
                            "ORDER BY name ASC"
            )) {
                while (c.moveToNext()) list.add(c.getString(0));
            }

            runOnUiThread(() -> {
                if (!isAlive || isFinishing() || isDestroyed()) return;

                List<String> displayList = new ArrayList<>();
                displayList.add("");
                displayList.addAll(list);
                ArrayAdapter<String> ad = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, displayList);
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTables.setAdapter(ad);
                spTables.setSelection(0);
            });
        });
    }

    private void loadTableData(String table) {
        executor.execute(() -> {
            List<String> cols = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            SupportSQLiteDatabase db = roomDb.getOpenHelper().getReadableDatabase();
            try (Cursor c = db.query("SELECT * FROM " + table)) {
                for (String s : c.getColumnNames()) cols.add(s);

                while (c.moveToNext()) {
                    List<String> r = new ArrayList<>();
                    for (int i = 0; i < cols.size(); i++) {
                        r.add(c.isNull(i) ? "NULL" : c.getString(i));
                    }
                    rows.add(r);
                }
            }

            runOnUiThread(() -> {
                if (!isAlive || isFinishing() || isDestroyed()) return;
                tableAdapter.setTable(cols, rows);
                updateHeaderRow(cols, tableAdapter.getColumnWidthsPx(), tableAdapter.getRowHeaderWidthPx(), tableAdapter.getRowHeightPx());
            });
        });
    }

    // =============================================================================================
    // TableView Adapter
    // =============================================================================================

    private void updateHeaderRow(List<String> columns, List<Integer> widthsPx, int rowHeaderWidthPx, int rowHeightPx) {
        headerRow.removeAllViews();
        TextView rowHeader = buildHeaderCell("", rowHeaderWidthPx, rowHeightPx);
        headerRow.addView(rowHeader);
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            int widthPx = (i < widthsPx.size()) ? widthsPx.get(i) : dp(120);
            TextView tv = buildHeaderCell(column, widthPx, rowHeightPx);
            headerRow.addView(tv);
        }
    }

    private TextView buildHeaderCell(String label, int widthPx, int heightPx) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF000000);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.bg_menu_count);
        tv.setPadding(dp(6), dp(4), dp(6), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, heightPx);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void setupBottomButtons() {
        bindBottomButtonsIfExists();
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        if (yellow != null) {
            yellow.setText("戻る");
        }
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
    }

    @Override
    protected void onFunctionYellow() {
        finish();
    }

    private class DbTableAdapter extends RecyclerView.Adapter<DbTableAdapter.RowViewHolder> {

        // 見た目
        private final int rowHeaderWidthDp = 48;
        private final int rowHeightDp = 28;
        private final int textSp = 15;

        // 列幅（px）自動調整
        private final List<Integer> colWidthsPx = new ArrayList<>();

        // データ保持（全文表示 & 列幅計算）
        private final List<String> currentColumns = new ArrayList<>();
        private final List<List<String>> currentRows = new ArrayList<>();

        private String norm(String s) {
            if (s == null) return "";
            return s.replace("\n", " ").replace("\r", " ");
        }

        @Override
        public int getItemViewType(int position) {
            return currentColumns.size();
        }

        void setTable(List<String> cols, List<List<String>> data) {
            currentColumns.clear();
            currentRows.clear();
            currentColumns.addAll(cols);
            currentRows.addAll(data);

            autoAdjustColumnWidths();
            notifyDataSetChanged();
        }

        /**
         * 列幅を「ヘッダ（太字） + セル（通常）」の最大文字幅に合わせて決める（上限なし）
         * ★最後の1文字見切れ対策で safety を少し足す
         */
        private void autoAdjustColumnWidths() {
            colWidthsPx.clear();

            TextView headerMeasure = new TextView(DbTestActivity.this);
            headerMeasure.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            headerMeasure.setTypeface(headerMeasure.getTypeface(), android.graphics.Typeface.BOLD);

            TextView cellMeasure = new TextView(DbTestActivity.this);
            cellMeasure.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);

            int pad = dp(12);       // 左右padding相当
            int min = dp(60);       // 最小幅
            int safety = dp(4);     // ★見切れ防止の安全マージン（1文字欠け対策）

            for (int c = 0; c < currentColumns.size(); c++) {
                float w = 0f;

                // ヘッダ（太字Paintで計測）
                String header = norm(currentColumns.get(c));
                w = Math.max(w, headerMeasure.getPaint().measureText(header));

                // セル（通常Paintで計測）
                for (List<String> r : currentRows) {
                    if (c >= r.size()) continue;
                    String v = norm(r.get(c));
                    w = Math.max(w, cellMeasure.getPaint().measureText(v));
                }

                int wPx = (int) Math.ceil(w) + pad + safety;
                if (wPx < min) wPx = min;

                colWidthsPx.add(wPx);
            }
        }

        List<Integer> getColumnWidthsPx() {
            return new ArrayList<>(colWidthsPx);
        }

        int getRowHeaderWidthPx() {
            return dp(rowHeaderWidthDp);
        }

        int getRowHeightPx() {
            return dp(rowHeightDp);
        }

        private int getColWidthPx(int columnPosition) {
            if (columnPosition >= 0 && columnPosition < colWidthsPx.size()) {
                return colWidthsPx.get(columnPosition);
            }
            return dp(120);
        }

        @Override
        public RowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(DbTestActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(rowHeightDp)
            ));

            List<TextView> cells = new ArrayList<>();

            TextView rowHeader = buildRowHeader();
            row.addView(rowHeader);
            cells.add(rowHeader);

            for (int i = 0; i < viewType; i++) {
                TextView cell = buildCell();
                row.addView(cell);
                cells.add(cell);
            }
            return new RowViewHolder(row, cells);
        }

        @Override
        public void onBindViewHolder(RowViewHolder holder, int position) {
            List<TextView> cells = holder.cells;
            if (cells.isEmpty()) return;

            TextView rowHeader = cells.get(0);
            rowHeader.setText(String.valueOf(position + 1));
            rowHeader.setLayoutParams(new LinearLayout.LayoutParams(dp(rowHeaderWidthDp), dp(rowHeightDp)));

            List<String> row = currentRows.get(position);
            for (int i = 0; i < currentColumns.size(); i++) {
                int cellIndex = i + 1;
                if (cellIndex >= cells.size()) break;
                TextView tv = cells.get(cellIndex);
                String value = (i < row.size()) ? row.get(i) : "";
                tv.setText(norm(value));
                tv.setLayoutParams(new LinearLayout.LayoutParams(getColWidthPx(i), dp(rowHeightDp)));
            }
        }

        @Override
        public int getItemCount() {
            return currentRows.size();
        }

        private TextView buildRowHeader() {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setTextColor(0xFF000000);
            tv.setBackgroundResource(R.drawable.bg_table_row);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));
            tv.setLayoutParams(new LinearLayout.LayoutParams(dp(rowHeaderWidthDp), dp(rowHeightDp)));
            return tv;
        }

        private TextView buildCell() {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setTextColor(0xFF000000);
            tv.setBackgroundResource(R.drawable.bg_table_row);
            tv.setSingleLine(true);
            tv.setEllipsize(null);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(rowHeightDp)
            ));
            return tv;
        }

        class RowViewHolder extends RecyclerView.ViewHolder {
            final List<TextView> cells;

            RowViewHolder(View itemView, List<TextView> cells) {
                super(itemView);
                this.cells = cells;
            }
        }
    }
}
