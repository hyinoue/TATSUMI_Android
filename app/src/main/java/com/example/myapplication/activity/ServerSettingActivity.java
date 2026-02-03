package com.example.myapplication.activity;

import android.os.Bundle;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;


//======================================
//　処理概要　:　ServerSettingActivityクラス
//======================================

/**
 * サーバ設定画面Activity。
 *
 * <p>現状は設定UI表示と終了ボタンのみを扱う。</p>
 * <p>将来的なサーバ接続先/同期設定の入力画面として拡張される前提。</p>
 */
public class ServerSettingActivity extends BaseActivity {

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_setting);
        setupBottomButtons();
    }
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

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

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }
}
