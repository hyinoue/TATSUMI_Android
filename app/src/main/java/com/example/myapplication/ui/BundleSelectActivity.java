package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class BundleSelectActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bundleselect);

        // ==============================
        // メニューで選んだコンテナサイズに応じて重量を自動セット
        // 20ft -> 2400, 40ft -> 3000
        // ==============================
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");

        int weight = "40ft".equals(size) ? 3000 : 2400;

        EditText etContainerKg = findViewById(R.id.etContainerKg);
        etContainerKg.setText(String.valueOf(weight));


        // ▼ 下ボタン（include）を取得
        View bottom = findViewById(R.id.includeBottomButtons);

        // ▼ 各ボタンを取得
        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        // ▼ 文字設定（画面ごとにここだけ変える）
        btnBlue.setText("確定");
        btnRed.setText("束クリア");
        btnGreen.setText("");
        btnYellow.setText("終了");

        btnYellow.setOnClickListener(v -> finish());

    }
}
