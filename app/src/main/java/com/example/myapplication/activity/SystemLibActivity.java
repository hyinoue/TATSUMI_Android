package com.example.myapplication.activity;

import android.os.Bundle;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;


//============================================================
//　処理概要　:　SystemLibActivityクラス
//============================================================

/**
 * システムライブラリ関連の画面Activity。
 *
 * <p>現状は終了処理のみを持つダミー画面であり、
 * 実際のライブラリ設定/情報画面を実装するための枠として利用する。</p>
 */
public class SystemLibActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_lib);
        // 画面に留まらず即時終了する（現状はプレースホルダのため）
        onFunctionYellow();
    }

    private void setupBottomButtons() {
        bindBottomButtonsIfExists();
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        if (yellow != null) {
            yellow.setText("終了");
        }
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
    }

    @Override
    protected void onFunctionYellow() {
        finish();
    }
}
