package com.example.myapplication.connector;

//============================================================
//　処理概要　:　SoapFaultExceptionクラス
//============================================================

public class SoapFaultException extends Exception {
    private final String rawXml;


    public SoapFaultException(String message, String rawXml) {
        super(message);
        this.rawXml = rawXml;
    }

    public String getRawXml() {
        return rawXml;
    }
}

