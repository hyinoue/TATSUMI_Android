package com.example.myapplication.grid;

//============================================================
//　処理概要　:　共通関数
//　関　　数　:　CollateContainerSelectRow ..... 照合対象コンテナ一覧の表示行データ
//============================================================
public class CollateContainerSelectRow {

    // 一覧表示用の連番（1始まり）
    public final String index; // 行番号

    // コンテナNo
    public final String containerNo; // コンテナNo

    // 束数（表示用文字列）
    public final String bundleCnt; // 束数

    // 作業日（表示用フォーマット済み文字列）
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
