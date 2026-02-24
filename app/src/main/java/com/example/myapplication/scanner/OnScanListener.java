package com.example.myapplication.scanner;

import androidx.annotation.Nullable;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　OnScanListener ..... スキャン結果通知用コールバック
//　　　　　　:　onScan ..... スキャン受信時の通知
//============================================================

//===============================
//　処理概要　:　OnScanListenerクラス
//===============================
public interface OnScanListener {

    //========================================
    //　機　能　:　スキャン受信時の処理
    //　引　数　:　normalizedData ..... String（正規化済みバーコード文字列：CR/LF除去+trim）
    //　　　　　:　aim ..... String（AIM ID：例 "]A0" 等）※nullの可能性あり
    //　　　　　:　denso ..... String（Denso側シンボロジ名：例 "CODE39" 等）※nullの可能性あり
    //　戻り値　:　[void] ..... なし
    //========================================
    void onScan(String normalizedData, @Nullable String aim, @Nullable String denso);
}