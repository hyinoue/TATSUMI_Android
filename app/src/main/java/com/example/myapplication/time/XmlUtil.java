package com.example.myapplication.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　XmlUtil ..... XML生成ユーティリティ
//　　　　　　:　escape ..... XMLエスケープ処理
//　　　　　　:　toXsdDateTime ..... xsd:dateTime形式文字列へ変換（JST固定/タイムゾーン無し）
//　　　　　　:　tag ..... XMLタグ生成（値はエスケープ）
//　　　　　　:　tagRaw ..... XMLタグ生成（値はそのまま出力）
//============================================================

//========================
//　処理概要　:　XmlUtilクラス
//========================
public class XmlUtil {

    //===============================
    //　機　能　:　XmlUtilのインスタンス生成を禁止する
    //　引　数　:　なし
    //　戻り値　:　[XmlUtil] ..... なし
    //===============================
    private XmlUtil() {
        // Utilityクラスのためインスタンス化しない
    }

    //==============================
    //　機　能　:　XMLエスケープを行う
    //　引　数　:　s ..... String
    //　戻り値　:　[String] ..... XMLエスケープ済み文字列（nullは空文字）
    //==============================
    public static String escape(String s) {

        // nullは空文字で返す
        if (s == null) return "";

        // XML特殊文字をエスケープ
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 送信用 xsd:dateTime
     * ★ASMX 側がタイムゾーン無し前提で「完全一致」をしているため、
     * ここでは "+09:00" を付けない（yyyy-MM-dd'T'HH:mm:ss）形式で送る。
     * <p>
     * 例: 2020-03-21T00:00:00
     */
    //================================
    //　機　能　:　XSD日時文字列へ変換する
    //　引　数　:　d ..... Date
    //　戻り値　:　[String] ..... xsd:dateTime形式文字列（JST固定）
    //================================
    public static String toXsdDateTime(Date d) {

        // yyyy-MM-dd'T'HH:mm:ss 形式（タイムゾーン無し）
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        // タイムゾーンをJST固定（端末設定に依存させない）
        f.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

        // Dateをフォーマットして返却
        return f.format(d);
    }

    //===================================
    //　機　能　:　XMLタグ文字列を生成する
    //　引　数　:　sb ..... StringBuilder
    //　　　　　:　name ..... String（タグ名）
    //　　　　　:　value ..... String（値：エスケープ対象）
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void tag(StringBuilder sb, String name, String value) {

        // <name>escapedValue</name> を生成
        sb.append("<").append(name).append(">")
                .append(escape(value))   // 値はXMLエスケープ
                .append("</").append(name).append(">");
    }

    //===================================
    //　機　能　:　値をそのまま使ってXMLタグ文字列を生成する
    //　引　数　:　sb ..... StringBuilder
    //　　　　　:　name ..... String（タグ名）
    //　　　　　:　raw ..... String（値：エスケープしない）
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void tagRaw(StringBuilder sb, String name, String raw) {

        // <name>raw</name> を生成（エスケープしない）
        sb.append("<").append(name).append(">")
                .append(raw == null ? "" : raw) // nullは空文字
                .append("</").append(name).append(">");
    }
}