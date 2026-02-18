package com.example.myapplication.scanner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * C#版 ImageScanTextBox 相当の Android 実装。
 * <p>
 * 方針：
 * - フォーカス中だけ Code39 をアプリ処理する
 * - 光/マーカーは端末既定（制御しない）
 * - SCANキー制御もしない（ここでは「アプリに入れる/入れない」だけを制御）
 */
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

    private void init() {
        setShowSoftInputOnFocus(false);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, @Nullable android.graphics.Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (focused) {
            ensureScannerReady();
            if (scannerController != null) {
                // フォーカスONになったので decode=CODE39_ONLY を即反映
                scannerController.refreshProfile("ImageScanTextBoxFocusOn");
            }
        } else {
            if (scannerController != null) {
                // フォーカスOFFになったので decode=NONE を即反映
                scannerController.refreshProfile("ImageScanTextBoxFocusOff");
            }
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
        // SCANキー押下はここで握りつぶす（アプリ側の他処理を起こさない）
        if (isTriggerKey(keyCode)) {
            // ここで何もせずtrue。端末側は光を出すかもしれないが、
            // decode=NONE の時はアプリに結果が入ってこない。
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isTriggerKey(keyCode)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void ensureScannerReady() {
        Activity activity = findActivity(getContext());
        if (activity == null) return;

        if (scannerController == null) {
            scannerController = new DensoScannerController(
                    activity,
                    (normalizedData, aim, denso) -> {
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

                        // Enterを疑似送出（既存仕様）
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                    },
                    // ★このEditTextがフォーカス中だけ Code39 を許可
                    DensoScannerController.createCode39OnFocusPolicy(this)
            );
        }

        if (!scannerCreated) {
            scannerController.onCreate();
            scannerCreated = true;
        }
        scannerController.onResume();
    }

    private void pauseScanner() {
        if (scannerController != null) {
            scannerController.onPause();
        }
    }

    private void destroyScanner() {
        if (scannerController != null) {
            scannerController.onDestroy();
            scannerController = null;
        }
        scannerCreated = false;
    }

    private boolean isTriggerKey(int keyCode) {
        // 端末依存キー（必要なら 501 も追加）
        return keyCode == 234 || keyCode == 230 || keyCode == 233 || keyCode == 501;
    }

    private int resolveMaxLength() {
        InputFilter[] filters = getFilters();
        if (filters == null) return 0;

        for (InputFilter filter : filters) {
            if (filter instanceof InputFilter.LengthFilter) {
                Editable current = getText(); // Editable は Spanned
                Spanned dest = (current != null) ? current : new SpannableStringBuilder("");

                // 「1文字追加しようとした時に弾かれるか」で maxLength 到達を推定
                CharSequence out = filter.filter(
                        "A",          // source
                        0,
                        1,
                        dest,         // ★Spannedを渡す
                        0,
                        dest.length()
                );

                if (out != null && out.length() == 0) {
                    return dest.length();
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
        // ※ waitDecode は現状未使用（残しておくだけ）
    }
}
