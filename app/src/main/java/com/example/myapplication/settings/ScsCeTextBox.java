package com.example.myapplication.settings;

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

//=============================
//　処理概要　:　ScsCeTextBoxクラス
//=============================

public class ScsCeTextBox extends EditText {
    public interface OnValueChangedListener {
        //=====================================
        //　機　能　:　on Value Changedの処理
        //　引　数　:　view ..... ScsCeTextBox
        //　　　　　:　beforeValue ..... String
        //　戻り値　:　[void] ..... なし
        //=====================================
        void onValueChanged(ScsCeTextBox view, String beforeValue);
    }

    private boolean highlightText;
    private boolean enterNext;
    private String focusedValue = "";
    private boolean isValidating;
    private OnValueChangedListener valueChangedListener;
    //====================================
    //　機　能　:　ScsCeTextBoxの初期化処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[ScsCeTextBox] ..... なし
    //====================================

    public ScsCeTextBox(Context context) {
        super(context);
        init();
    }
    //=====================================
    //　機　能　:　ScsCeTextBoxの初期化処理
    //　引　数　:　context ..... Context
    //　　　　　:　attrs ..... AttributeSet
    //　戻り値　:　[ScsCeTextBox] ..... なし
    //=====================================

    public ScsCeTextBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    //=====================================
    //　機　能　:　ScsCeTextBoxの初期化処理
    //　引　数　:　context ..... Context
    //　　　　　:　attrs ..... AttributeSet
    //　　　　　:　defStyleAttr ..... int
    //　戻り値　:　[ScsCeTextBox] ..... なし
    //=====================================

    public ScsCeTextBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    //============================
    //　機　能　:　initの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void init() {
        setOnFocusChangeListener(this::handleFocusChange);
        setOnEditorActionListener(this::handleEditorAction);
        addTextChangedListener(new TextWatcher() {
            //===================================
            //　機　能　:　before Text Changedの処理
            //　引　数　:　s ..... CharSequence
            //　　　　　:　start ..... int
            //　　　　　:　count ..... int
            //　　　　　:　after ..... int
            //　戻り値　:　[void] ..... なし
            //===================================
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            //=================================
            //　機　能　:　on Text Changedの処理
            //　引　数　:　s ..... CharSequence
            //　　　　　:　start ..... int
            //　　　　　:　before ..... int
            //　　　　　:　count ..... int
            //　戻り値　:　[void] ..... なし
            //=================================
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            //==================================
            //　機　能　:　after Text Changedの処理
            //　引　数　:　s ..... Editable
            //　戻り値　:　[void] ..... なし
            //==================================
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
    //===================================
    //　機　能　:　focus Changeを処理する
    //　引　数　:　view ..... View
    //　　　　　:　hasFocus ..... boolean
    //　戻り値　:　[void] ..... なし
    //===================================

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
    //=================================
    //　機　能　:　editor Actionを処理する
    //　引　数　:　v ..... TextView
    //　　　　　:　actionId ..... int
    //　　　　　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... なし
    //=================================

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
    //========================================
    //　機　能　:　highlight Textを設定する
    //　引　数　:　highlightText ..... boolean
    //　戻り値　:　[void] ..... なし
    //========================================

    public void setHighlightText(boolean highlightText) {
        this.highlightText = highlightText;
    }
    //================================
    //　機　能　:　highlight Textを判定する
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //================================

    public boolean isHighlightText() {
        return highlightText;
    }
    //====================================
    //　機　能　:　enter Nextを設定する
    //　引　数　:　enterNext ..... boolean
    //　戻り値　:　[void] ..... なし
    //====================================

    public void setEnterNext(boolean enterNext) {
        this.enterNext = enterNext;
    }
    //===============================
    //　機　能　:　enter Nextを判定する
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //===============================

    public boolean isEnterNext() {
        return enterNext;
    }
    //==================================================
    //　機　能　:　on Value Changed Listenerを設定する
    //　引　数　:　listener ..... OnValueChangedListener
    //　戻り値　:　[void] ..... なし
    //==================================================

    public void setOnValueChangedListener(@Nullable OnValueChangedListener listener) {
        this.valueChangedListener = listener;
    }
}
