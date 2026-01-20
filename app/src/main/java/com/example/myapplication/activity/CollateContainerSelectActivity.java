package com.example.myapplication.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;


public class CollateContainerSelectActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collate_container_select);

        // ▼ 下ボタン（include）を取得
        View bottom = findViewById(R.id.includeBottomButtons);

        // ▼ 各ボタンを取得
        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        // ▼ 文字設定（画面ごとにここだけ変える）
        btnBlue.setText("決定");
        btnRed.setText("");
        btnGreen.setText("");
        btnYellow.setText("終了");

        btnBlue.setOnClickListener(v -> {
            Intent intent = new Intent(CollateContainerSelectActivity.this, VanningCollationActivity.class);
            startActivity(intent);
        });
        btnYellow.setOnClickListener(v -> finish());
    }

    // ==============================
    // 4色ボタンの実処理（タップも物理キーもここに集約）
    // ==============================
    @Override
    protected void onFunctionBlue() {
        // 確定
        // TODO: 確定処理をここに実装
        showInfoMsg("確定（未実装）", MsgDispMode.Label);
    }

    @Override
    protected void onFunctionRed() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    @Override
    protected void onFunctionGreen() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    @Override
    protected void onFunctionYellow() {
        // 終了
        finish();
    }
}
