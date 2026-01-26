package com.example.myapplication.activity;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbTestActivity extends BaseActivity {

    private Spinner spTables;
    private TableView tableView;

    private AppDatabase roomDb;
    private ExecutorService executor;

    private MyTableAdapter tableAdapter;
    private volatile boolean isAlive = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_test);

        isAlive = true;

        spTables = findViewById(R.id.spContainerSize);
        tableView = findViewById(R.id.tableView);

        if (spTables == null) throw new IllegalStateException("View is null: spContainerSize");
        if (tableView == null) throw new IllegalStateException("View is null: tableView");

        tableAdapter = new MyTableAdapter();
        tableView.setAdapter(tableAdapter);

        roomDb = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        loadTableNames();

        spTables.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTableData((String) parent.getItemAtPosition(position));
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

                ArrayAdapter<String> ad = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, list);
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTables.setAdapter(ad);
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
            });
        });
    }

    // =============================================================================================
    // TableView Adapter
    // =============================================================================================

    private static class Col {
        final String v;

        Col(String v) {
            this.v = v;
        }
    }

    private static class Row {
        final String v;

        Row(String v) {
            this.v = v;
        }
    }

    private static class Cell {
        final String v;

        Cell(String v) {
            this.v = v;
        }
    }

    private class MyTableAdapter extends AbstractTableAdapter<Col, Row, Cell> {

        // 見た目
        private final int headerColor = 0xFFC5B8FF;
        private final int rowHeaderWidthDp = 48;
        private final int textSp = 11;

        // 列幅（px）自動調整
        private final List<Integer> colWidthsPx = new ArrayList<>();

        // データ保持（全文表示 & 列幅計算）
        private final List<String> currentColumns = new ArrayList<>();
        private final List<List<String>> currentRows = new ArrayList<>();

        private int dp(int v) {
            return (int) (v * getResources().getDisplayMetrics().density);
        }

        private String norm(String s) {
            if (s == null) return "";
            return s.replace("\n", " ").replace("\r", " ");
        }

        // ---------------- Column Header ----------------

        private class HeaderVH extends AbstractViewHolder {
            final FrameLayout root;
            final TextView tv;

            HeaderVH(FrameLayout r, TextView t) {
                super(r);
                root = r;
                tv = t;
            }
        }

        @Override
        public AbstractViewHolder onCreateColumnHeaderViewHolder(ViewGroup parent, int viewType) {
            FrameLayout root = new FrameLayout(DbTestActivity.this);
            root.setBackgroundColor(headerColor);
            root.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tv = new TextView(DbTestActivity.this);
            FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            tvLp.gravity = Gravity.CENTER;
            tv.setLayoutParams(tvLp);

            tv.setTextColor(0xFF000000);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);

            // 列幅を広げるので省略は不要
            tv.setSingleLine(true);
            tv.setEllipsize(null);

            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));

            root.addView(tv);
            return new HeaderVH(root, tv);
        }

        @Override
        public void onBindColumnHeaderViewHolder(AbstractViewHolder holder, Col columnHeader, int columnPosition) {
            HeaderVH h = (HeaderVH) holder;
            h.tv.setText(columnHeader.v);

            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) h.root.getLayoutParams();
            lp.width = getColWidthPx(columnPosition);
            h.root.setLayoutParams(lp);
        }

        // ---------------- Row Header ----------------

        @Override
        public AbstractViewHolder onCreateRowHeaderViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setTextColor(0xFF000000);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));

            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    dp(rowHeaderWidthDp),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new AbstractViewHolder(tv) {
            };
        }

        @Override
        public void onBindRowHeaderViewHolder(AbstractViewHolder holder, Row rowHeader, int rowPosition) {
            TextView tv = (TextView) holder.itemView;
            tv.setText(rowHeader.v);
        }

        // ---------------- Cell ----------------

        @Override
        public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setTextColor(0xFF000000);

            // 列幅を広げるので省略は不要
            tv.setSingleLine(true);
            tv.setEllipsize(null);

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));

            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new AbstractViewHolder(tv) {
            };
        }

        @Override
        public void onBindCellViewHolder(AbstractViewHolder holder, Cell cell, int columnPosition, int rowPosition) {
            TextView tv = (TextView) holder.itemView;

            tv.setText(norm(cell.v));

            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) tv.getLayoutParams();
            lp.width = getColWidthPx(columnPosition);
            tv.setLayoutParams(lp);
        }

        // ---------------- Corner ----------------

        @Override
        public View onCreateCornerView(ViewGroup parent) {
            FrameLayout root = new FrameLayout(DbTestActivity.this);
            root.setBackgroundColor(headerColor);
            root.setLayoutParams(new RecyclerView.LayoutParams(
                    dp(rowHeaderWidthDp),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return root;
        }

        @Override
        public int getColumnHeaderItemViewType(int columnPosition) {
            return 0;
        }

        @Override
        public int getRowHeaderItemViewType(int rowPosition) {
            return 0;
        }

        @Override
        public int getCellItemViewType(int columnPosition) {
            return 0;
        }

        // ---------------- Data ----------------

        void showFullTextDialogIfNeeded(int rowPosition, int columnPosition) {
            if (rowPosition < 0 || columnPosition < 0) return;
            if (rowPosition >= currentRows.size()) return;
            List<String> row = currentRows.get(rowPosition);
            if (columnPosition >= row.size()) return;

            String value = row.get(columnPosition);
            if (value == null) value = "";

            if (value.length() <= 12) return;

            String title = (columnPosition < currentColumns.size()) ? currentColumns.get(columnPosition) : "Cell";

            new AlertDialog.Builder(DbTestActivity.this)
                    .setTitle(title + " (Row " + (rowPosition + 1) + ")")
                    .setMessage(value)
                    .setPositiveButton("OK", null)
                    .show();
        }

        void setTable(List<String> cols, List<List<String>> data) {
            currentColumns.clear();
            currentRows.clear();
            currentColumns.addAll(cols);
            currentRows.addAll(data);

            autoAdjustColumnWidths();

            List<Col> ch = new ArrayList<>();
            for (String s : currentColumns) ch.add(new Col(s));

            List<Row> rh = new ArrayList<>();
            for (int i = 0; i < currentRows.size(); i++) rh.add(new Row(String.valueOf(i + 1)));

            List<List<Cell>> ci = new ArrayList<>();
            for (List<String> r : currentRows) {
                List<Cell> one = new ArrayList<>();
                for (int i = 0; i < currentColumns.size(); i++) {
                    String v = (i < r.size()) ? r.get(i) : "";
                    one.add(new Cell(v));
                }
                ci.add(one);
            }

            setAllItems(ch, rh, ci);
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

        private int getColWidthPx(int columnPosition) {
            if (columnPosition >= 0 && columnPosition < colWidthsPx.size()) {
                return colWidthsPx.get(columnPosition);
            }
            return dp(120);
        }
    }
}
