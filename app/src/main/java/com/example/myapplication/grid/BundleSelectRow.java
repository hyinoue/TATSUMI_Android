package com.example.myapplication.grid;

//============================================================
//　処理概要　:　一覧表示データの制御を行うクラス
//　関　　数　:　BundleSelectRow ..... Bundle選択一覧の表示行データ（PNo/BNo/Index/重量/取消）
//============================================================
public class BundleSelectRow {

    public String pNo; // PNo
    public String bNo; // BNo

    public String index; // 行番号

    public String jyuryo; // 重量
    public String cancelText; // 「削除」表示

    //============================================================
    //　機　能　:　BundleSelectRowの初期化処理
    //　引　数　:　pNo ..... PNo
    //　　　　　:　bNo ..... BNo
    //　　　　　:　index ..... 位置番号
    //　　　　　:　jyuryo ..... 重量
    //　　　　　:　cancelText ..... テキスト
    //　戻り値　:　[BundleSelectRow] ..... なし
    //============================================================
    public BundleSelectRow(String pNo, String bNo, String index, String jyuryo, String cancelText) {

        // 引数をそのままメンバへ格納（表示用モデルなので変換は呼び出し側で実施）
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.cancelText = cancelText;
    }
}
