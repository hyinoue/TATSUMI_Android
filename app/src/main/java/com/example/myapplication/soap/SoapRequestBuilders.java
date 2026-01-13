package com.example.myapplication.soap;

import java.util.Date;

public class SoapRequestBuilders {
    private static final String NS = "http://tempuri.org/";

    private SoapRequestBuilders() {
    }

    // GetSysDate()
    public static String buildGetSysDate() {
        String body = "<GetSysDate xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    // GetSagyouYmd()
    public static String buildGetSagyouYmd() {
        String body = "<GetSagyouYmd xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    // GetUpdateYmdHms(DateTime sagyouYmd)
    public static String buildGetUpdateYmdHms(Date sagyouYmd) {
        StringBuilder inner = new StringBuilder();
        inner.append("<GetUpdateYmdHms xmlns=\"").append(NS).append("\">");
        XmlUtil.tag(inner, "sagyouYmd", XmlUtil.toXsdDateTime(sagyouYmd)); // 引数名は reference.cs のメソッド引数名
        inner.append("</GetUpdateYmdHms>");
        return SoapEnvelope.wrapBody(inner.toString());
    }

    // GetSyukkaData(DateTime sagyouYmd)
    public static String buildGetSyukkaData(Date sagyouYmd) {
        StringBuilder inner = new StringBuilder();
        inner.append("<GetSyukkaData xmlns=\"").append(NS).append("\">");
        XmlUtil.tag(inner, "sagyouYmd", XmlUtil.toXsdDateTime(sagyouYmd)); // 引数名 sagyouYmd
        inner.append("</GetSyukkaData>");
        return SoapEnvelope.wrapBody(inner.toString());
    }

    // GetSyougoData()
    public static String buildGetSyougoData() {
        String body = "<GetSyougoData xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }
}

