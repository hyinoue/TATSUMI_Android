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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * フォーカス中だけ Code39 をアプリ処理する EditText
 * <p>
 * ポイント:
 * - SCANキーはこのViewで消費しない（端末に渡す）
 * - フォーカスON: デコード Code39_ONLY + アプリ処理ON
 * - フォーカスOFF: デコード NONE + アプリ処理OFF
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
        } else {
            pauseScanner();
        }

        // ★フォーカス変化直後に設定反映（CODE39_ONLY / NONE 切替）
        if (scannerController != null) {
            scannerController.refreshProfile("ImageScanTextBox.onFocusChanged");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        destroyScanner();
        super.onDetachedFromWindow();
    }

    /**
     * ★重要：SCANキーをここで消費しない
     * （消費すると端末側のトリガー処理が動かず「反応しない」になりがち）
     * <p>
     * ※なので onKeyDown/onKeyUp はオーバーライドしない（=端末に渡す）
     */

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

                        // 既存処理に流したい場合：Enterを擬似発火
                        dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER));
                        dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER));
                    },
                    new DensoScannerController.ScanPolicy() {
                        @Override
                        public boolean canAcceptResult() {
                            return hasFocus() && isEnabled();
                        }

                        @NonNull
                        @Override
                        public DensoScannerController.SymbologyProfile getSymbologyProfile() {
                            return (hasFocus() && isEnabled())
                                    ? DensoScannerController.SymbologyProfile.CODE39_ONLY
                                    : DensoScannerController.SymbologyProfile.NONE;
                        }

                        @Override
                        public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                            // 念のため Code39 以外は弾く
                            return DensoScannerController.isCode39(aim, denso, displayName);
                        }
                    }
            );
        }

        if (!scannerCreated) {
            scannerController.onCreate();
            scannerCreated = true;
        }
        scannerController.onResume();
        scannerController.refreshProfile("ImageScanTextBox.ensureScannerReady");
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

    private int resolveMaxLength() {
        InputFilter[] filters = getFilters();
        if (filters == null) return 0;

        for (InputFilter filter : filters) {
            if (filter instanceof InputFilter.LengthFilter) {
                Editable current = getText(); // Editable は Spanned
                Spanned dest = (current != null) ? current : new SpannableStringBuilder("");

                CharSequence out = filter.filter(
                        "A",
                        0,
                        1,
                        dest,
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
}
