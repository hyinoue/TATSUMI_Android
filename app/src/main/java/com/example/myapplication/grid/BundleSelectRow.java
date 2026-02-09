package com.example.myapplication.grid;

/**
 * C# の DataTable 1行分（P No / B No / Index / 重量 / 取消）に相当
 * RecyclerView の Adapter はこれを表示すればOK
 */

//==============================
//　処理概要　:　BundleGridRowクラス
//==============================

public class BundleSelectRow {
    public String pNo;
    public String bNo;
    public String index;
    public String jyuryo;
    public String cancelText; // "削除"
    //=====================================
    //　機　能　:　BundleGridRowの初期化処理
    //　引　数　:　pNo ..... String
    //　　　　　:　bNo ..... String
    //　　　　　:　index ..... String
    //　　　　　:　jyuryo ..... String
    //　　　　　:　cancelText ..... String
    //　戻り値　:　[BundleGridRow] ..... なし
    //=====================================

    public BundleSelectRow(String pNo, String bNo, String index, String jyuryo, String cancelText) {
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.cancelText = cancelText;
    }
}
