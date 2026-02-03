package com.example.myapplication.grid;

//==============================
//　処理概要　:　VanningCollationRowクラス
//==============================

public class VanningCollationRow {
    public final String pNo;
    public final String bNo;

    public final String index;
    public final String jyuryo;
    public final String confirmed;
    //=====================================
    //　機　能　:　VanningCollationRowの初期化処理
    //　引　数　:　pNo ..... String
    //　　　　　:　bNo ..... String
    //　　　　　:　index ..... String
    //　　　　　:　jyuryo ..... String
    //　　　　　:　confirmed ..... String
    //　戻り値　:　[VanningCollationRow] ..... なし
    //=====================================

    public VanningCollationRow(String pNo, String bNo, String index, String jyuryo, String confirmed) {
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.confirmed = confirmed;
    }
}
