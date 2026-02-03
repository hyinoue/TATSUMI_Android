package com.example.myapplication.connector;

//===================================
//　処理概要　:　SoapFaultExceptionクラス
//===================================

public class SoapFaultException extends Exception {
    private final String rawXml;
    //==========================================
    //　機　能　:　SoapFaultExceptionの初期化処理
    //　引　数　:　message ..... String
    //　　　　　:　rawXml ..... String
    //　戻り値　:　[SoapFaultException] ..... なし
    //==========================================


    public SoapFaultException(String message, String rawXml) {
        super(message);
        this.rawXml = rawXml;
    }
    //==============================
    //　機　能　:　raw Xmlを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //==============================

    public String getRawXml() {
        return rawXml;
    }
}

