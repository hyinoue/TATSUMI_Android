package com.example.myapplication.connector;

//============================================================
//　処理概要　:　SoapFaultExceptionクラス
//　関　　数　:　SoapFaultException ....................... SOAP Fault例外生成（メッセージ＋生XML保持）
//　　　　　　:　getRawXml ............................... 生XML取得
//============================================================
public class SoapFaultException extends Exception {

    // SOAP Fault時の生レスポンスXML（調査用）
    private final String rawXml;

    //================================================================
    //　機　能　:　SoapFaultExceptionを生成する
    //　引　数　:　message ..... String（例外メッセージ）
    //　　　　　:　rawXml ..... String（SOAPレスポンス生XML）
    //　戻り値　:　[SoapFaultException] ..... なし
    //================================================================
    public SoapFaultException(String message, String rawXml) {
        // 親クラス（Exception）へメッセージを渡す
        super(message);

        // SOAP Fault解析・デバッグ用に生XMLを保持
        this.rawXml = rawXml;
    }

    //================================================================
    //　機　能　:　SOAPレスポンス生XMLを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAPレスポンス生XML
    //================================================================
    public String getRawXml() {
        return rawXml;
    }
}
