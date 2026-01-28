package com.example.myapplication.grid;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * TableView用の一式（モデル/ビルダー/アダプタ/リスナー）を1ファイルに集約。
 * <p>
 * ※あなたのTableViewライブラリのAPIに合わせた版：
 * - AbstractTableAdapter は super()（引数なし）
 * - onCreateCornerView は LayoutParams を返す
 * - ITableViewListener の引数は RecyclerView.ViewHolder 版
 */
public final class BundleTableViewKit {

    private BundleTableViewKit() {
    }

    // 列インデックス（固定：5列）
    public static final int COL_PNO = 0;
    public static final int COL_BNO = 1;
    public static final int COL_INDEX = 2;
    public static final int COL_JYURYO = 3;
    public static final int COL_DEL = 4;

    // ====== 使い方（Activity側） ======
    // binder = new BundleTableViewKit.Binder(this, tableView, controller);
    // binder.bind();
    // binder.refresh();
    // ================================

    public static final class Binder {
        public interface DeleteHandler {
            void delete(int row);
        }

        private final Context context;
        private final TableView tableView;
        private final BundleSelectController controller;
        private final BundleTableAdapter adapter;
        @Nullable
        private final Runnable onRefreshed;
        @Nullable
        private final DeleteHandler deleteHandler;

        public Binder(@NonNull Context context,
                      @NonNull TableView tableView,
                      @NonNull BundleSelectController controller) {
            this(context, tableView, controller, null, null);
        }

        public Binder(@NonNull Context context,
                      @NonNull TableView tableView,
                      @NonNull BundleSelectController controller,
                      @Nullable Runnable onRefreshed) {
            this(context, tableView, controller, onRefreshed, null);
        }

        public Binder(@NonNull Context context,
                      @NonNull TableView tableView,
                      @NonNull BundleSelectController controller,
                      @Nullable Runnable onRefreshed,
                      @Nullable DeleteHandler deleteHandler) {
            this.context = context;
            this.tableView = tableView;
            this.controller = controller;
            this.adapter = new BundleTableAdapter(context, row -> showDeleteConfirm(row));
            this.onRefreshed = onRefreshed;
            this.deleteHandler = deleteHandler;
        }

        public void bind() {
            tableView.setAdapter(adapter);
            tableView.setRowHeaderWidth(0);

            refresh();
        }

        public void refresh() {
            List<BundleGridRow> rows = controller.getDisplayRows();
            TableData data = TableData.build(rows);
            adapter.setAllItems(data.columnHeaders, data.rowHeaders, data.cells);
            if (onRefreshed != null) {
                onRefreshed.run();
            }
        }

        private void showDeleteConfirm(int row) {
            new AlertDialog.Builder(context)
                    .setMessage("行を削除します。よろしいですか？")
                    .setPositiveButton("はい", (d, w) -> {
                        if (deleteHandler != null) {
                            deleteHandler.delete(row);
                        } else {
                            tableView.post(() -> {
                                controller.removeBundle(row);
                                refresh();
                            });
                        }
                    })
                    .setNegativeButton("いいえ", null)
                    .show();
        }
    }

    // =========================
    //  内部モデル（最小）
    // =========================
    static final class Cell {
        final String id;
        final String text;

        Cell(String id, @Nullable String text) {
            this.id = id;
            this.text = (text == null) ? "" : text;
        }
    }

    static final class ColumnHeader {
        final String id;
        final String title;

        ColumnHeader(String id, @Nullable String title) {
            this.id = id;
            this.title = (title == null) ? "" : title;
        }
    }

    static final class RowHeader {
        final String id;
        final String title;

        RowHeader(String id, @Nullable String title) {
            this.id = id;
            this.title = (title == null) ? "" : title;
        }
    }

    static final class TableData {
        final List<ColumnHeader> columnHeaders;
        final List<RowHeader> rowHeaders;
        final List<List<Cell>> cells;

        TableData(List<ColumnHeader> columnHeaders,
                  List<RowHeader> rowHeaders,
                  List<List<Cell>> cells) {
            this.columnHeaders = columnHeaders;
            this.rowHeaders = rowHeaders;
            this.cells = cells;
        }

        static TableData build(@NonNull List<BundleGridRow> rows) {
            List<ColumnHeader> col = new ArrayList<>();
            col.add(new ColumnHeader("c0", "P No"));
            col.add(new ColumnHeader("c1", "B No"));
            col.add(new ColumnHeader("c2", "Index"));
            col.add(new ColumnHeader("c3", "重量"));
            col.add(new ColumnHeader("c4", "")); // 削除列ヘッダは空

            List<RowHeader> rowHeaders = new ArrayList<>();
            List<List<Cell>> cells = new ArrayList<>();

            for (int r = 0; r < rows.size(); r++) {
                BundleGridRow row = rows.get(r);
                rowHeaders.add(new RowHeader("r" + r, ""));

                List<Cell> one = new ArrayList<>();
                one.add(new Cell("r" + r + "_c0", row.pNo));
                one.add(new Cell("r" + r + "_c1", row.bNo));
                one.add(new Cell("r" + r + "_c2", row.index));
                one.add(new Cell("r" + r + "_c3", row.jyuryo));
                one.add(new Cell("r" + r + "_c4", row.cancelText)); // "削除"
                cells.add(one);
            }

            return new TableData(col, rowHeaders, cells);
        }
    }

