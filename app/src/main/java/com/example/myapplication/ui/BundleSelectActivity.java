package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class BundleSelectActivity extends BaseActivity {

    // ★ onKeyDown でも使うのでフィールドで保持
    private MaterialButton btnBottomBlue;
    private MaterialButton btnBottomRed;
    private MaterialButton btnBottomGreen;
    private MaterialButton btnBottomYellow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bundle_select);

        // 20ft/40ft で重量セット
        applyContainerDefaultWeight();

        // 下ボタン初期化（includeから取得）
        initBottomButtons();

        // 文言設定（画面ごと）
        setBottomButtonTexts();

        // クリック処理（画面ボタン）
        wireBottomButtonActions();
    }

    // ==============================
    // 初期表示：コンテナ重量セット
    // ==============================
    private void applyContainerDefaultWeight() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");

        int weight = "40ft".equals(size) ? 3000 : 2400;

        EditText etContainerKg = findViewById(R.id.etContainerKg);
        etContainerKg.setText(String.valueOf(weight));
    }

    // ==============================
    // 下ボタン（include）取得
    // ==============================
    private void initBottomButtons() {
        View bottom = findViewById(R.id.includeBottomButtons);

        btnBottomBlue = bottom.findViewById(R.id.btnBottomBlue);
        btnBottomRed = bottom.findViewById(R.id.btnBottomRed);
        btnBottomGreen = bottom.findViewById(R.id.btnBottomGreen);
        btnBottomYellow = bottom.findViewById(R.id.btnBottomYellow);
    }

    // ==============================
    // 文言設定（画面ごとに変更）
    // ==============================
    private void setBottomButtonTexts() {
        btnBottomBlue.setText("確定");
        btnBottomRed.setText("束クリア");
        btnBottomGreen.setText("");
        btnBottomYellow.setText("終了");
    }

    // ==============================
    // クリック処理（画面ボタン）
    // ※ 実処理はメソッドに集約（重要）
    // ==============================
    private void wireBottomButtonActions() {
        btnBottomYellow.setOnClickListener(v -> onYellowButton());
        // 必要なら他も同様に
        // btnBottomBlue.setOnClickListener(v -> onBlueButton());
        // btnBottomRed.setOnClickListener(v -> onRedButton());
        // btnBottomGreen.setOnClickListener(v -> onGreenButton());
    }

    // ==============================
    // 黄色ボタン（終了）の実処理
    // ==============================
    private void onYellowButton() {
        finish();
    }

    // ==============================
    // 物理Fキー → 画面ボタンと同じ動き
    // ==============================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // ★ 長押し連打防止（業務端末で必須）
        if (event.getRepeatCount() > 0) {
            return true;
        }

        switch (keyCode) {

            case KeyEvent.KEYCODE_F1:
                if (btnBottomRed != null) btnBottomRed.performClick();
                return true;

            case KeyEvent.KEYCODE_F2:
                if (btnBottomBlue != null) btnBottomBlue.performClick();
                return true;

            case KeyEvent.KEYCODE_F3:
                if (btnBottomGreen != null) btnBottomGreen.performClick();
                return true;

            case KeyEvent.KEYCODE_F4:
                if (btnBottomYellow != null) btnBottomYellow.performClick();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
