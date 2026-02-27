package com.example.myapplication.scanner;

import androidx.annotation.Nullable;

//============================================================
//　処理概要　:　スキャン結果受け取り用コールバックを定義するインターフェース
//　関　　数　:　OnScanListener ..... スキャン結果通知用コールバック
//　　　　　　:　onScan ..... スキャン受信時の通知
//============================================================

public interface OnScanListener {

    //============================================================
    //　機　能　:　スキャン受信時の処理
    //　引　数　:　normalizedData ..... 正規化済みスキャンデータ
    //　　　　　:　aim ..... AIMスキャンデータ
    //　　　　　:　denso ..... DENSOスキャンデータ
    //　戻り値　:　[void] ..... なし
    //============================================================
    void onScan(String normalizedData, @Nullable String aim, @Nullable String denso);
}
