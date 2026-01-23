package com.example.myapplication.grid;

/**
 * C# の DataTable 1行分（P No / B No / Index / 重量 / 取消）に相当
 * RecyclerView の Adapter はこれを表示すればOK
 */
public class BundleGridRow {
    public String pNo;
    public String bNo;
    public String index;
    public String jyuryo;
    public String cancelText; // "削除"

    public BundleGridRow(String pNo, String bNo, String index, String jyuryo, String cancelText) {
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.cancelText = cancelText;
    }
}
