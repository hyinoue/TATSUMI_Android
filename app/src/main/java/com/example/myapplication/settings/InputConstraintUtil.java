package com.example.myapplication.settings;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.densowave.bhtsdk.hardkeyboardsettings.HardKeyboardSettings;
import com.densowave.bhtsdk.hardkeyboardsettings.HardKeyboardSettingsException;
import com.densowave.bhtsdk.hardkeyboardsettings.HardKeyboardSettingsLibrary;
import com.densowave.bhtsdk.hardkeyboardsettings.INPUT_MODE;

//===================================
//　処理概要　:　InputConstraintUtilクラス
//===================================

public final class InputConstraintUtil {

    private static final String TAG = "InputConstraintUtil";

    @Nullable
    private static volatile HardKeyboardSettingsLibrary hardKeyboardSettingsLibrary;
    private static volatile boolean creatingHardKeyboardLibrary;

    private InputConstraintUtil() {
    }

    //===================================
    //　機　能　:　英字入力(大文字化)を設定する
    //　引　数　:　target ..... EditText
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void applyAlphabetUpper(@Nullable EditText target) {
        if (target == null) return;

        target.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        target.setKeyListener(DigitsKeyListener.getInstance("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"));
        appendInputFilter(target, new InputFilter.AllCaps());
    }

    //=============================
    //　機　能　:　数字入力を設定する
    //　引　数　:　target ..... EditText
    //　戻り値　:　[void] ..... なし
    //=============================
    public static void applyDigitsOnly(@Nullable EditText target) {
        if (target == null) return;

        target.setInputType(InputType.TYPE_CLASS_NUMBER);
        target.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
    }

    //===================================
    //　機　能　:　フォーカス時IME入力モードを設定する
    //　引　数　:　context ..... Context
    //　　　　　:　target ..... EditText
    //　　　　　:　inputMode ..... INPUT_MODE
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void applyHardKeyboardModeOnFocus(@NonNull Context context,
                                                    @Nullable EditText target,
                                                    @NonNull INPUT_MODE inputMode) {
        if (target == null) return;

        target.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) return;
            setHardKeyboardInputMode(context, inputMode);
        });
    }

    //===================================
    //　機　能　:　IME入力モードを設定する
    //　引　数　:　context ..... Context
    //　　　　　:　inputMode ..... INPUT_MODE
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void setHardKeyboardInputMode(@NonNull Context context, @NonNull INPUT_MODE inputMode) {
        executeWithHardKeyboardSettingsLibrary(context, library -> {
            try {
                library.setInputMode(inputMode);
            } catch (HardKeyboardSettingsException e) {
                Log.w(TAG, "setInputMode failed. code=" + e.getErrorCode(), e);
            }
        });
    }

    //===================================
    //　機　能　:　IME入力を数字固定で設定する
    //　引　数　:　context ..... Context
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void setHardKeyboardNumericLocked(@NonNull Context context) {
        executeWithHardKeyboardSettingsLibrary(context, library -> {
            try {
                HardKeyboardSettings settings = library.getSettings();
                settings.shitKeyLockType.set(HardKeyboardSettings.LOCK_TYPE.LOCK);
                settings.functionKeyLockType.set(HardKeyboardSettings.LOCK_TYPE.LOCK);
                library.setSettings(settings);
                library.setInputMode(INPUT_MODE.MODE_NUMERIC);
            } catch (HardKeyboardSettingsException e) {
                Log.w(TAG, "setHardKeyboardNumericLocked failed. code=" + e.getErrorCode(), e);
            }
        });
    }

    private static void executeWithHardKeyboardSettingsLibrary(@NonNull Context context,
                                                               @NonNull HardKeyboardLibraryAction action) {
        HardKeyboardSettingsLibrary library = hardKeyboardSettingsLibrary;
        if (library != null) {
            action.run(library);
            return;
        }

        if (creatingHardKeyboardLibrary) return;
        creatingHardKeyboardLibrary = true;

        try {
            HardKeyboardSettingsLibrary.create(context.getApplicationContext(), created -> {
                hardKeyboardSettingsLibrary = created;
                creatingHardKeyboardLibrary = false;
                action.run(created);
            });
        } catch (HardKeyboardSettingsException e) {
            creatingHardKeyboardLibrary = false;
            Log.w(TAG, "HardKeyboardSettingsLibrary.create failed. code=" + e.getErrorCode(), e);
        }
    }

    private interface HardKeyboardLibraryAction {
        void run(@NonNull HardKeyboardSettingsLibrary library);
    }

    private static void appendInputFilter(@NonNull EditText target, @NonNull InputFilter filter) {
        InputFilter[] current = target.getFilters();
        if (current == null || current.length == 0) {
            target.setFilters(new InputFilter[]{filter});
            return;
        }

        InputFilter[] next = new InputFilter[current.length + 1];
        System.arraycopy(current, 0, next, 0, current.length);
        next[current.length] = filter;
        target.setFilters(next);
    }
}
