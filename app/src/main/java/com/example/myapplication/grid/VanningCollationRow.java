package com.example.myapplication.grid;

//============================================================
//　処理概要　:　共通関数
//　関　　数　:　VanningCollationRow ..... バンニング照合一覧の表示行データ
//============================================================
public class VanningCollationRow {
    public final String pNo; // PNo
    public final String bNo; // BNo
    // 束番（Index）
    public final String index; // 行番号

    // 重量（3桁区切り＋左スペース埋め済み表示用文字列）
    public final String jyuryo; // 重量

    // 照合状態表示（"済" または 空白）
    public final String confirmed; // 確定状態

    //===========================================
    //　機　能　:　VanningCollationRowの初期化処理
    //　引　数　:　pNo ..... String
    //　　　　　:　bNo ..... String
    //　　　　　:　index ..... String
    //　　　　　:　jyuryo ..... String
    //　　　　　:　confirmed ..... String
    //　戻り値　:　[VanningCollationRow] ..... なし
    //===========================================
    public VanningCollationRow(String pNo,
                               String bNo,
                               String index,
                               String jyuryo,
                               String confirmed) {

        // 引数値をそのままメンバへ設定
        // ※ 表示専用モデルのため、値の整形はController側で実施する前提
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.confirmed = confirmed;
    }
}
