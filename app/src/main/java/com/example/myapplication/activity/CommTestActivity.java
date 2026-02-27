package com.example.myapplication.activity;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

//==================================================================================
//　処理概要　:　通信テスト画面（Activity）
//　関　　数　:　onCreate ................. 画面生成時の初期化（部品取得/イベント設定/初期表示）
//　　　　　　:　setupBottomButtons ...... 下部ボタン設定
//　　　　　　:　bindViews ............... 画面部品のバインド
//　　　　　　:　setupActionButtons ...... 画面内ボタン（疎通テスト）イベント設定
//　　　　　　:　onFunctionYellow ........ 終了
//　　　　　　:　onFunctionBlue .......... 何もしない（最小画面）
//　　　　　　:　testConnection .......... 疎通テスト（インターネット到達性の確認）
//　　　　　　:　updateInternetStatus .... インターネット接続状態表示更新
//　　　　　　:　updateNetworkStatus ..... 回線状態表示更新（モバイル/Wi-Fi/アクティブ回線）
//　　　　　　:　getNetworkSnapshot ...... 回線スナップショット取得
//==================================================================================

public class CommTestActivity extends BaseActivity {

    private static final String STATUS_ON = "ＯＮ";                    // ON表示文字列
    private static final String STATUS_OFF = "ＯＦＦ";                  // OFF表示文字列
    private static final String STATUS_ERROR = "エラー";               // エラー表示文字列
    private static final String STATUS_CONNECTED = "接続";             // 接続状態表示文字列
    private static final String STATUS_DISCONNECTED = "未接続";         // 未接続状態表示文字列

    private static final String STATUS_CELLULAR = "モバイル";           // モバイル回線表示文字列
    private static final String STATUS_WIFI = "Wi-Fi";                // Wi-Fi表示文字列
    private static final String STATUS_NONE = "なし";                  // 回線なし表示文字列
    private static final String STATUS_OTHER = "その他";               // その他回線表示文字列

    private TextView tvMobilePowerValue;    // モバイル電波状態
    private TextView tvWifiValue;           // Wi-Fi状態
    private TextView tvActiveNetworkValue;  // 有効ネットワーク種別
    private TextView tvInternetValue;       // インターネット接続状態

    // 疎通テスト結果（インターネット接続状態）を保持
    private boolean gprsConnected; // モバイル接続フラグ

    //============================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... 画面再生成時の保存状態
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウト設定
        setContentView(R.layout.activity_comm_test);

        // 下部ボタン設定（終了のみ）
        setupBottomButtons();

        // 画面部品の紐づけ
        bindViews();

        // 画面内ボタン（疎通テスト）のイベント設定
        setupActionButtons();

        // 起動時点のネットワーク状態を取得し、表示へ反映
        NetworkSnapshot snapshot = updateNetworkStatus();

