package com.example.myapplication.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


//============================================================
//　処理概要　:　XmlUtilクラス
//============================================================

public class XmlUtil {
    private XmlUtil() {
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 送信用 xsd:dateTime
     * ★ASMX(C# DateTime) 側がタイムゾーン無し前提で「完全一致」をしているため、
     * ここでは "+09:00" を付けない（yyyy-MM-dd'T'HH:mm:ss）形式で送る。
     * <p>
     * 例: 2020-03-21T00:00:00
     */
    public static String toXsdDateTime(Date d) {
        // タイムゾーンをJST固定（端末設定に依存させない）
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return f.format(d);
    }

    public static void tag(StringBuilder sb, String name, String value) {
        sb.append("<").append(name).append(">")
                .append(escape(value))
                .append("</").append(name).append(">");
    }

    public static void tagRaw(StringBuilder sb, String name, String raw) {
        sb.append("<").append(name).append(">")
                .append(raw == null ? "" : raw)
                .append("</").append(name).append(">");
    }
}


