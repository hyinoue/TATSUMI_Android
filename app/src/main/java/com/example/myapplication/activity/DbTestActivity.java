package com.example.myapplication.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.evrencoskun.tableview.listener.ITableViewListener;
import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbTestActivity extends BaseActivity {

    private static final String TAG = "DbTestActivity";

    private Spinner spTables;
    private TableView tableView;

    private AppDatabase roomDb;
    private ExecutorService executor;

    private MyTableAdapter tableAdapter;

    // 画面遷移時クラッシュ予防（UI更新ガード）
    private volatile boolean isAlive = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_test);

        isAlive = true;

        Log.e(TAG, "DbTestActivity onCreate: " + getClass().getName());

        spTables = findViewById(R.id.spContainerSize);
        tableView = findViewById(R.id.tableView);

        if (spTables == null) throw new IllegalStateException("View is null: spContainerSize");
        if (tableView == null) throw new IllegalStateException("View is null: tableView");

        tableAdapter = new MyTableAdapter();
        tableView.setAdapter(tableAdapter);

        // ★クリックは TableViewListener で取る（安定）
        tableView.setTableViewListener(new ITableViewListener() {

            // -------------------------
            // 必須：Cell click / double click
            // -------------------------
            @Override
            public void onCellClicked(RecyclerView.ViewHolder cellView, int columnPosition, int rowPosition) {
                // 1列目タップ → 行選択（青ハイライトは1列目除外は adapter 側で反映）
                if (columnPosition == 0) {
                    tableAdapter.selectRow(rowPosition);
                } else {
                    tableAdapter.selectCell(rowPosition, columnPosition);
                }
            }

            @Override
            public void onCellDoubleClicked(RecyclerView.ViewHolder cellView, int columnPosition, int rowPosition) {
                // ダブルクリックは同じ動作でOK（不要なら何もしないでも可）
                if (columnPosition == 0) {
                    tableAdapter.selectRow(rowPosition);
                } else {
                    tableAdapter.selectCell(rowPosition, columnPosition);
                }
            }

            @Override
            public void onCellLongPressed(RecyclerView.ViewHolder cellView, int columnPosition, int rowPosition) {
                // 使わないなら空でOK
            }

            // -------------------------
            // Column header
            // -------------------------
            @Override
            public void onColumnHeaderClicked(RecyclerView.ViewHolder columnHeaderView, int columnPosition) {
            }

            @Override
            public void onColumnHeaderDoubleClicked(RecyclerView.ViewHolder columnHeaderView, int columnPosition) {
            }

            @Override
            public void onColumnHeaderLongPressed(RecyclerView.ViewHolder columnHeaderView, int columnPosition) {
            }

            // -------------------------
            // Row header
            // -------------------------
            @Override
            public void onRowHeaderClicked(RecyclerView.ViewHolder rowHeaderView, int rowPosition) {
            }

            @Override
            public void onRowHeaderDoubleClicked(RecyclerView.ViewHolder rowHeaderView, int rowPosition) {
            }

            @Override
            public void onRowHeaderLongPressed(RecyclerView.ViewHolder rowHeaderView, int rowPosition) {
            }
        });

        roomDb = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        loadTableNames();

        spTables.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String tableName = (String) parent.getItemAtPosition(position);
                loadTableData(tableName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        isAlive = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        super.onDestroy();
    }

    private void loadTableNames() {
        executor.execute(() -> {
            List<String> tables;
            try {
                tables = getTableNames(roomDb);
            } catch (Exception e) {
                Log.e(TAG, "getTableNames failed", e);
                tables = new ArrayList<>();
            }

            List<String> finalTables = tables;
            runOnUiThread(() -> {
                if (!isAlive || isFinishing() || isDestroyed()) return;

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        finalTables
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTables.setAdapter(adapter);
            });
        });
    }

    private void loadTableData(String tableName) {
        executor.execute(() -> {
            TableData tableData;
            try {
                tableData = selectAllAsTable(roomDb, tableName);
            } catch (Exception e) {
                Log.e(TAG, "selectAll failed: " + tableName, e);
                tableData = new TableData(new ArrayList<>(), new ArrayList<>());
            }

            TableData finalData = tableData;
            runOnUiThread(() -> {
                if (!isAlive || isFinishing() || isDestroyed()) return;
                tableAdapter.setTable(finalData.columns, finalData.rows);
            });
        });
    }

    private List<String> getTableNames(AppDatabase db) {
        List<String> list = new ArrayList<>();
        SupportSQLiteDatabase sdb = db.getOpenHelper().getReadableDatabase();

        try (Cursor c = sdb.query(
                "SELECT name FROM sqlite_master " +
                        "WHERE type='table' " +
                        "AND name NOT LIKE 'sqlite_%' " +
                        "ORDER BY name"
        )) {
            while (c.moveToNext()) list.add(c.getString(0));
        }
        return list;
    }

    private TableData selectAllAsTable(AppDatabase db, String tableName) {
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        SupportSQLiteDatabase sdb = db.getOpenHelper().getReadableDatabase();

        try (Cursor c = sdb.query("SELECT * FROM " + tableName)) {
            String[] colArray = c.getColumnNames();
            for (String col : colArray) columns.add(col);

            while (c.moveToNext()) {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < colArray.length; i++) {
                    row.add(c.isNull(i) ? "NULL" : c.getString(i));
                }
                rows.add(row);
            }
        }
        return new TableData(columns, rows);
    }

    private static class TableData {
        final List<String> columns;
        final List<List<String>> rows;

        TableData(List<String> columns, List<List<String>> rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    // =============================================================================================
    // TableView adapter（新規ファイルなし）
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

        // 色
        private final int headerColor = 0xFFC5B8FF;   // 列ヘッダ
        private final int firstRowColor = 0xFFC5B8FF; // 1行目
        private final int selectBlue = 0xFF3399FF;    // 選択

        // サイズ
        private final int defaultColWidthDp = 120;
        private final int rowHeaderWidthDp = 48;

        // 高さ（ここで調整）
        private final int headerHeightDp = 22; // 列名
        private final int rowHeightDp = 28;    // セル

        // 列幅（px）
        private final List<Integer> colWidthsPx = new ArrayList<>();

        // 選択状態（行選択：selectedCol=-1、セル選択：>=0）
        private int selectedRow = -1;
        private int selectedCol = -1;

        private class TVH extends AbstractViewHolder {
            final TextView tv;

            TVH(TextView itemView) {
                super(itemView);
                tv = itemView;
            }
        }

        private int dpToPx(int dp) {
            return (int) (dp * getResources().getDisplayMetrics().density);
        }

        private TextView newCellTextView(int minWidthDp, boolean bold) {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setMinWidth(dpToPx(minWidthDp));
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setIncludeFontPadding(false);
            tv.setPadding(dpToPx(6), 0, dpToPx(6), 0);
            tv.setTextColor(0xFF000000);
            tv.setGravity(Gravity.CENTER_VERTICAL);

            int h = dpToPx(rowHeightDp);
            tv.setMinHeight(h);
            tv.setHeight(h);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);

            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return tv;
        }

        private TextView newHeaderTextView(int minWidthDp, boolean bold) {
            TextView tv = new TextView(DbTestActivity.this);
            tv.setMinWidth(dpToPx(minWidthDp));
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setIncludeFontPadding(false);
            tv.setPadding(dpToPx(6), 0, dpToPx(6), 0);
            tv.setTextColor(0xFF000000);
            tv.setGravity(Gravity.CENTER_VERTICAL);

            int h = dpToPx(headerHeightDp);
            tv.setMinHeight(h);
            tv.setHeight(h);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);

            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return tv;
        }

        // Activityから呼ぶ：行選択
        void selectRow(int rowPosition) {
            selectedRow = rowPosition;
            selectedCol = -1;
            notifyDataSetChanged();
        }

        // Activityから呼ぶ：セル選択
        void selectCell(int rowPosition, int columnPosition) {
            selectedRow = rowPosition;
            selectedCol = columnPosition;
            notifyDataSetChanged();
        }

        // Column Header
        @Override
        public AbstractViewHolder onCreateColumnHeaderViewHolder(ViewGroup parent, int viewType) {
            TextView tv = newHeaderTextView(defaultColWidthDp, true);
            tv.setBackgroundColor(headerColor);
            return new TVH(tv);
        }

        @Override
        public void onBindColumnHeaderViewHolder(AbstractViewHolder holder, Col columnHeader, int columnPosition) {
            TextView tv = ((TVH) holder).tv;
            tv.setText(columnHeader.v);

            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            lp.width = getColWidthPx(columnPosition);
            tv.setLayoutParams(lp);
        }

        // Row Header（行番号）
        @Override
        public AbstractViewHolder onCreateRowHeaderViewHolder(ViewGroup parent, int viewType) {
            TextView tv = newCellTextView(rowHeaderWidthDp, true);
            tv.setBackgroundResource(R.drawable.bg_menu_count);
            return new TVH(tv);
        }

        @Override
        public void onBindRowHeaderViewHolder(AbstractViewHolder holder, Row rowHeader, int rowPosition) {
            TextView tv = ((TVH) holder).tv;
            tv.setText(rowHeader.v);

            boolean isRowSelected = (selectedRow == rowPosition && selectedCol == -1);
            if (isRowSelected) tv.setBackgroundColor(selectBlue);
            else tv.setBackgroundResource(R.drawable.bg_menu_count);
        }

        // Cell
        @Override
        public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
            TextView tv = newCellTextView(defaultColWidthDp, false);
            tv.setBackgroundResource(R.drawable.bg_edittext_black_border);
            return new TVH(tv);
        }

        @Override
        public void onBindCellViewHolder(AbstractViewHolder holder, Cell cell, int columnPosition, int rowPosition) {
            TextView tv = ((TVH) holder).tv;
            tv.setText(cell.v);

            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            lp.width = getColWidthPx(columnPosition);
            tv.setLayoutParams(lp);

            boolean isFirstColumn = (columnPosition == 0);

            boolean isCellSelected = (selectedRow == rowPosition && selectedCol == columnPosition);
            boolean isRowSelected = (selectedRow == rowPosition && selectedCol == -1);

            // 要件：
            // ・行選択青：1列目除外
            // ・セル選択青：1列目除外
            if (!isFirstColumn && isCellSelected) {
                tv.setBackgroundColor(selectBlue);
            } else if (!isFirstColumn && isRowSelected) {
                tv.setBackgroundColor(selectBlue);
            } else if (rowPosition == 0) {
                tv.setBackgroundColor(firstRowColor);
            } else {
                tv.setBackgroundResource(R.drawable.bg_edittext_black_border);
            }
        }

        // Corner
        @Override
        public android.view.View onCreateCornerView(ViewGroup parent) {
            TextView tv = newHeaderTextView(rowHeaderWidthDp, false);
            tv.setBackgroundColor(headerColor);
            return tv;
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

        void setTable(List<String> columns, List<List<String>> rows) {
            selectedRow = -1;
            selectedCol = -1;

            colWidthsPx.clear();
            int w = dpToPx(defaultColWidthDp);
            for (int i = 0; i < columns.size(); i++) colWidthsPx.add(w);

            List<Col> colHeaders = new ArrayList<>();
            for (String c : columns) colHeaders.add(new Col(c));

            List<Row> rowHeaders = new ArrayList<>();
            for (int r = 0; r < rows.size(); r++) rowHeaders.add(new Row(String.valueOf(r + 1)));

            List<List<Cell>> cellItems = new ArrayList<>();
            for (List<String> row : rows) {
                List<Cell> one = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    String v = (i < row.size()) ? row.get(i) : "";
                    one.add(new Cell(v));
                }
                cellItems.add(one);
            }

            setAllItems(colHeaders, rowHeaders, cellItems);
        }

        private int getColWidthPx(int columnPosition) {
            if (columnPosition >= 0 && columnPosition < colWidthsPx.size())
                return colWidthsPx.get(columnPosition);
            return dpToPx(defaultColWidthDp);
        }
    }
}
