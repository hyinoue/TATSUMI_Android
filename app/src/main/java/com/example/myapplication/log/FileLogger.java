package com.example.myapplication.log;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　info ..... 情報ログ出力
//　　　　　　:　error ..... エラーログ出力（例外含む）
//　　　　　　:　write ..... ログファイル書込処理（内部共通）
//============================================================

//====================================
//　処理概要　:　FileLoggerクラス
//====================================
public final class FileLogger {

    // ログ出力ファイル名
    private static final String LOG_FILE_NAME = "ErrorLog.txt";

    // 排他制御用ロック（マルチスレッド対策）
    private static final Object LOCK = new Object();

    // インスタンス化禁止（ユーティリティクラス）
    private FileLogger() {
    }

    //===========================================
    //　機　能　:　infoログを出力する
    //　引　数　:　context ..... Context
    //　　　　　:　source ..... String（出力元クラス・処理名）
    //　　　　　:　message ..... String（ログメッセージ）
    //　戻り値　:　[void] ..... なし
    //===========================================
    public static void info(Context context, String source, String message) {

        // 共通write処理へ委譲（例外なし）
        write(context, "INFO", source, message, null);
    }

    //===========================================
    //　機　能　:　errorログを出力する
    //　引　数　:　context ..... Context
    //　　　　　:　source ..... String（出力元クラス・処理名）
    //　　　　　:　message ..... String（ログメッセージ）
    //　　　　　:　t ..... Throwable（例外）
    //　戻り値　:　[void] ..... なし
    //===========================================
    public static void error(Context context, String source, String message, Throwable t) {

        // 共通write処理へ委譲（例外情報付き）
        write(context, "ERROR", source, message, t);
    }

    //===========================================
    //　機　能　:　ログファイルへ書き込む
    //　引　数　:　context ..... Context
    //　　　　　:　level ..... String（INFO/ERROR）
    //　　　　　:　source ..... String（出力元）
    //　　　　　:　message ..... String（メッセージ）
    //　　　　　:　t ..... Throwable（例外）
    //　戻り値　:　[void] ..... なし
    //===========================================
    private static void write(Context context,
                              String level,
                              String source,
                              String message,
                              Throwable t) {

        // タイムスタンプ生成（ミリ秒付き）
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS0", Locale.JAPAN)
                .format(new Date());

        // アプリ内部ストレージ上のログファイル
        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);

        // 出力内容を構築
        StringBuilder sb = new StringBuilder();

        // 1行目：日時|レベル|固定文字|出力元
        sb.append(ts)
                .append("|")
                .append(level)
                .append("|file|")
                .append(source)
                .append("\n");

        // メッセージがあれば出力
        if (message != null && !message.isEmpty()) {
            sb.append(message).append("\n");
        }

        // 例外があればスタックトレース出力
        if (t != null) {
            sb.append(t.toString()).append("\n");

            for (StackTraceElement e : t.getStackTrace()) {
                sb.append("   at ")
                        .append(e.toString())
                        .append("\n");
            }
        }

        // 区切りの空行
        sb.append("\n");

        // マルチスレッド対策のため排他制御
        synchronized (LOCK) {
            try (FileOutputStream fos = new FileOutputStream(logFile, true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter bw = new BufferedWriter(osw)) {

                // 追記モードで書き込み
                bw.write(sb.toString());
                bw.flush();

            } catch (Exception ignored) {
                // ログ出力失敗時の再帰呼び出し防止のため何もしない
            }
        }
    }
}