    // =========================
    //  Adapter（列幅/表示）
    // =========================
    static final class BundleTableAdapter extends AbstractTableAdapter<ColumnHeader, RowHeader, Cell> {

        interface DeleteClickHandler {
            void onDelete(int rowIndex);
        }

        private final Context context;
        private final int[] columnWidthsPx;
        @Nullable
        private final DeleteClickHandler deleteClickHandler;
        private static final float[] COLUMN_WEIGHTS = {
                0.26f, // P No
                0.18f, // B No
                0.20f, // Index
                0.18f, // 重量
                0.18f  // 削除
        };

        BundleTableAdapter(@NonNull Context context, @Nullable DeleteClickHandler deleteClickHandler) {
            super();                 // ★あなたの環境では引数なし
            this.context = context;
            this.columnWidthsPx = calculateColumnWidthsPx();
            this.deleteClickHandler = deleteClickHandler;
        }

        static final class HeaderVH extends AbstractViewHolder {
            final TextView tv;

            HeaderVH(TextView v) {
                super(v);
                tv = v;
            }
        }

        static final class CellVH extends AbstractViewHolder {
            final TextView tv;

            CellVH(TextView v) {
                super(v);
                tv = v;
            }
        }

        static final class RowHeaderVH extends AbstractViewHolder {
            final TextView tv;

            RowHeaderVH(TextView v) {
                super(v);
                tv = v;
            }
        }

        @NonNull
        @Override
        public AbstractViewHolder onCreateCellViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(32)
            ));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setPadding(dp(8), dp(4), dp(8), dp(4));
            tv.setSingleLine(true);
            tv.setGravity(Gravity.CENTER);
            return new CellVH(tv);
        }

        @NonNull
        @Override
        public AbstractViewHolder onCreateColumnHeaderViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(32)
            ));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(8), dp(4), dp(8), dp(4));
            tv.setSingleLine(true);
            return new HeaderVH(tv);
        }

        @NonNull
        @Override
        public AbstractViewHolder onCreateRowHeaderViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(0, dp(32)));
            tv.setText("");
            return new RowHeaderVH(tv);
        }

        // ★あなたの環境：cornerは LayoutParams を返す版
        @Override
        public android.view.View onCreateCornerView(@NonNull ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(0, dp(32))); // 行ヘッダ非表示運用なので幅0
            tv.setText("");
            return tv;
        }


        // ★Cell は Object ではなく C(Cell) で override
        @Override
        public void onBindCellViewHolder(@NonNull AbstractViewHolder holder, @NonNull Cell value, int xPosition, int yPosition) {
            CellVH vh = (CellVH) holder;
            vh.tv.setText(value.text);
            applyColumnWidth(vh.tv, xPosition);

            if (xPosition == COL_JYURYO) {
                vh.tv.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                vh.tv.setTypeface(Typeface.DEFAULT);
            } else if (xPosition == COL_DEL) {
                vh.tv.setGravity(Gravity.CENTER);
                vh.tv.setTypeface(Typeface.DEFAULT_BOLD);
                vh.tv.setOnClickListener(v -> {
                    if (deleteClickHandler != null) {
                        deleteClickHandler.onDelete(yPosition);
                    }
                });
            } else {
                vh.tv.setGravity(Gravity.CENTER);
                vh.tv.setTypeface(Typeface.DEFAULT);
            }
            if (xPosition != COL_DEL) {
                vh.tv.setOnClickListener(null);
            }
        }

        // ★ColumnHeader は Object ではなく CH(ColumnHeader) で override
        @Override
        public void onBindColumnHeaderViewHolder(@NonNull AbstractViewHolder holder, @NonNull ColumnHeader value, int xPosition) {
            HeaderVH vh = (HeaderVH) holder;
            vh.tv.setText(value.title);
            applyColumnWidth(vh.tv, xPosition);
        }

        @Override
        public void onBindRowHeaderViewHolder(@NonNull AbstractViewHolder holder, @NonNull RowHeader value, int yPosition) {
            // 行ヘッダ非表示運用
        }

        @Override
        public int getColumnHeaderItemViewType(int position) {
            return 0;
        }

        @Override
        public int getRowHeaderItemViewType(int position) {
            return 0;
        }

        @Override
        public int getCellItemViewType(int column) {
            return 0;
        }

        private int dp(int dp) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    context.getResources().getDisplayMetrics()
            );
        }

        private void applyColumnWidth(@NonNull TextView tv, int xPosition) {
            ViewGroup.LayoutParams params = tv.getLayoutParams();
            params.width = getColumnWidthPx(xPosition);
            tv.setLayoutParams(params);
        }

        private int getColumnWidthPx(int columnPosition) {
            if (columnPosition >= 0 && columnPosition < columnWidthsPx.length) {
                return columnWidthsPx[columnPosition];
            }
            return columnWidthsPx[0];
        }

        private int[] calculateColumnWidthsPx() {
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int[] widths = new int[COLUMN_WEIGHTS.length];
            int assigned = 0;

            for (int i = 0; i < COLUMN_WEIGHTS.length; i++) {
                widths[i] = Math.round(screenWidth * COLUMN_WEIGHTS[i]);
                assigned += widths[i];
            }

            int diff = screenWidth - assigned;
            if (diff != 0 && widths.length > 0) {
                widths[0] += diff;
            }

            return widths;
        }
    }

}
