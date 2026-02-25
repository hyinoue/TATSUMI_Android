package com.example.myapplication.grid;

//============================================================
//　処理概要　:　共通関数
//　関　　数　:　CollateContainerSelectRow ..... 照合対象コンテナ一覧の表示行データ
//============================================================
public class CollateContainerSelectRow {

    public final String index; // 行番号

    public final String containerNo; // コンテナNo
    public final String bundleCnt; // 束数
    public final String sagyouYmd; // 作業日時

    //===========================================
    //　機　能　:　CollateContainerSelectRowの初期化処理
    //　引　数　:　index ..... String
    //　　　　　:　containerNo ..... String
    //　　　　　:　bundleCnt ..... String
    //　　　　　:　sagyouYmd ..... String
    //　戻り値　:　[CollateContainerSelectRow] ..... なし
    //===========================================
    public CollateContainerSelectRow(String index,
                                     String containerNo,
                                     String bundleCnt,
                                     String sagyouYmd) {

        // 引数値をそのままメンバへ設定
        // ※ 表示用モデルのため、変換・加工はController側で実施する前提
        this.index = index;
        this.containerNo = containerNo;
        this.bundleCnt = bundleCnt;
        this.sagyouYmd = sagyouYmd;
    }
}
