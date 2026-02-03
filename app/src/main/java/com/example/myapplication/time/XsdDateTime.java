package com.example.myapplication.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//======================
//　処理概要　:　XsdDateTimeクラス
//======================

public class XsdDateTime {
    //=============================
    //　機　能　:　XsdDateTimeの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[XsdDateTime] ..... なし
    //=============================
    private XsdDateTime() {
    }

    // ASMXは環境により
    // 例1: 2025-12-24T10:11:12+09:00
    // 例2: 2025-12-24T10:11:12
    // 例3: 2025-12-24T10:11:12.123+09:00
    //======================
    //　機　能　:　parseの処理
    //　引　数　:　s ..... String
    //　戻り値　:　[Date] ..... なし
    //======================
    public static Date parse(String s) throws ParseException {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        // 小数秒がある場合は落とす（.123 → 削る）
        // "XXX" 形式に影響しない範囲で除去
        String normalized = s.replaceFirst("\\.\\d{1,9}", "");

        ParseException last = null;

        // タイムゾーン付き
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            return f.parse(normalized);
        } catch (ParseException e) {
            last = e;
        }

        // タイムゾーン無し
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return f.parse(normalized);
        } catch (ParseException e) {
            last = e;
        }

        throw last;
    }
}
