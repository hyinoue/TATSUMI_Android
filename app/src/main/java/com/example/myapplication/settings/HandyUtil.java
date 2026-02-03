package com.example.myapplication.settings;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;


//============================================================
//　処理概要　:　HandyUtilクラス
//============================================================

public class HandyUtil {

    private HandyUtil() {
    }

    /**
     * ISO 6346 コンテナ番号のチェックデジット計算
     */
    @NonNull
    public static String calcCheckDigit(String containerNo) {
        if (containerNo == null || containerNo.trim().isEmpty()) {
            return "";
        }

        String value = containerNo.trim().toUpperCase();
        int sum = 0;
        for (int i = 0; i < value.length(); i++) {
            int code = charToCode(value.charAt(i));
            if (code < 0) {
                return "";
            }
            sum += code * (1 << i);
        }

        int check = sum % 11;
        if (check == 10) check = 0;
        return String.valueOf(check);
    }

    public static void playErrorBuzzer(@NonNull Context context) {
        playBuzzer(context, ToneGenerator.TONE_PROP_NACK);
    }

    public static void playSuccessBuzzer(@NonNull Context context) {
        playBuzzer(context, ToneGenerator.TONE_PROP_ACK);
    }

    public static void playVibrater(@NonNull Context context) {
        playVibrater(context, 0);
    }

    public static void playVibrater(@NonNull Context context, int extraCount) {
        if (AppSettings.VibratorMute) return;

        int baseCount = Math.max(1, AppSettings.VibratorCount);
        int totalCount = baseCount + Math.max(0, extraCount);
        int length = Math.max(0, AppSettings.VibratorLength);
        int interval = Math.max(0, AppSettings.VibratorInterval);

        Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (vibrator == null) return;

        long[] pattern = new long[totalCount * 2];
        for (int i = 0; i < totalCount; i++) {
            pattern[i * 2] = (i == 0) ? 0 : interval;
            pattern[i * 2 + 1] = length;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private static void playBuzzer(@NonNull Context context, int toneType) {
        if (AppSettings.BuzzerMute) return;

        int length = Math.max(0, AppSettings.BuzzerLength);
        int volume = Math.max(0, Math.min(AppSettings.BuzzerVolume, 10));
        int toneVolume = Math.max(0, Math.min(volume * 10, 100));

        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, toneVolume);
        tone.startTone(toneType, length);
        new Handler(Looper.getMainLooper()).postDelayed(tone::release, length + 50L);
    }

    private static int charToCode(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }

        switch (c) {
            case 'A':
                return 10;
            case 'B':
                return 12;
            case 'C':
                return 13;
            case 'D':
                return 14;
            case 'E':
                return 15;
            case 'F':
                return 16;
            case 'G':
                return 17;
            case 'H':
                return 18;
            case 'I':
                return 19;
            case 'J':
                return 20;
            case 'K':
                return 21;
            case 'L':
                return 23;
            case 'M':
                return 24;
            case 'N':
                return 25;
            case 'O':
                return 26;
            case 'P':
                return 27;
            case 'Q':
                return 28;
            case 'R':
                return 29;
            case 'S':
                return 30;
            case 'T':
                return 31;
            case 'U':
                return 32;
            case 'V':
                return 34;
            case 'W':
                return 35;
            case 'X':
                return 36;
            case 'Y':
                return 37;
            case 'Z':
                return 38;
            default:
                return -1;
        }
    }
}
