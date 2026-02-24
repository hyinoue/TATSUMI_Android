package com.example.myapplication.grid;

//============================================================
//　処理概要　:　共通関数
//　関　　数　:　BundleSelectRow ..... Bundle選択一覧の表示行データ（PNo/BNo/Index/重量/取消）
//============================================================

//==============================
//　処理概要　:　BundleSelectRowクラス
//==============================
public class BundleSelectRow {

    // 梱包（Packing）No
    public String pNo;

    // 束（Bundle）No
    public String bNo;

    // 行インデックス（側番など）
    public String index;

    // 表示用重量（フォーマット済み文字列）
    public String jyuryo;

    // 取消表示文言（例："削除"）
    public String cancelText;

    //=====================================
    //　機　能　:　BundleSelectRowの初期化処理
    //　引　数　:　pNo ..... String
    //　　　　　:　bNo ..... String
    //　　　　　:　index ..... String
    //　　　　　:　jyuryo ..... String
    //　　　　　:　cancelText ..... String
    //　戻り値　:　[BundleSelectRow] ..... なし
    //=====================================
    public BundleSelectRow(String pNo, String bNo, String index, String jyuryo, String cancelText) {

        // 引数をそのままメンバへ格納（表示用モデルなので変換は呼び出し側で実施）
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.cancelText = cancelText;
    }
}