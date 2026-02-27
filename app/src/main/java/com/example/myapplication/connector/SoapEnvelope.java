package com.example.myapplication.connector;

//============================================================
//　処理概要　:　SOAP Envelope（Header/Body）文字列を生成するユーティリティクラス
//　関　　数　:　wrapBody ................................. SOAP Envelope生成
//============================================================
public class SoapEnvelope {

    //============================================================
    //　機　能　:　SOAP Body文字列をEnvelopeでラップする
    //　引　数　:　innerBodyXml ..... XML文字列
    //　戻り値　:　[String] ..... SOAP Envelope全体のXML文字列
    //============================================================
    public static String wrapBody(String innerBodyXml) {

        // XML宣言 + SOAP Envelope + SOAP Body で包む
        // 名前空間は ASMX（SOAP 1.1）用
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"

                // SOAP Envelope開始（必要な名前空間定義を含む）
                + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"

                // SOAP Body開始
                + "<soap:Body>"

                // 呼び出しメソッドXML（呼び出し側で組み立てた部分）
                + innerBodyXml

                // SOAP Body終了
                + "</soap:Body>"

                // SOAP Envelope終了
                + "</soap:Envelope>";
    }
}
