package com.example.myapplication.scanner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * C#版 ImageScanTextBox 相当の Android 実装。
 *
 * <p>フォーカス取得中のみスキャナを有効化し、トリガーキー押下で読み取りを行う。</p>
 */

//======================================
//　処理概要　:　ImageScanTextBoxクラス
//======================================

public class ImageScanTextBox extends AppCompatEditText {

    private static final int DEFAULT_WAIT_DECODE_MS = 5000;

    private int minLength = 0;
    private long waitDecodeMs = DEFAULT_WAIT_DECODE_MS;

    @Nullable
    private DensoScannerController scannerController;
    private boolean scannerCreated = false;

    public ImageScanTextBox(@NonNull Context context) {
        super(context);
        init();
    }

    public ImageScanTextBox(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageScanTextBox(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    //============================
    //　機　能　:　初期化処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void init() {
        setShowSoftInputOnFocus(false);
    }

    //============================
    //　機　能　:　フォーカス取得時の処理
    //　引　数　:　focused ..... boolean
    //　　　　　:　direction ..... int
    //　　　　　:　previouslyFocusedRect ..... Rect
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onFocusChanged(boolean focused, int direction, @Nullable android.graphics.Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (focused) {
            ensureScannerReady();
        } else {
            pauseScanner();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        destroyScanner();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isTriggerKey(keyCode) && scannerController != null) {
            scannerController.handleDispatchKeyEvent(event);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isTriggerKey(keyCode) && scannerController != null) {
            scannerController.handleDispatchKeyEvent(event);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    //============================
    //　機　能　:　scannerの準備処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void ensureScannerReady() {
        Activity activity = findActivity(getContext());
        if (activity == null) return;

        if (scannerController == null) {
            scannerController = new DensoScannerController(activity, (normalizedData, aim, denso) -> {
                if (TextUtils.isEmpty(normalizedData)) return;

                int effectiveMinLength = minLength <= 0 ? 1 : minLength;
                if (normalizedData.length() < effectiveMinLength) return;

                int maxLength = resolveMaxLength();
                String value = normalizedData;
                if (maxLength > 0 && value.length() > maxLength) {
                    value = value.substring(0, maxLength);
                }

                setText(value);
                setSelection(value.length());

                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            });
            scannerController.setWaitDecodeMs(waitDecodeMs);
        }

        if (!scannerCreated) {
            scannerController.onCreate();
            scannerCreated = true;
        }
        scannerController.onResume();
    }

    //============================
    //　機　能　:　scanner一時停止処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void pauseScanner() {
        if (scannerController != null) {
            scannerController.onPause();
        }
    }

    //============================
    //　機　能　:　scanner破棄処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void destroyScanner() {
        if (scannerController != null) {
            scannerController.onDestroy();
            scannerController = null;
        }
        scannerCreated = false;
    }

    private boolean isTriggerKey(int keyCode) {
        return keyCode == 234 || keyCode == 230 || keyCode == 233;
    }

    private int resolveMaxLength() {
        InputFilter[] filters = getFilters();
        if (filters == null) return 0;

        for (InputFilter filter : filters) {
            if (filter instanceof InputFilter.LengthFilter) {

                CharSequence current = getText();

                CharSequence out = filter.filter(
                        "A",
                        0,
                        1,
                        getText(),                 // そのまま渡す
                        0,
                        current.length()
                );

                if (out != null && out.length() == 0) {
                    return current.length();
                }
            }
        }

        return 0;
    }

    @Nullable
    private Activity findActivity(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    /// <summary>
    /// バーコード読み取りの最短文字列長
    /// </summary>
    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = Math.max(0, minLength);
    }

    public long getWaitDecodeMs() {
        return waitDecodeMs;
    }

    public void setWaitDecodeMs(long waitDecodeMs) {
        this.waitDecodeMs = waitDecodeMs <= 0 ? DEFAULT_WAIT_DECODE_MS : waitDecodeMs;
        if (scannerController != null) {
            scannerController.setWaitDecodeMs(this.waitDecodeMs);
        }
    }
}