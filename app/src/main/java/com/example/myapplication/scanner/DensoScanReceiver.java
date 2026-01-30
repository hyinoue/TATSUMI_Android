package com.example.myapplication.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.densowave.bhtsdk.barcode.OutputSettings;


//============================================================
//　処理概要　:　DensoScanReceiverクラス
//============================================================

public class DensoScanReceiver extends BroadcastReceiver {

    public static final String TAG = "DensoScanReceiver";

    // Manifestで指定したAction名（SDK側の設定とも一致させる）
    public static final String ACTION_DENSO_SCAN = "com.example.myapplication.DENSO_SCAN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        Log.d(TAG, "RECEIVED action=" + intent.getAction());

        // SDKが付与するEXTRAキーは OutputSettings に定数があります
        String[] dataArr = intent.getStringArrayExtra(OutputSettings.EXTRA_BARCODE_DATA);

        String data = "";
        if (dataArr != null && dataArr.length > 0 && dataArr[0] != null) {
            data = dataArr[0];
        }

        data = normalize(data);

        Log.d(TAG, "RECEIVED data=" + data);

        // Activityへ渡す（アプリ内Broadcast）
        Intent i = new Intent(ImagerTestActivityBridge.ACTION_INTERNAL_SCAN);
        i.putExtra(ImagerTestActivityBridge.EXTRA_INTERNAL_DATA, data);
        context.sendBroadcast(i);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", "").trim();
    }

    /**
     * Activity側で使う内部Action/Extra定義（Receiver→Activity橋渡し）
     * ※別ファイルにしたくなければ Activity側に同じ定数を置いてもOKです
     */
    public static class ImagerTestActivityBridge {
        public static final String ACTION_INTERNAL_SCAN = "com.example.myapplication.INTERNAL_SCAN";
        public static final String EXTRA_INTERNAL_DATA = "data";
    }
}
