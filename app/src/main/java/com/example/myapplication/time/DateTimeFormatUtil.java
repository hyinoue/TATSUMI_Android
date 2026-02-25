package com.example.myapplication.time;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　DateTimeFormatUtil ..... 日時文字列の表示用整形ユーティリティ
//　　　　　　:　formatSagyouYmdForDisplay ..... 作業日時文字列を画面表示用へ整形
//============================================================

//===================================
//　処理概要　:　DateTimeFormatUtilクラス
//===================================
public final class DateTimeFormatUtil {
    private static final String OUTPUT_PATTERN = "MM-dd HH:mm";     // 出力形式（表示用）
    private static final String[] INPUT_PATTERNS = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};   // 入力として想定する形式（DB/既存資産互換）

    //==========================================
    //　機　能　:　DateTimeFormatUtilの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[DateTimeFormatUtil] ..... なし
    //==========================================
    private DateTimeFormatUtil() {
        // Utilityクラスのためインスタンス化しない
    }

    //========================================
    //　機　能　:　sagyou Ymd For Displayを整形する
    //　引　数　:　value ..... String（入力日時文字列：例 "yyyy-MM-dd HH:mm:ss" 等）
    //　戻り値　:　[String] ..... 表示用文字列（整形不可時は可能な範囲で返却）
    //========================================
    public static String formatSagyouYmdForDisplay(@Nullable String value) {

        // nullは空文字
        if (value == null) {
            return "";
        }

        // 前後空白除去
        String trimmed = value.trim();

        // 空は空文字
        if (trimmed.isEmpty()) {
            return "";
        }

        // 想定パターンで順番にパースを試みる
        for (String pattern : INPUT_PATTERNS) {
            try {
                // パーサ生成（日本ロケール）
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.JAPAN);

                // 厳密パース（例：存在しない日付を弾く）
                parser.setLenient(false);

                // 日付へ変換
                Date parsed = parser.parse(trimmed);

                // 成功したら表示用フォーマットへ変換して返す
                if (parsed != null) {
                    return new SimpleDateFormat(OUTPUT_PATTERN, Locale.JAPAN).format(parsed);
                }

            } catch (ParseException ignored) {
                // 次のパターンで再試行
            }
        }

        // ここに来るのは想定外フォーマットの場合
        // 例："yyyy-MM-dd HH:mm:ss" 相当なら "MM-dd HH:mm" 部分を切り出して返す
        if (trimmed.length() >= 16) {
            return trimmed.substring(5, 16);
        }

        // それでも短い場合はそのまま返す（最悪でも情報を落とさない）
        return trimmed;
    }
}