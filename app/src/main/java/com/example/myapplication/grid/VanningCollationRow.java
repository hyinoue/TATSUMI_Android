package com.example.myapplication.grid;

//============================================================
//　処理概要　:　共通関数
//　関　　数　:　VanningCollationRow ..... バンニング照合一覧の表示行データ
//============================================================

//====================================
//　処理概要　:　VanningCollationRowクラス
//====================================
public class VanningCollationRow {

    // 出荷指図No（Packing No）
    public final String pNo;

    // 束No（Bundle No）
    public final String bNo;

    // 側番（Index）
    public final String index;

    // 重量（3桁区切り＋左スペース埋め済み表示用文字列）
    public final String jyuryo;

    // 照合状態表示（"済" または 空白）
    public final String confirmed;

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