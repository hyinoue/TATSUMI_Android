package com.example.myapplication.grid;

//==============================
//　処理概要　:　CollateContainerRowクラス
//==============================

public class CollateContainerRow {
    public final String index;
    public final String containerNo;

    public final String bundleCnt;
    public final String sagyouYmd;
    //=====================================
    //　機　能　:　CollateContainerRowの初期化処理
    //　引　数　:　index ..... String
    //　　　　　:　containerNo ..... String
    //　　　　　:　bundleCnt ..... String
    //　　　　　:　sagyouYmd ..... String
    //　戻り値　:　[CollateContainerRow] ..... なし
    //=====================================

    public CollateContainerRow(String index, String containerNo, String bundleCnt, String sagyouYmd) {
        this.index = index;
        this.containerNo = containerNo;
        this.bundleCnt = bundleCnt;
        this.sagyouYmd = sagyouYmd;
    }
}
