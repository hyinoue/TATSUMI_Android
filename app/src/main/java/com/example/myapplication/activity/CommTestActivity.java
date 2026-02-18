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


//=================================
//　処理概要　:　CommTestActivityクラス
//=================================

/**
 * 通信テスト用の最小画面Activity。
 *
 * <p>新SIM運用向けに、回線状態確認と疎通テストだけを提供する。</p>
 */
public class CommTestActivity extends BaseActivity {

    private static final String STATUS_ON = "ＯＮ";
    private static final String STATUS_OFF = "ＯＦＦ";
    private static final String STATUS_ERROR = "エラー";
    private static final String STATUS_CONNECTED = "接続";
    private static final String STATUS_DISCONNECTED = "未接続";

    private static final String STATUS_CELLULAR = "モバイル";
    private static final String STATUS_WIFI = "Wi-Fi";
    private static final String STATUS_NONE = "なし";
    private static final String STATUS_OTHER = "その他";

    private TextView tvMobilePowerValue;
    private TextView tvWifiValue;
    private TextView tvActiveNetworkValue;
    private TextView tvInternetValue;
    private boolean gprsConnected;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comm_test);
        setupBottomButtons();

        bindViews();
        setupActionButtons();
        NetworkSnapshot snapshot = updateNetworkStatus();
        gprsConnected = snapshot.hasInternet;
        updateInternetStatus();
    }
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupBottomButtons() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        if (yellow != null) {
            yellow.setText("終了");
        }
        if (blue != null) {
            blue.setText("");
        }
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
    }

    private void bindViews() {
        tvMobilePowerValue = findViewById(R.id.tvMobilePowerValue);
        tvWifiValue = findViewById(R.id.tvWifiValue);
        tvActiveNetworkValue = findViewById(R.id.tvActiveNetworkValue);
        tvInternetValue = findViewById(R.id.tvInternetValue);
    }

    private void setupActionButtons() {
        Button btnTest = findViewById(R.id.btnTestConn);

        if (btnTest != null) {
            btnTest.setOnClickListener(v -> testConnection());
        }

    }

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //==================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionBlue() {
        // 最小画面では保存処理なし
    }

    private void testConnection() {
        NetworkSnapshot snapshot = updateNetworkStatus();

        if (!snapshot.hasInternet) {
            showErrorMsg("接続できませんでした。", MsgDispMode.MsgBox);
            gprsConnected = false;
            updateInternetStatus();
            return;
        }

        gprsConnected = true;
        updateInternetStatus();

        if (STATUS_CELLULAR.equals(snapshot.activeNetworkName)) {
            showInfoMsg("モバイル回線で接続できています", MsgDispMode.MsgBox);
            return;
        }

        if (STATUS_WIFI.equals(snapshot.activeNetworkName)) {
            showInfoMsg("Wi-Fiで接続できています", MsgDispMode.MsgBox);
            return;
        }

        showInfoMsg("接続しました", MsgDispMode.MsgBox);
    }

    private void updateInternetStatus() {
        if (gprsConnected) {
            tvInternetValue.setText(STATUS_CONNECTED);
            tvInternetValue.setBackgroundColor(Color.parseColor("#9ACD32"));
        } else {
            tvInternetValue.setText(STATUS_DISCONNECTED);
            tvInternetValue.setBackgroundColor(Color.WHITE);
        }
    }

    private NetworkSnapshot updateNetworkStatus() {
        NetworkSnapshot snapshot = getNetworkSnapshot();

        if (snapshot.hasCellular) {
            tvMobilePowerValue.setText(STATUS_ON);
            tvMobilePowerValue.setTextColor(Color.parseColor("#006400"));
        } else {
            tvMobilePowerValue.setText(STATUS_OFF);
            tvMobilePowerValue.setTextColor(Color.BLACK);
        }

        if (snapshot.hasWifi) {
            tvWifiValue.setText(STATUS_CONNECTED);
            tvWifiValue.setTextColor(Color.parseColor("#006400"));
        } else {
            tvWifiValue.setText(STATUS_DISCONNECTED);
            tvWifiValue.setTextColor(Color.BLACK);
        }

        tvActiveNetworkValue.setText(snapshot.activeNetworkName);
        return snapshot;
    }

    private NetworkSnapshot getNetworkSnapshot() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            tvMobilePowerValue.setText(STATUS_ERROR);
            tvMobilePowerValue.setTextColor(Color.BLUE);
            tvWifiValue.setText(STATUS_ERROR);
            tvWifiValue.setTextColor(Color.BLUE);
            tvActiveNetworkValue.setText(STATUS_ERROR);
            return new NetworkSnapshot(false, false, false, STATUS_ERROR);
        }

        boolean hasCellular = false;
        boolean hasWifi = false;

        for (Network network : manager.getAllNetworks()) {
            NetworkCapabilities networkCapabilities = manager.getNetworkCapabilities(network);
            if (networkCapabilities == null) {
                continue;
            }
            hasCellular = hasCellular || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            hasWifi = hasWifi || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }

        String activeNetworkName = STATUS_NONE;
        boolean hasInternet = false;
        Network active = manager.getActiveNetwork();
        if (active != null) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
            if (capabilities != null) {
                hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
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
