package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.log.FileLogger;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　MyApplication ..... アプリケーションクラス
//　　　　　　:　onCreate ..... アプリ起動時の初期化処理
//============================================================

//====================================
//　処理概要　:　MyApplicationクラス
//====================================
public class MyApplication extends Application {

    //========================================
    //　機　能　:　起動時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================================
    @Override
    public void onCreate() {

        // Applicationの初期化処理を実行
        super.onCreate();

        // アプリ起動ログを出力
        FileLogger.info(this, "Application#onCreate", "app start");

        // 未捕捉例外ハンドラを設定（全スレッド共通）
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->

                // 例外発生時にログファイルへ出力
                FileLogger.error(
                        this,
                        "UncaughtException",
                        "thread=" + thread.getName(),
                        throwable
                )
        );
    }
}