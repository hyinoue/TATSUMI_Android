package com.example.myapplication.settings;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　HandyUtil ..... 端末ユーティリティ（チェックデジット/ブザー/バイブ）
//　　　　　　:　calcCheckDigit ..... ISO 6346 コンテナNo.チェックデジット計算
//　　　　　　:　playErrorBuzzer ..... エラーブザー再生
//　　　　　　:　playSuccessBuzzer ..... 成功ブザー再生
//　　　　　　:　playVibrater ..... バイブ再生（設定値で回数/長さ/間隔制御）
//　　　　　　:　playBuzzer ..... ブザー再生（内部共通）
//　　　　　　:　charToCode ..... ISO 6346 文字→コード変換（重み付け計算用）
//============================================================

//==========================
//　処理概要　:　HandyUtilクラス
//==========================
public class HandyUtil {

    private static final String TAG = "HandyUtil";

    //=================================
    //　機　能　:　HandyUtilの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[HandyUtil] ..... なし
    //=================================
    private HandyUtil() {
        // Utilityクラスのためインスタンス化しない
    }

    /**
     * ISO 6346 コンテナNo.のチェックデジット計算
     */
    //=====================================
    //　機　能　:　calc Check Digitの処理
    //　引　数　:　containerNo ..... String
    //　戻り値　:　[String] ..... チェックデジット（不正時は空文字）
    //=====================================
    @NonNull
    public static String calcCheckDigit(String containerNo) {

        // null/空は不正として空文字
        if (containerNo == null || containerNo.trim().isEmpty()) {
            return "";
        }

        // 前後空白除去＋大文字化
        String value = containerNo.trim().toUpperCase();

        // ISO 6346: 文字コード * 2^i を総和
        int sum = 0;
        for (int i = 0; i < value.length(); i++) {

            // 1文字をコードへ変換
            int code = charToCode(value.charAt(i));
            if (code < 0) {
                // 対象外文字が含まれる場合は不正
                return "";
            }

            // 重み（2^i）を掛けて加算
            sum += code * (1 << i);
        }

        // 11で割った余り（10は0扱い）
        int check = sum % 11;
        if (check == 10) check = 0;

        return String.valueOf(check);
    }

    //==================================
    //　機　能　:　play Error Buzzerの処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[void] ..... なし
    //==================================
    public static void playErrorBuzzer(@NonNull Context context) {
        // エラー音（NACK）を再生
        playBuzzer(context, ToneGenerator.TONE_PROP_NACK);
    }

    //===================================
    //　機　能　:　play Success Buzzerの処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[void] ..... なし
    //===================================
    public static void playSuccessBuzzer(@NonNull Context context) {
        // 成功音（ACK）を再生
        playBuzzer(context, ToneGenerator.TONE_PROP_ACK);
    }

    //==================================
    //　機　能　:　play Vibraterの処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[void] ..... なし
    //==================================
    public static void playVibrater(@NonNull Context context) {
        // 追加回数なしで実行
        playVibrater(context, 0);
    }

    //==================================
    //　機　能　:　play Vibraterの処理
    //　引　数　:　context ..... Context
    //　　　　　:　extraCount ..... int（設定値に加算する回数）
    //　戻り値　:　[void] ..... なし
    //==================================
    public static void playVibrater(@NonNull Context context, int extraCount) {

        // 設定でミュートなら何もしない
        if (AppSettings.VibratorMute) return;

        // 回数/長さ/間隔を設定値から取得（安全側に補正）
        int baseCount = Math.max(1, AppSettings.VibratorCount);
        int totalCount = Math.max(1, baseCount + extraCount);
        int length = Math.max(0, AppSettings.VibratorLength);
        int interval = Math.max(0, AppSettings.VibratorInterval);

        // 長さ0なら鳴らさない（ログだけ）
        if (length == 0) {
            Log.w(TAG, "Vibrator length is zero; skipping vibration");
            return;
        }

        // Vibratorサービス取得
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (vibrator == null) return;

        // 波形パターン生成（[待機,振動,待機,振動...]）
        long[] pattern = new long[totalCount * 2];
        for (int i = 0; i < totalCount; i++) {
            // 先頭は即時開始、それ以降はinterval待機
            pattern[i * 2] = (i == 0) ? 0 : interval;
            pattern[i * 2 + 1] = length;
        }

        // OSバージョンでAPIが異なるため分岐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    //==================================
    //　機　能　:　play Buzzerの処理
    //　引　数　:　context ..... Context
    //　　　　　:　toneType ..... int（ToneGenerator定数）
    //　戻り値　:　[void] ..... なし
    //==================================
    private static void playBuzzer(@NonNull Context context, int toneType) {

        // 設定でミュートなら何もしない
        if (AppSettings.BuzzerMute) return;

        // ブザー長（ms）と音量を設定値から取得（安全側に補正）
        int length = Math.max(0, AppSettings.BuzzerLength);

        // 0～10想定の設定値を0～10へ丸め
        int volume = Math.max(0, Math.min(AppSettings.BuzzerVolume, 10));

        // ToneGeneratorは0～100なのでスケール変換（10刻み）
        int toneVolume = Math.max(0, Math.min(volume * 10, 100));

        // ブザー再生（通知ストリーム）
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, toneVolume);
        tone.startTone(toneType, length);

        // 再生後にreleaseしないとリソースリークするため、少し余裕を見て解放
        new Handler(Looper.getMainLooper()).postDelayed(tone::release, length + 50L);
    }

    //============================
    //　機　能　:　char To Codeの処理
    //　引　数　:　c ..... char
    //　戻り値　:　[int] ..... ISO 6346 変換コード（不正は-1）
    //============================
    private static int charToCode(char c) {

        // 数字はそのまま 0～9
        if (c >= '0' && c <= '9') {
            return c - '0';
        }

        // アルファベットはISO 6346の変換表に従う
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
                // 対象外文字
                return -1;
        }
    }
}