package com.example.myapplication.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　XsdDateTime ..... xsd:dateTime文字列解析ユーティリティ
//　　　　　　:　parse ..... xsd:dateTime文字列をDateへ変換
//============================================================

//============================
//　処理概要　:　XsdDateTimeクラス
//============================
public class XsdDateTime {

    //===================================
    //　機　能　:　XsdDateTimeの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[XsdDateTime] ..... なし
    //===================================
    private XsdDateTime() {
        // Utilityクラスのためインスタンス化しない
    }

    // ASMXは環境により
    // 例1: 2025-12-24T10:11:12+09:00
    // 例2: 2025-12-24T10:11:12
    // 例3: 2025-12-24T10:11:12.123+09:00

    //============================
    //　機　能　:　parseの処理
    //　引　数　:　s ..... String（xsd:dateTime形式文字列）
    //　戻り値　:　[Date] ..... 変換結果（null許容）
    //============================
    public static Date parse(String s) throws ParseException {

        // nullはnullで返す
        if (s == null) return null;

        // 前後空白除去
        s = s.trim();
        if (s.isEmpty()) return null;

        // 小数秒がある場合は削除（例: .123 → 除去）
        // タイムゾーン表記（+09:00）に影響しない範囲で除去
        String normalized = s.replaceFirst("\\.\\d{1,9}", "");

        ParseException last = null;

        // -----------------------------
        // 1. タイムゾーン付き（例: +09:00）
        // -----------------------------
        try {
            SimpleDateFormat f =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);

            return f.parse(normalized);

        } catch (ParseException e) {
            last = e;
        }

        // -----------------------------
        // 2. タイムゾーン無し
        // -----------------------------
        try {
            SimpleDateFormat f =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

            return f.parse(normalized);

        } catch (ParseException e) {
            last = e;
        }

        // どちらも失敗した場合は最後の例外を投げる
        throw last;
    }
}