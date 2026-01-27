package com.example.myapplication.scanner;

import androidx.annotation.Nullable;

/**
 * スキャン結果を画面へ返すコールバック
 */
public interface OnScanListener {

    /**
     * スキャン成功時に呼ばれる
     *
     * @param normalizedData 正規化済みバーコード文字列（CR/LF除去+trim）
     * @param aim            AIM ID（例："]A0" 等）nullの可能性あり
     * @param denso          Denso側シンボロジ名（例："CODE39" 等）nullの可能性あり
     */
    void onScan(String normalizedData, @Nullable String aim, @Nullable String denso);
}
