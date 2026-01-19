package com.example.myapplication.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * C# の ScsCeTextBox の挙動を Android の EditText で再現した例。
 * - フォーカス取得時に選択（HighlightText）
 * - フォーカス喪失時に値変更イベント（ValueChanged）
 * - Enter キーで次の項目へ移動（EnterNext）
 */
public class ScsCeTextBox extends EditText {
    public interface OnValueChangedListener {
        void onValueChanged(ScsCeTextBox view, String beforeValue);
    }

    private boolean highlightText;
    private boolean enterNext;
    private String focusedValue = "";
    private boolean isValidating;
    private OnValueChangedListener valueChangedListener;

    public ScsCeTextBox(Context context) {
        super(context);
        init();
    }

    public ScsCeTextBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScsCeTextBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnFocusChangeListener(this::handleFocusChange);
        setOnEditorActionListener(this::handleEditorAction);
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSingleLine()) {
                    return;
                }
                String text = s.toString().replace("\r\n", "");
                if (!text.equals(s.toString())) {
                    removeTextChangedListener(this);
                    setText(text);
                    setSelection(text.length());
                    addTextChangedListener(this);
                }
            }
        });
    }

    private void handleFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
            if (!isValidating) {
                focusedValue = getText() != null ? getText().toString() : "";
                if (highlightText) {
                    selectAll();
                }
            }
            return;
        }

        isValidating = true;
        try {
            String current = getText() != null ? getText().toString() : "";
            if (!focusedValue.equals(current) && valueChangedListener != null) {
                valueChangedListener.onValueChanged(this, focusedValue);
            }
        } finally {
            isValidating = false;
        }
    }

    private boolean handleEditorAction(TextView v, int actionId, KeyEvent event) {
        if (!enterNext) {
            return false;
        }

        boolean isEnterKey = actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_DONE
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_DOWN);

        if (!isEnterKey) {
            return false;
        }

        View next = focusSearch(View.FOCUS_FORWARD);
        if (next != null) {
            next.requestFocus();
            return true;
        }
        return false;
    }

    public void setHighlightText(boolean highlightText) {
        this.highlightText = highlightText;
    }

    public boolean isHighlightText() {
        return highlightText;
    }

    public void setEnterNext(boolean enterNext) {
        this.enterNext = enterNext;
    }

    public boolean isEnterNext() {
        return enterNext;
    }

    public void setOnValueChangedListener(@Nullable OnValueChangedListener listener) {
        this.valueChangedListener = listener;
    }
}