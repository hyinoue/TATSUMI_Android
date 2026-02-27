package com.example.myapplication.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.densowave.bhtsdk.barcode.OutputSettings;


//============================================================
//　処理概要　:　デンソースキャナのBroadcastを受信して読み取り文字列を通知するクラス
//　関　　数　:　DensoScanReceiver ..... DENSOスキャン結果Broadcast受信→アプリ内Broadcastへ橋渡し
//　　　　　　:　onReceive ..... 受信処理（EXTRA取得/正規化/内部Broadcast送信）
//　　　　　　:　normalize ..... 受信文字列の正規化（改行除去/trim）
//　　　　　　:　ImagerTestActivityBridge ..... Receiver→Activity橋渡し用の内部Action/Extra定義
//============================================================

public class DensoScanReceiver extends BroadcastReceiver {

    public static final String TAG = "DensoScanReceiver"; // ログタグ

    // Manifestで指定したAction名（SDK側の設定とも一致させる）
    public static final String ACTION_DENSO_SCAN = "com.example.myapplication.DENSO_SCAN"; // 外部スキャン受信アクション

    //============================================================
    //　機　能　:　受信時の処理
    //　引　数　:　context ..... コンテキスト情報
    //　　　　　:　intent ..... 画面遷移情報
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    public void onReceive(Context context, Intent intent) {
        // intent未設定は処理しない
        if (intent == null) return;

        // 受信したActionをログ出力（トラブルシュート用）
        Log.d(TAG, "RECEIVED action=" + intent.getAction());

        // SDKが付与するEXTRAキーは OutputSettings に定数がある
        String[] dataArr = intent.getStringArrayExtra(OutputSettings.EXTRA_BARCODE_DATA);

        // 先頭要素のみ使用（存在しない場合は空文字）
        String data = "";
        if (dataArr != null && dataArr.length > 0 && dataArr[0] != null) {
            data = dataArr[0];
        }

        // 改行等を除去して正規化
        data = normalize(data);

        // 受信データをログ出力
        Log.d(TAG, "RECEIVED data=" + data);

        // Activityへ渡す（アプリ内Broadcastとして橋渡し）
        Intent i = new Intent(ImagerTestActivityBridge.ACTION_INTERNAL_SCAN);
        i.putExtra(ImagerTestActivityBridge.EXTRA_INTERNAL_DATA, data);
        context.sendBroadcast(i);
    }

    //============================================================
    //　機　能　:　文字列を正規化する
    //　引　数　:　s ..... 文字列
    //　戻り値　:　[String] ..... 正規化後文字列
    //============================================================
    private String normalize(String s) {
        // nullは空文字扱い
        if (s == null) return "";

        // 改行コード除去＋トリム
        return s.replace("\r", "").replace("\n", "").trim();
    }

    //============================================================
    //　処理概要　:　内部ブロードキャスト連携定数をまとめたクラス
    //============================================================

    /**
     * Activity側で使う内部Action/Extra定義（Receiver→Activity橋渡し）
     * ※別ファイルにしたくなければ Activity側に同じ定数を置いてもOK
     */
    public static class ImagerTestActivityBridge {

        // Receiver→Activityへ渡すための内部Broadcast Action
        public static final String ACTION_INTERNAL_SCAN = "com.example.myapplication.INTERNAL_SCAN";

        // 内部Broadcastに乗せるデータのキー
        public static final String EXTRA_INTERNAL_DATA = "data";
    }
}
