// BundleSelectActivity.java
package com.example.myapplication.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class BundleSelectActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bundle_select);

        // 初期表示：コンテナ重量セット（20ft/40ft）
        applyContainerDefaultWeight();

        // 下ボタン文言（画面ごと）
        setBottomButtonTexts();

        // 文言を入れたので、押せる/押せない（空は無効＋薄く）を更新
        refreshBottomButtonsEnabled();
    }

    // ==============================
    // 初期表示：コンテナ重量セット
    // ==============================
    private void applyContainerDefaultWeight() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        int weight = "40ft".equals(size) ? 3000 : 2400;

        EditText etContainerKg = findViewById(R.id.etContainerKg);
        if (etContainerKg != null) {
            etContainerKg.setText(String.valueOf(weight));
        }
    }

    // ==============================
    // 下ボタン文言設定（画面ごと）
    // ==============================
    private void setBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (blue != null) blue.setText("確定");
        if (red != null) red.setText("束クリア");
        if (green != null) green.setText("");      // 空なら押せない（BaseActivityで制御）
        if (yellow != null) yellow.setText("終了");
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
        // 束クリア
        // TODO: 束クリア処理をここに実装
        showWarningMsg("束クリア（未実装）", MsgDispMode.Label);
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
