package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import com.example.myapplication.R;


//============================================================
//　処理概要　:　PhotographingActivityクラス
//============================================================

public class PhotographingActivity extends BaseActivity {

    private ImageButton btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photographing); // ← あなたのカメラXML

        // 設定アイコン取得
        btnSettings = findViewById(R.id.btnSettings);

        // 設定画面へ遷移
        btnSettings.setOnClickListener(v -> {
            openCameraSetting();
        });
    }

    /**
     * カメラ設定画面を開く
     */
    private void openCameraSetting() {
        Intent intent = new Intent(this, CameraSettingActivity.class);
        startActivity(intent);
    }
}
