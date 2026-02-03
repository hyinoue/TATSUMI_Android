package com.example.myapplication.activity;

import android.os.Bundle;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;


//==================================
//　処理概要　:　SystemLibActivityクラス
//==================================

/**
 * システムライブラリ関連の画面Activity。
 *
 * <p>現状は終了処理のみを持つダミー画面であり、
 * 実際のライブラリ設定/情報画面を実装するための枠として利用する。</p>
 */
public class SystemLibActivity extends BaseActivity {

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_lib);
        // 画面に留まらず即時終了する（現状はプレースホルダのため）
        onFunctionYellow();
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
