package com.example.myapplication.connector;

import com.example.myapplication.time.XmlUtil;

import java.util.Date;


//==============================
//　処理概要　:　SoapRequestBuildersクラス
//==============================

public class SoapRequestBuilders {
    private static final String NS = "http://tempuri.org/";
    //=====================================
    //　機　能　:　SoapRequestBuildersの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[SoapRequestBuilders] ..... なし
    //=====================================

    private SoapRequestBuilders() {
    }

    // GetSysDate()
    //========================
    //　機　能　:　get Sys Dateを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //========================
    public static String buildGetSysDate() {
        String body = "<GetSysDate xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    // GetSagyouYmd()
    //==========================
    //　機　能　:　get Sagyou Ymdを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //==========================
    public static String buildGetSagyouYmd() {
        String body = "<GetSagyouYmd xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    // GetUpdateYmdHms(DateTime sagyouYmd)
    //==============================
    //　機　能　:　get Update Ymd Hmsを生成する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[String] ..... なし
    //==============================
    public static String buildGetUpdateYmdHms(Date sagyouYmd) {
        StringBuilder inner = new StringBuilder();
        inner.append("<GetUpdateYmdHms xmlns=\"").append(NS).append("\">");
        XmlUtil.tag(inner, "sagyouYmd", XmlUtil.toXsdDateTime(sagyouYmd)); // 引数名は reference.cs のメソッド引数名
        inner.append("</GetUpdateYmdHms>");
        return SoapEnvelope.wrapBody(inner.toString());
    }

    // GetSyukkaData(DateTime sagyouYmd)
    //===========================
    //　機　能　:　get Syukka Dataを生成する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[String] ..... なし
    //===========================
    public static String buildGetSyukkaData(Date sagyouYmd) {
        StringBuilder inner = new StringBuilder();
        inner.append("<GetSyukkaData xmlns=\"").append(NS).append("\">");
        XmlUtil.tag(inner, "sagyouYmd", XmlUtil.toXsdDateTime(sagyouYmd)); // 引数名 sagyouYmd
        inner.append("</GetSyukkaData>");
        return SoapEnvelope.wrapBody(inner.toString());
    }

    // GetSyougoData()
    //===========================
    //　機　能　:　get Syougo Dataを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //===========================
    public static String buildGetSyougoData() {
        String body = "<GetSyougoData xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }
}

