package com.example.myapplication.ui;


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
}
