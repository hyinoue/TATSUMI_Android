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

    private TextView tvWanPowerValue;
    private TextView tvGprsValue;
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
        updatePowerStatus();
        updateGprsStatus();
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
        tvWanPowerValue = findViewById(R.id.tvWanPowerValue);
        tvGprsValue = findViewById(R.id.tvGprsValue);
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
        if (!updatePowerStatus()) {
            showErrorMsg("WAN電源がOFFです。", MsgDispMode.MsgBox);
            return;
        }

        if (!isNetworkConnected()) {
            showErrorMsg("通信に接続できませんでした。", MsgDispMode.MsgBox);
            gprsConnected = false;
            updateGprsStatus();
            return;
        }

        gprsConnected = true;
        updateGprsStatus();
        showInfoMsg("接続しました", MsgDispMode.MsgBox);
    }

    private void updateGprsStatus() {
        if (gprsConnected) {
            tvGprsValue.setText(STATUS_CONNECTED);
            tvGprsValue.setBackgroundColor(Color.parseColor("#9ACD32"));
        } else {
            tvGprsValue.setText(STATUS_DISCONNECTED);
            tvGprsValue.setBackgroundColor(Color.WHITE);
        }
    }

    private boolean updatePowerStatus() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            tvWanPowerValue.setText(STATUS_ERROR);
            tvWanPowerValue.setTextColor(Color.BLUE);
            return false;
        }

        boolean hasAvailableTransport = false;
        Network active = manager.getActiveNetwork();
        if (active != null) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
            if (capabilities != null) {
                hasAvailableTransport = hasSupportedTransport(capabilities);
            }
        }

        if (hasAvailableTransport) {
            tvWanPowerValue.setText(STATUS_ON);
            tvWanPowerValue.setTextColor(Color.parseColor("#006400"));
            return true;
        }

        tvWanPowerValue.setText(STATUS_OFF);
        tvWanPowerValue.setTextColor(Color.BLACK);
        return false;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        Network active = manager.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
        if (capabilities == null) {
            return false;
        }
        return hasSupportedTransport(capabilities)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private boolean hasSupportedTransport(NetworkCapabilities capabilities) {
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
