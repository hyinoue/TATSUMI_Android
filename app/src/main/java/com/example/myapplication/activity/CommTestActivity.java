package com.example.myapplication.activity;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.settings.AppSettings;
import com.google.android.material.button.MaterialButton;


//=================================
//　処理概要　:　CommTestActivityクラス
//=================================

/**
 * 通信テスト用のシンプルな画面Activity。
 *
 * <p>画面下ボタンの「終了」だけを有効化し、
 * 実装確認/疎通確認のための最小UIとして利用する。</p>
 */
public class CommTestActivity extends BaseActivity {

    private static final String STATUS_ON = "ＯＮ";
    private static final String STATUS_OFF = "ＯＦＦ";
    private static final String STATUS_UNKNOWN = "不明";
    private static final String STATUS_ERROR = "エラー";
    private static final String STATUS_CONNECTED = "接続";
    private static final String STATUS_DISCONNECTED = "未接続";

    private EditText etCommName;
    private EditText etApn;
    private EditText etUser;
    private EditText etPass;
    private TextView tvWanPowerValue;
    private TextView tvGprsValue;

    private boolean testSuccess;
    private boolean connectionCreated;
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
        loadSettings();
        etCommName.requestFocus();
        setupInputListeners();
        setupActionButtons();
        updatePowerStatus();
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
            blue.setText("保存");
        }
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
    }

    private void bindViews() {
        etCommName = findViewById(R.id.etCommName);
        etApn = findViewById(R.id.etApn);
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        tvWanPowerValue = findViewById(R.id.tvWanPowerValue);
        tvGprsValue = findViewById(R.id.tvGprsValue);
    }

    private void loadSettings() {
        etCommName.setText(AppSettings.CommName);
        etApn.setText(AppSettings.CommApn);
        etUser.setText(AppSettings.CommUser);
        etPass.setText(AppSettings.CommPasswd);
    }

    private void setupInputListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                testSuccess = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        etCommName.addTextChangedListener(watcher);
        etApn.addTextChangedListener(watcher);
        etUser.addTextChangedListener(watcher);
        etPass.addTextChangedListener(watcher);
    }

    private void setupActionButtons() {
        Button btnCreate = findViewById(R.id.btnCreateConn);
        Button btnDelete = findViewById(R.id.btnDeleteConn);
        Button btnTest = findViewById(R.id.btnTestConn);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> createConnection());
        }
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteConnection());
        }
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> testConnection());
        }
        if (btnDisconnect != null) {
            btnDisconnect.setOnClickListener(v -> disconnect());
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
        if (!testSuccess) {
            showErrorMsg("接続テストが行われていません。設定保存前に接続テストを行ってください。", MsgDispMode.MsgBox);
            return;
        }

        AppSettings.CommName = textOf(etCommName);
        AppSettings.CommApn = textOf(etApn);
        AppSettings.CommUser = textOf(etUser);
        AppSettings.CommPasswd = textOf(etPass);
        AppSettings.save();
        finish();
    }

    private void createConnection() {
        if (!updatePowerStatus()) {
            showErrorMsg("WAN電源がOFFです。", MsgDispMode.MsgBox);
            return;
        }
        connectionCreated = true;
        gprsConnected = false;
        updateGprsStatus();
        showInfoMsg("接続設定を作成しました。", MsgDispMode.MsgBox);
    }

    private void deleteConnection() {
        if (!connectionCreated) {
            showErrorMsg("接続設定が存在していません。", MsgDispMode.MsgBox);
            return;
        }
        showQuestion(etCommName.getText() + "の接続設定を削除します。よろしいですか？", yes -> {
            if (!yes) {
                return;
            }
            connectionCreated = false;
            gprsConnected = false;
            updateGprsStatus();
            showInfoMsg("削除しました。", MsgDispMode.MsgBox);
        });
    }

    private void testConnection() {
        if (!updatePowerStatus()) {
            showErrorMsg("WAN電源がOFFです。", MsgDispMode.MsgBox);
            return;
        }

        connectionCreated = true;
        if (!isCellularConnected()) {
            showErrorMsg("通信に接続できませんでした。", MsgDispMode.MsgBox);
            gprsConnected = false;
            updateGprsStatus();
            return;
        }

        gprsConnected = true;
        updateGprsStatus();
        showInfoMsg("接続しました", MsgDispMode.MsgBox);
        testSuccess = true;
    }

    private void disconnect() {
        if (!gprsConnected) {
            showInfoMsg("未接続です。", MsgDispMode.MsgBox);
            return;
        }
        gprsConnected = false;
        updateGprsStatus();
        showInfoMsg("切断しました", MsgDispMode.MsgBox);
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

        boolean hasCellular = false;
        Network active = manager.getActiveNetwork();
        if (active != null) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
            if (capabilities != null) {
                hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }

        if (hasCellular) {
            tvWanPowerValue.setText(STATUS_ON);
            tvWanPowerValue.setTextColor(Color.parseColor("#006400"));
            return true;
        }

        tvWanPowerValue.setText(STATUS_OFF);
        tvWanPowerValue.setTextColor(Color.BLACK);
        return false;
    }

    private boolean isCellularConnected() {
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
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
