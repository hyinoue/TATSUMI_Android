package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class ServiceMenuActivity extends BaseActivity {

    // 画面メニュー（TextView）
    private TextView menu1;
    private TextView menu5;
    private TextView menu6;
    private TextView menu7;
    private TextView menu8; // XMLに menu8（サーバー切替）を追加した場合

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_menu);

        // ▼ 下ボタン（include）を取得
        View bottom = findViewById(R.id.includeBottomButtons);

        // ▼ 各ボタンを取得
        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        // ▼ 文字設定（画面ごとにここだけ変える）
        btnBlue.setText("");
        btnRed.setText("");
        btnGreen.setText("");
        btnYellow.setText("終了");

        btnYellow.setOnClickListener(v -> finish());

        // ▼ メニューTextView取得
        menu1 = findViewById(R.id.menu1);
        menu5 = findViewById(R.id.menu5);
        menu6 = findViewById(R.id.menu6);
        menu7 = findViewById(R.id.menu7);
        // menu8 は XMLに追加している場合のみ存在
        View v8 = findViewById(R.id.menu8);
        if (v8 instanceof TextView) {
            menu8 = (TextView) v8;
        }

        // ▼ 画面タップで遷移
        menu1.setOnClickListener(v -> openDbTest());
        menu5.setOnClickListener(v -> openCommTest());
        menu6.setOnClickListener(v -> openImagerTest());
        menu7.setOnClickListener(v -> openSystemLib());
        if (menu8 != null) {
            menu8.setOnClickListener(v -> openServerSetting());
        }
    }

    /**
     * 物理キー（1/5/6/7/8）で遷移
     * ※端末/キー割当によって拾えるKEYCODEが異なる場合があります。
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                openDbTest();
                return true;

            case KeyEvent.KEYCODE_5:
                openCommTest();
                return true;

            case KeyEvent.KEYCODE_6:
                openImagerTest();
                return true;

            case KeyEvent.KEYCODE_7:
                openSystemLib();
                return true;

            case KeyEvent.KEYCODE_8:
                openServerSetting();
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // -------------------------
    // 画面遷移（ここだけ置換）
    // -------------------------

    private void openDbTest() {
        startActivity(new Intent(this, DbTestActivity.class));
    }

    private void openCommTest() {
        startActivity(new Intent(this, CommTestActivity.class));
    }

    private void openImagerTest() {
        startActivity(new Intent(this, ImagerTestActivity.class));
    }

    private void openSystemLib() {
        startActivity(new Intent(this, SystemLibActivity.class));
    }

    private void openServerSetting() {
        startActivity(new Intent(this, ServerSettingActivity.class));
    }
}
