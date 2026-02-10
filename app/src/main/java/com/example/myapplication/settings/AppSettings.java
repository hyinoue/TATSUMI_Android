package com.example.myapplication.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * アプリケーション設定クラス（Android版）
 * C# WinCE AppSettings の移植 + INIファイル対応
 */
public final class AppSettings {

    // ================================
    // SharedPreferences
    // ================================
    private static final String PREF_NAME = "AppSettings";
    private static final String INI_FILE_NAME = "TatsumiHandy.ini";

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
    private static Context appContext;
    private static File iniFile;

    // ================================
    // 初期化（Application / Activityで1回）
    // ================================
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        pref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        File baseDir = resolveIniBaseDir();
        iniFile = new File(baseDir, INI_FILE_NAME);
        copyIniFromAssetsIfNeeded();
    }

    private static File resolveIniBaseDir() {
        // APK配布後の実機では、アプリ専用外部領域を優先（永続性と運用のしやすさ重視）
        File appExternalDir = appContext.getExternalFilesDir(null);
        if (appExternalDir != null) {
            return appExternalDir;
        }

        // 取得不可時のみ内部領域へフォールバック
        return appContext.getFilesDir();
    }

    /**
     * 初回のみ assets/TatsumiHandy.ini をコピーする。
     * 2回目以降は既存ファイルを保持して、save() で更新していく。
     */
    private static void copyIniFromAssetsIfNeeded() {
        if (iniFile.exists()) {
            return;
        }

        File parent = iniFile.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        try (InputStream in = appContext.getAssets().open(INI_FILE_NAME);
             FileOutputStream out = new FileOutputStream(iniFile, false)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } catch (IOException ignored) {
            // assets に無い場合やコピー失敗時は従来どおり継続
        }
    }

    // ================================
    // Load / Save
    // ================================
    public static void load() {
        ensureInitialized();

        // まずSharedPreferencesから読み込み（既存互換）
        loadFromPreferences();

        // INIが存在する場合はINIを優先して上書き
        if (iniFile.exists()) {
            loadFromIni();
        }

        // C#版同様のデフォルト補正
        applyDefaultValues();
    }

    public static void save() {
        ensureInitialized();

        // SharedPreferencesへ保存
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

        e.putString(KEY_WEBSVC_HONBAN, WebSvcURL_Honban);
        e.putString(KEY_WEBSVC_SCS, WebSvcURL_SCS);
        e.putString(KEY_WEBSVC_TEST, WebSvcURL_Test);

        e.apply();

        // INIへも保存
        saveToIni();
    }

    private static void loadFromPreferences() {
        BuzzerMute = pref.getBoolean(KEY_BUZZER_MUTE, false);
        BuzzerLength = pref.getInt(KEY_BUZZER_LENGTH, 0);
        BuzzerVolume = pref.getInt(KEY_BUZZER_VOLUME, 0);

        VibratorMute = pref.getBoolean(KEY_VIBRATOR_MUTE, false);
        VibratorLength = pref.getInt(KEY_VIBRATOR_LENGTH, 0);
        VibratorCount = pref.getInt(KEY_VIBRATOR_COUNT, 0);
        VibratorInterval = pref.getInt(KEY_VIBRATOR_INTERVAL, 0);

        CameraImageSize = pref.getInt(KEY_CAMERA_SIZE, 0);
        CameraFlash = pref.getInt(KEY_CAMERA_FLASH, 0);
        CameraLightMode = pref.getInt(KEY_CAMERA_LIGHT, 0);

        CommName = pref.getString(KEY_COMM_NAME, "");
        CommApn = pref.getString(KEY_COMM_APN, "");
        CommUser = pref.getString(KEY_COMM_USER, "");
        CommPasswd = pref.getString(KEY_COMM_PWD, "");

        WebSvcURL_Honban = pref.getString(KEY_WEBSVC_HONBAN, "");
        WebSvcURL_SCS = pref.getString(KEY_WEBSVC_SCS, "");
        WebSvcURL_Test = pref.getString(KEY_WEBSVC_TEST, "");
    }

    private static void loadFromIni() {
        Map<String, String> map = readIni();

        BuzzerMute = parseBoolean(map.get(KEY_BUZZER_MUTE), BuzzerMute);
        BuzzerLength = parseInt(map.get(KEY_BUZZER_LENGTH), BuzzerLength);
        BuzzerVolume = parseInt(map.get(KEY_BUZZER_VOLUME), BuzzerVolume);

        VibratorMute = parseBoolean(map.get(KEY_VIBRATOR_MUTE), VibratorMute);
        VibratorLength = parseInt(map.get(KEY_VIBRATOR_LENGTH), VibratorLength);
        VibratorCount = parseInt(map.get(KEY_VIBRATOR_COUNT), VibratorCount);
        VibratorInterval = parseInt(map.get(KEY_VIBRATOR_INTERVAL), VibratorInterval);

        CameraImageSize = parseInt(map.get(KEY_CAMERA_SIZE), CameraImageSize);
        CameraFlash = parseInt(map.get(KEY_CAMERA_FLASH), CameraFlash);
        CameraLightMode = parseInt(map.get(KEY_CAMERA_LIGHT), CameraLightMode);

        CommName = parseString(map.get(KEY_COMM_NAME), CommName);
        CommApn = parseString(map.get(KEY_COMM_APN), CommApn);
        CommUser = parseString(map.get(KEY_COMM_USER), CommUser);
        CommPasswd = parseString(map.get(KEY_COMM_PWD), CommPasswd);

        WebSvcURL_Honban = parseString(map.get(KEY_WEBSVC_HONBAN), WebSvcURL_Honban);
        WebSvcURL_SCS = parseString(map.get(KEY_WEBSVC_SCS), WebSvcURL_SCS);
        WebSvcURL_Test = parseString(map.get(KEY_WEBSVC_TEST), WebSvcURL_Test);
    }

    private static void saveToIni() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(KEY_BUZZER_MUTE, String.valueOf(BuzzerMute));
        map.put(KEY_BUZZER_LENGTH, String.valueOf(BuzzerLength));
        map.put(KEY_BUZZER_VOLUME, String.valueOf(BuzzerVolume));

        map.put(KEY_VIBRATOR_MUTE, String.valueOf(VibratorMute));
        map.put(KEY_VIBRATOR_LENGTH, String.valueOf(VibratorLength));
        map.put(KEY_VIBRATOR_COUNT, String.valueOf(VibratorCount));
        map.put(KEY_VIBRATOR_INTERVAL, String.valueOf(VibratorInterval));

        map.put(KEY_CAMERA_SIZE, String.valueOf(CameraImageSize));
        map.put(KEY_CAMERA_FLASH, String.valueOf(CameraFlash));
        map.put(KEY_CAMERA_LIGHT, String.valueOf(CameraLightMode));

        map.put(KEY_COMM_NAME, nullToEmpty(CommName));
        map.put(KEY_COMM_APN, nullToEmpty(CommApn));
        map.put(KEY_COMM_USER, nullToEmpty(CommUser));
        map.put(KEY_COMM_PWD, nullToEmpty(CommPasswd));

        map.put(KEY_WEBSVC_HONBAN, nullToEmpty(WebSvcURL_Honban));
        map.put(KEY_WEBSVC_SCS, nullToEmpty(WebSvcURL_SCS));
        map.put(KEY_WEBSVC_TEST, nullToEmpty(WebSvcURL_Test));

        writeIni(map);
    }

    private static Map<String, String> readIni() {
        Map<String, String> result = new LinkedHashMap<>();
        if (!iniFile.exists()) {
            return result;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(iniFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx < 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                result.put(key, value);
            }
        } catch (IOException ignored) {
            // 読み込み失敗時は既存値を維持
        }

        return result;
    }

    private static void writeIni(Map<String, String> map) {
        File parent = iniFile.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(iniFile, false), StandardCharsets.UTF_8))) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue());
                bw.newLine();
            }
        } catch (IOException ignored) {
            // 保存失敗時はSharedPreferencesがあるため継続
        }
    }

    private static void applyDefaultValues() {
        if (BuzzerLength == 0) BuzzerLength = 1000;
        if (BuzzerVolume == 0) BuzzerVolume = 1;
        if (VibratorLength == 0) VibratorLength = 500;
        if (VibratorCount == 0) VibratorCount = 2;
        if (VibratorInterval == 0) VibratorInterval = 100;
        if (CommName == null || CommName.isEmpty()) CommName = "docomo";
        if (CommApn == null || CommApn.isEmpty()) CommApn = "mopera.net";
        if (CommUser == null) CommUser = "";
        if (CommPasswd == null) CommPasswd = "";
        if (WebSvcURL_Honban == null) WebSvcURL_Honban = "";
        if (WebSvcURL_SCS == null) WebSvcURL_SCS = "";
        if (WebSvcURL_Test == null) WebSvcURL_Test = "";
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        if ("1".equals(value)) return true;
        if ("0".equals(value)) return false;
        return Boolean.parseBoolean(value);
    }

    private static String parseString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void ensureInitialized() {
        if (pref == null || appContext == null || iniFile == null) {
            throw new IllegalStateException("AppSettings.init(context) を先に呼び出してください");
        }
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
    private AppSettings() {
    }
}