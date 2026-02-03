package com.example.myapplication.settings;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * アプリケーション設定クラス（Android版）
 * C# WinCE AppSettings の移植
 */

//======================
//　処理概要　:　AppSettingsクラス
//======================

public final class AppSettings {

    // ================================
    // SharedPreferences
    // ================================
    private static final String PREF_NAME = "AppSettings";

    // ================================
    // Key 定義（C#版と完全一致）
    // ================================
    private static final String KEY_BUZZER_MUTE = "BuzzerMute";
    private static final String KEY_BUZZER_LENGTH = "BuzzerLength";
    private static final String KEY_BUZZER_VOLUME = "BuzzerVolume";

    private static final String KEY_VIBRATOR_MUTE = "VibratorMute";
    private static final String KEY_VIBRATOR_LENGTH = "VibratorLength";
    private static final String KEY_VIBRATOR_COUNT = "VibratorCount";
    private static final String KEY_VIBRATOR_INTERVAL = "VibratorInterval";

    private static final String KEY_CAMERA_SIZE = "CameraImageSize";
    private static final String KEY_CAMERA_FLASH = "CameraFlash";
    private static final String KEY_CAMERA_LIGHT = "CameraLight";

    private static final String KEY_COMM_NAME = "CommName";
    private static final String KEY_COMM_APN = "CommApn";
    private static final String KEY_COMM_USER = "CommUser";
    private static final String KEY_COMM_PWD = "CommPasswd";

    private static final String KEY_WEBSVC_HONBAN = "WebSvcHonban";
    private static final String KEY_WEBSVC_SCS = "WebSvcSCS";
    private static final String KEY_WEBSVC_TEST = "WebSvcTest";

    private static SharedPreferences pref;

    // ================================
    // 初期化（Application / Activityで1回）
    // ================================
    //============================
    //　機　能　:　initの処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[void] ..... なし
    //============================
    public static void init(Context context) {
        pref = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ================================
    // Load / Save
    // ================================
    //======================
    //　機　能　:　loadの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    public static void load() {
        BuzzerMute = pref.getBoolean(KEY_BUZZER_MUTE, false);
        BuzzerLength = pref.getInt(KEY_BUZZER_LENGTH, 1000);
        BuzzerVolume = pref.getInt(KEY_BUZZER_VOLUME, 5);

        VibratorMute = pref.getBoolean(KEY_VIBRATOR_MUTE, false);
        VibratorLength = pref.getInt(KEY_VIBRATOR_LENGTH, 500);
        VibratorCount = pref.getInt(KEY_VIBRATOR_COUNT, 2);
        VibratorInterval = pref.getInt(KEY_VIBRATOR_INTERVAL, 100);

        CameraImageSize = pref.getInt(KEY_CAMERA_SIZE, 0);
        CameraFlash = pref.getInt(KEY_CAMERA_FLASH, 0);
        CameraLightMode = pref.getInt(KEY_CAMERA_LIGHT, 0);

        CommName = pref.getString(KEY_COMM_NAME, "docomo");
        CommApn = pref.getString(KEY_COMM_APN, "mopera.net");
        CommUser = pref.getString(KEY_COMM_USER, "");
        CommPasswd = pref.getString(KEY_COMM_PWD, "");

        WebSvcURL_Honban = pref.getString(KEY_WEBSVC_HONBAN, "");
        WebSvcURL_SCS = pref.getString(KEY_WEBSVC_SCS, "");
        WebSvcURL_Test = pref.getString(KEY_WEBSVC_TEST, "");
    }
    //======================
    //　機　能　:　saveの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================

    public static void save() {
        SharedPreferences.Editor e = pref.edit();

        e.putBoolean(KEY_BUZZER_MUTE, BuzzerMute);
        e.putInt(KEY_BUZZER_LENGTH, BuzzerLength);
        e.putInt(KEY_BUZZER_VOLUME, BuzzerVolume);

        e.putBoolean(KEY_VIBRATOR_MUTE, VibratorMute);
        e.putInt(KEY_VIBRATOR_LENGTH, VibratorLength);
        e.putInt(KEY_VIBRATOR_COUNT, VibratorCount);
        e.putInt(KEY_VIBRATOR_INTERVAL, VibratorInterval);

        e.putInt(KEY_CAMERA_SIZE, CameraImageSize);
        e.putInt(KEY_CAMERA_FLASH, CameraFlash);
        e.putInt(KEY_CAMERA_LIGHT, CameraLightMode);

        e.putString(KEY_COMM_NAME, CommName);
        e.putString(KEY_COMM_APN, CommApn);
        e.putString(KEY_COMM_USER, CommUser);
        e.putString(KEY_COMM_PWD, CommPasswd);

        e.apply();
    }

    // ================================
    // 設定値（C# static property 相当）
    // ================================
    public static boolean BuzzerMute;
    public static int BuzzerLength;
    public static int BuzzerVolume;

    public static boolean VibratorMute;
    public static int VibratorLength;
    public static int VibratorCount;
    public static int VibratorInterval;

    public static int CameraImageSize;
    public static int CameraFlash;
    public static int CameraLightMode;

    public static String CommName;
    public static String CommApn;
    public static String CommUser;
    public static String CommPasswd;

    public static String WebSvcURL_Honban;
    public static String WebSvcURL_SCS;
    public static String WebSvcURL_Test;

    // インスタンス化禁止
    //=============================
    //　機　能　:　AppSettingsの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[AppSettings] ..... なし
    //=============================
    private AppSettings() {
    }
}
