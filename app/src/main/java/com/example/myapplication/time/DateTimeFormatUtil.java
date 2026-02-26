package com.example.myapplication.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　DateTimeFormatUtil ..... 日時関連の整形ユーティリティ
//============================================================

//===================================
//　処理概要　:　DateTimeFormatUtilクラス
//===================================
public final class DateTimeFormatUtil {
    private static final String DB_YMD_HMS_PATTERN = "yyyy-MM-dd HH:mm:ss";   // DB保存/更新日時
    private static final String COMPACT_YMD_HMS_PATTERN = "yyyyMMddHHmmss";    // 並び順/採番用
    private static final String OUTPUT_PATTERN = "MM-dd HH:mm";     // 出力形式（表示用）
    private static final String[] INPUT_PATTERNS = {DB_YMD_HMS_PATTERN, "yyyy-MM-dd HH:mm"};   // 入力として想定する形式（DB/既存資産互換）

    //==========================================
    //　機　能　:　DateTimeFormatUtilのインスタンス生成を禁止する
    //　引　数　:　なし
    //　戻り値　:　[DateTimeFormatUtil] ..... なし
    //==========================================
    private DateTimeFormatUtil() {
        // Utilityクラスのためインスタンス化しない
    }

    //========================================
    //　機　能　:　現在日時をDB保存用文字列で返す
    //　引　数　:　なし
    //　戻り値　:　[String] ..... yyyy-MM-dd HH:mm:ss
    //========================================
    public static String nowDbYmdHms() {
        return formatDbYmdHms(new Date());
    }

    //========================================
    //　機　能　:　DateをDB保存用文字列へ整形する
    //　引　数　:　value ..... Date
    //　戻り値　:　[String] ..... yyyy-MM-dd HH:mm:ss
    //========================================
    public static String formatDbYmdHms(Date value) {
        return new SimpleDateFormat(DB_YMD_HMS_PATTERN, Locale.JAPAN).format(value);
    }

    //========================================
    //　機　能　:　現在日時を並び順キー文字列で返す
    //　引　数　:　なし
    //　戻り値　:　[String] ..... yyyyMMddHHmmss
    //========================================
    public static String nowCompactYmdHms() {
        return new SimpleDateFormat(COMPACT_YMD_HMS_PATTERN, Locale.ROOT).format(new Date());
    }
}
