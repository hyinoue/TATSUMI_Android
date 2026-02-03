package com.example.myapplication.activity;

import android.os.Bundle;

import com.example.myapplication.R;
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