        // 初期の疎通表示は「インターネット到達性」を基準にセット
        gprsConnected = snapshot.hasInternet;
        updateInternetStatus();
    }

    //============================================================
    //　機　能　:　下部ボタンの表示内容を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupBottomButtons() {
        // 下部ボタン取得
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);

        // 文言設定（最小画面：終了のみ）
        if (yellow != null) {
            yellow.setText("終了");
        }
        if (blue != null) {
            blue.setText("");
        }
        if (red != null) {
            red.setText("");
        }
        if (green != null) {
            green.setText("");
        }

        // 活性制御（BaseActivity側の共通処理想定）
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　画面部品をバインドする
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void bindViews() {
        tvMobilePowerValue = findViewById(R.id.tvMobilePowerValue);
        tvWifiValue = findViewById(R.id.tvWifiValue);
        tvActiveNetworkValue = findViewById(R.id.tvActiveNetworkValue);
        tvInternetValue = findViewById(R.id.tvInternetValue);
    }

    //============================================================
    //　機　能　:　画面内ボタンのイベントを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupActionButtons() {
        Button btnTest = findViewById(R.id.btnTestConn);

        // 疎通テストボタン押下 → 接続確認を実施
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> testConnection());
        }
    }

    //============================================================
    //　機　能　:　黄ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionYellow() {
        // 終了
        finish();
    }

    //============================================================
    //　機　能　:　青ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionBlue() {
        // 最小画面では保存処理なし
    }

    //============================================================
    //　機　能　:　疎通テストを実行する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void testConnection() {
        // 現在のネットワーク状態を取得し、表示へ反映
        NetworkSnapshot snapshot = updateNetworkStatus();

        // インターネット到達性が無い場合は失敗扱い
        if (!snapshot.hasInternet) {
            showErrorMsg("接続できませんでした。", MsgDispMode.MsgBox);

            // 疎通状態をOFFにして表示更新
            gprsConnected = false;
            updateInternetStatus();
            return;
        }

        // 到達性あり：成功扱い
        gprsConnected = true;
        updateInternetStatus();
        showInfoMsg("接続しました", MsgDispMode.MsgBox);
    }

    //============================================================
    //　機　能　:　インターネット疎通状態を表示更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void updateInternetStatus() {
        if (tvInternetValue == null) {
            return;
        }

        if (gprsConnected) {
            // 接続あり：文字・背景色を変更
            tvInternetValue.setText(STATUS_CONNECTED);
            tvInternetValue.setBackgroundColor(Color.parseColor("#9ACD32"));
        } else {
            // 接続なし：文字・背景色を戻す
            tvInternetValue.setText(STATUS_DISCONNECTED);
            tvInternetValue.setBackgroundColor(Color.WHITE);
        }
    }

    //============================================================
    //　機　能　:　回線状態を取得して表示更新する
    //　引　数　:　なし
    //　戻り値　:　[NetworkSnapshot] ..... 現在の回線状態スナップショット
    //============================================================
    private NetworkSnapshot updateNetworkStatus() {
        // 回線状態取得
        NetworkSnapshot snapshot = getNetworkSnapshot();

        // モバイル回線の有無表示（ON/OFF）
        if (tvMobilePowerValue != null) {
            if (snapshot.hasCellular) {
                tvMobilePowerValue.setText(STATUS_ON);
                tvMobilePowerValue.setTextColor(Color.parseColor("#006400"));
            } else {
                tvMobilePowerValue.setText(STATUS_OFF);
                tvMobilePowerValue.setTextColor(Color.BLACK);
            }
        }

        // Wi-Fiの有無表示（接続/未接続）
        if (tvWifiValue != null) {
            if (snapshot.hasWifi) {
                tvWifiValue.setText(STATUS_CONNECTED);
                tvWifiValue.setTextColor(Color.parseColor("#006400"));
            } else {
                tvWifiValue.setText(STATUS_DISCONNECTED);
                tvWifiValue.setTextColor(Color.BLACK);
            }
        }

        // アクティブ回線種別表示（モバイル/Wi-Fi/なし/その他）
        if (tvActiveNetworkValue != null) {
            tvActiveNetworkValue.setText(snapshot.activeNetworkName);
        }

        return snapshot;
    }

    //============================================================
    //　機　能　:　ネットワーク状態スナップショットを取得する
    //　引　数　:　なし
    //　戻り値　:　[NetworkSnapshot] ..... 回線状態
    //============================================================
    private NetworkSnapshot getNetworkSnapshot() {
        // ConnectivityManager取得
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // 取得不可の場合はエラー表示用スナップショットを返却
        if (manager == null) {
            if (tvMobilePowerValue != null) {
                tvMobilePowerValue.setText(STATUS_ERROR);
                tvMobilePowerValue.setTextColor(Color.BLUE);
            }
            if (tvWifiValue != null) {
                tvWifiValue.setText(STATUS_ERROR);
                tvWifiValue.setTextColor(Color.BLUE);
            }
            if (tvActiveNetworkValue != null) {
                tvActiveNetworkValue.setText(STATUS_ERROR);
            }
            return new NetworkSnapshot(false, false, false, STATUS_ERROR);
        }

        boolean hasCellular = false;
        boolean hasWifi = false;

        // 端末が保持する全ネットワークから「モバイル/Wi-Fiが存在するか」を集計
        for (Network network : manager.getAllNetworks()) {
            NetworkCapabilities networkCapabilities = manager.getNetworkCapabilities(network);
            if (networkCapabilities == null) {
                continue;
            }
            hasCellular = hasCellular || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            hasWifi = hasWifi || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }

        // アクティブ回線（実際に使用中の回線）の種別と、インターネット到達性を判定
        String activeNetworkName = STATUS_NONE;
        boolean hasInternet = false;

        Network active = manager.getActiveNetwork();
        if (active != null) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
            if (capabilities != null) {
                // 「インターネット接続可能」フラグ
                hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                // アクティブ回線種別を判定
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    activeNetworkName = STATUS_CELLULAR;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    activeNetworkName = STATUS_WIFI;
                } else {
                    activeNetworkName = STATUS_OTHER;
                }
            }
        }

        return new NetworkSnapshot(hasCellular, hasWifi, hasInternet, activeNetworkName);
    }

    /**
     * ネットワーク状態保持用のスナップショット（画面表示/疎通判定に利用）。
     */
    private static final class NetworkSnapshot {
        private final boolean hasCellular;
        private final boolean hasWifi;
        private final boolean hasInternet;
        private final String activeNetworkName;

        private NetworkSnapshot(boolean hasCellular, boolean hasWifi, boolean hasInternet, String activeNetworkName) {
            this.hasCellular = hasCellular;
            this.hasWifi = hasWifi;
            this.hasInternet = hasInternet;
            this.activeNetworkName = activeNetworkName;
        }
    }
}
