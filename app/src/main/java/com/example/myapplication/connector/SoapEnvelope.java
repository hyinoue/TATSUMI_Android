package com.example.myapplication.connector;

//=============================
//　処理概要　:　SoapEnvelopeクラス
//=============================

public class SoapEnvelope {
    //======================================
    //　機　能　:　wrap Bodyの処理
    //　引　数　:　innerBodyXml ..... String
    //　戻り値　:　[String] ..... なし
    //======================================
    public static String wrapBody(String innerBodyXml) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"

                + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + innerBodyXml
                + "</soap:Body>"
                + "</soap:Envelope>";
    }
}
