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


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　AppSettings ..... アプリ設定（SharedPreferences + INI）管理
//　　　　　　:　init ..... 初期化（Context/SharedPreferences/INIパス決定/初回assetsコピー）
//　　　　　　:　load ..... 設定読込（Preferences→INI上書き→デフォルト補正）
//　　　　　　:　save ..... 設定保存（Preferences保存→INI保存）
//　　　　　　:　resolveIniBaseDir ..... INI保存先ディレクトリ決定
//　　　　　　:　copyIniFromAssetsIfNeeded ..... 初回のみassetsからINIをコピー
//　　　　　　:　loadFromPreferences ..... SharedPreferencesから読込
//　　　　　　:　loadFromIni ..... INIから読込して設定へ反映
//　　　　　　:　saveToIni ..... 設定値をINI用Mapへ変換して保存
//　　　　　　:　readIni ..... INI読み込み（key=value）
//　　　　　　:　writeIni ..... INI書き込み（key=value）
//　　　　　　:　applyDefaultValues ..... デフォルト値補正
//　　　　　　:　parseInt ..... int変換（失敗時デフォルト）
//　　　　　　:　parseBoolean ..... boolean変換（"1"/"0"/true/false）
//　　　　　　:　parseString ..... String変換（null時デフォルト）
//　　　　　　:　nullToEmpty ..... null→空文字
//　　　　　　:　ensureInitialized ..... init呼出しチェック
//============================================================
public final class AppSettings {

    // ================================
    // SharedPreferences
    // ================================
    private static final String PREF_NAME = "AppSettings"; // SharedPreferences名
    private static final String INI_FILE_NAME = "TatsumiHandy.ini"; // INIファイル名

    // ================================
    // Key 定義
    // ================================
    private static final String KEY_BUZZER_MUTE = "BuzzerMute";      // ブザーON/OFFキー
    private static final String KEY_BUZZER_LENGTH = "BuzzerLength";  // ブザー長さキー
    private static final String KEY_BUZZER_VOLUME = "BuzzerVolume";  // ブザー音量キー

    private static final String KEY_VIBRATOR_MUTE = "VibratorMute";          // バイブON/OFFキー
    private static final String KEY_VIBRATOR_LENGTH = "VibratorLength";      // バイブ長さキー
    private static final String KEY_VIBRATOR_COUNT = "VibratorCount";        // バイブ回数キー
    private static final String KEY_VIBRATOR_INTERVAL = "VibratorInterval";  // バイブ間隔キー

    private static final String KEY_CAMERA_SIZE = "CameraImageSize"; // カメラサイズキー
    private static final String KEY_CAMERA_FLASH = "CameraFlash";    // カメラフラッシュキー
    private static final String KEY_CAMERA_LIGHT = "CameraLight";    // カメラ露出キー

    private static final String KEY_WEBSVC_HONBAN = "WebSvcHonban"; // 本番WebSvcキー
    private static final String KEY_WEBSVC_SCS = "WebSvcSCS";       // SCS WebSvcキー
    private static final String KEY_WEBSVC_TEST = "WebSvcTest";     // テストWebSvcキー


    private static SharedPreferences pref; // SharedPreferencesインスタンス
    private static Context appContext; // アプリケーションコンテキスト
    private static File iniFile; // INIファイル実体

    // ================================
    // 初期化（Application / Activityで1回）
    // ================================

    //=================================================
    //　機　能　:　AppSettingsを初期化する
    //　引　数　:　context ..... Context
    //　戻り値　:　[void] ..... なし
    //=================================================
    public static void init(Context context) {

        // ApplicationContextを保持（Activityリーク防止）
        appContext = context.getApplicationContext();

        // SharedPreferences取得
        pref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // INI保存先ディレクトリを解決してファイルを生成
        File baseDir = resolveIniBaseDir();
        iniFile = new File(baseDir, INI_FILE_NAME);

        // 初回のみassetsからINIをコピー（存在する場合は保持）
        copyIniFromAssetsIfNeeded();
    }

    //=================================================
    //　機　能　:　INI保存先のベースディレクトリを解決する
    //　引　数　:　なし
    //　戻り値　:　[File] ..... ディレクトリ
    //=================================================
    private static File resolveIniBaseDir() {

        // APK配布後の実機では、アプリ専用外部領域を優先（永続性と運用のしやすさ重視）
        File appExternalDir = appContext.getExternalFilesDir(null);
        if (appExternalDir != null) {
            return appExternalDir;
        }

        // 取得不可時のみ内部領域へフォールバック
        return appContext.getFilesDir();
    }

    //=================================================
    //　機　能　:　assetsからINIを初回だけコピーする
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================

    /**
     * 初回のみ assets/TatsumiHandy.ini をコピーする。
     * 2回目以降は既存ファイルを保持して、save() で更新していく。
     */
    private static void copyIniFromAssetsIfNeeded() {

        // 既に存在する場合は何もしない（運用で編集されたINIを保持）
        if (iniFile.exists()) {
            return;
        }

        // 親ディレクトリが無ければ作成
        File parent = iniFile.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        // assetsからINIをコピー
        try (InputStream in = appContext.getAssets().open(INI_FILE_NAME);
             FileOutputStream out = new FileOutputStream(iniFile, false)) {

            byte[] buffer = new byte[4096];
            int read;

            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }

        } catch (IOException ignored) {
            // assets に無い場合やコピー失敗時は従来どおり継続（SharedPreferencesにフォールバック）
        }
    }

    // ================================
    // Load / Save
    // ================================

    //=================================================
    //　機　能　:　設定を読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    public static void load() {

        // init未実施の場合は例外
        ensureInitialized();

        // まずSharedPreferencesから読み込み（既存互換）
        loadFromPreferences();

        // INIが存在する場合はINIを優先して上書き
        if (iniFile.exists()) {
            loadFromIni();
        }

        // デフォルト補正
        applyDefaultValues();
    }

    //=================================================
    //　機　能　:　設定を保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    public static void save() {

        // init未実施の場合は例外
        ensureInitialized();

        // SharedPreferencesへ保存
        SharedPreferences.Editor e = pref.edit();

        // ブザー設定
        e.putBoolean(KEY_BUZZER_MUTE, BuzzerMute);
        e.putInt(KEY_BUZZER_LENGTH, BuzzerLength);
        e.putInt(KEY_BUZZER_VOLUME, BuzzerVolume);

        // バイブ設定
        e.putBoolean(KEY_VIBRATOR_MUTE, VibratorMute);
        e.putInt(KEY_VIBRATOR_LENGTH, VibratorLength);
        e.putInt(KEY_VIBRATOR_COUNT, VibratorCount);
        e.putInt(KEY_VIBRATOR_INTERVAL, VibratorInterval);

        // カメラ設定
        e.putInt(KEY_CAMERA_SIZE, CameraImageSize);
        e.putInt(KEY_CAMERA_FLASH, CameraFlash);
        e.putInt(KEY_CAMERA_LIGHT, CameraLightMode);

        // WebサービスURL
        e.putString(KEY_WEBSVC_HONBAN, WebSvcURL_Honban);
        e.putString(KEY_WEBSVC_SCS, WebSvcURL_SCS);
        e.putString(KEY_WEBSVC_TEST, WebSvcURL_Test);

        // 非同期反映
        e.apply();

        // INIへも保存
        saveToIni();
    }

    //=================================================
    //　機　能　:　保存された各種設定を読み込んで変数へ反映する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private static void loadFromPreferences() {

        // ブザー設定
        BuzzerMute = pref.getBoolean(KEY_BUZZER_MUTE, false);
        BuzzerLength = pref.getInt(KEY_BUZZER_LENGTH, 0);
        BuzzerVolume = pref.getInt(KEY_BUZZER_VOLUME, 0);

        // バイブ設定
        VibratorMute = pref.getBoolean(KEY_VIBRATOR_MUTE, false);
        VibratorLength = pref.getInt(KEY_VIBRATOR_LENGTH, 0);
        VibratorCount = pref.getInt(KEY_VIBRATOR_COUNT, 0);
        VibratorInterval = pref.getInt(KEY_VIBRATOR_INTERVAL, 0);

        // カメラ設定
        CameraImageSize = pref.getInt(KEY_CAMERA_SIZE, 0);
        CameraFlash = pref.getInt(KEY_CAMERA_FLASH, 0);
        CameraLightMode = pref.getInt(KEY_CAMERA_LIGHT, 0);

        // WebサービスURL
        WebSvcURL_Honban = pref.getString(KEY_WEBSVC_HONBAN, "");
        WebSvcURL_SCS = pref.getString(KEY_WEBSVC_SCS, "");
        WebSvcURL_Test = pref.getString(KEY_WEBSVC_TEST, "");
    }

    //=================================================
    //　機　能　:　INIから読み込んで設定へ反映する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private static void loadFromIni() {

        // key=value をMapで取得
        Map<String, String> map = readIni();

        // ブザー設定
        BuzzerMute = parseBoolean(map.get(KEY_BUZZER_MUTE), BuzzerMute);
        BuzzerLength = parseInt(map.get(KEY_BUZZER_LENGTH), BuzzerLength);
        BuzzerVolume = parseInt(map.get(KEY_BUZZER_VOLUME), BuzzerVolume);

        // バイブ設定
        VibratorMute = parseBoolean(map.get(KEY_VIBRATOR_MUTE), VibratorMute);
        VibratorLength = parseInt(map.get(KEY_VIBRATOR_LENGTH), VibratorLength);
        VibratorCount = parseInt(map.get(KEY_VIBRATOR_COUNT), VibratorCount);
        VibratorInterval = parseInt(map.get(KEY_VIBRATOR_INTERVAL), VibratorInterval);

        // カメラ設定
        CameraImageSize = parseInt(map.get(KEY_CAMERA_SIZE), CameraImageSize);
        CameraFlash = parseInt(map.get(KEY_CAMERA_FLASH), CameraFlash);
        CameraLightMode = parseInt(map.get(KEY_CAMERA_LIGHT), CameraLightMode);

        // WebサービスURL
        WebSvcURL_Honban = parseString(map.get(KEY_WEBSVC_HONBAN), WebSvcURL_Honban);
        WebSvcURL_SCS = parseString(map.get(KEY_WEBSVC_SCS), WebSvcURL_SCS);
        WebSvcURL_Test = parseString(map.get(KEY_WEBSVC_TEST), WebSvcURL_Test);
    }

    //=================================================
    //　機　能　:　現在設定値をINIへ保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private static void saveToIni() {

        // 保存順を保持したいので LinkedHashMap
        Map<String, String> map = new LinkedHashMap<>();

        // ブザー設定
        map.put(KEY_BUZZER_MUTE, String.valueOf(BuzzerMute));
        map.put(KEY_BUZZER_LENGTH, String.valueOf(BuzzerLength));
        map.put(KEY_BUZZER_VOLUME, String.valueOf(BuzzerVolume));

        // バイブ設定
        map.put(KEY_VIBRATOR_MUTE, String.valueOf(VibratorMute));
        map.put(KEY_VIBRATOR_LENGTH, String.valueOf(VibratorLength));
        map.put(KEY_VIBRATOR_COUNT, String.valueOf(VibratorCount));
        map.put(KEY_VIBRATOR_INTERVAL, String.valueOf(VibratorInterval));

        // カメラ設定
        map.put(KEY_CAMERA_SIZE, String.valueOf(CameraImageSize));
        map.put(KEY_CAMERA_FLASH, String.valueOf(CameraFlash));
        map.put(KEY_CAMERA_LIGHT, String.valueOf(CameraLightMode));

        // WebサービスURL（nullは空文字へ）
        map.put(KEY_WEBSVC_HONBAN, nullToEmpty(WebSvcURL_Honban));
        map.put(KEY_WEBSVC_SCS, nullToEmpty(WebSvcURL_SCS));
        map.put(KEY_WEBSVC_TEST, nullToEmpty(WebSvcURL_Test));

        // INIへ書き込み
        writeIni(map);
    }

    //=================================================
    //　機　能　:　INIを読み込む（key=value）
    //　引　数　:　なし
    //　戻り値　:　[Map<String,String>] ..... 読み込み結果
    //=================================================
    private static Map<String, String> readIni() {

        Map<String, String> result = new LinkedHashMap<>();

        // ファイルが無ければ空で返す
        if (!iniFile.exists()) {
            return result;
        }

        // UTF-8で読み込み
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(iniFile), StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {

                // 前後空白除去
                line = line.trim();

                // 空行/コメント行はスキップ
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }

                // key=value のみ対象
                int idx = line.indexOf('=');
                if (idx < 0) {
                    continue;
                }

                // keyとvalueを抽出して格納
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                result.put(key, value);
            }

        } catch (IOException ignored) {
            // 読み込み失敗時は既存値を維持（呼び出し側でデフォルト補正）
        }

        return result;
    }

    //=================================================
    //　機　能　:　INIへ書き込む（key=value）
    //　引　数　:　map ..... Map<String,String>
    //　戻り値　:　[void] ..... なし
    //=================================================
    private static void writeIni(Map<String, String> map) {

        // 親ディレクトリが無ければ作成
        File parent = iniFile.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        // UTF-8で上書き保存
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

    //=================================================
    //　機　能　:　デフォルト値を補正する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private static void applyDefaultValues() {

        // 数値が0の場合のみデフォルトを適用
        if (BuzzerLength == 0) BuzzerLength = 1000;
        if (BuzzerVolume == 0) BuzzerVolume = 1;

        if (VibratorLength == 0) VibratorLength = 500;
        if (VibratorCount == 0) VibratorCount = 2;
        if (VibratorInterval == 0) VibratorInterval = 100;

        // URLはnull禁止（空文字へ）
        if (WebSvcURL_Honban == null) WebSvcURL_Honban = "";
        if (WebSvcURL_SCS == null) WebSvcURL_SCS = "";
        if (WebSvcURL_Test == null) WebSvcURL_Test = "";
    }

    //=================================================
    //　機　能　:　文字列をintへ変換する
    //　引　数　:　value ..... String
    //　　　　　:　defaultValue ..... int
    //　戻り値　:　[int] ..... 変換結果（失敗時defaultValue）
    //=================================================
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

    //=================================================
    //　機　能　:　文字列をbooleanへ変換する
    //　引　数　:　value ..... String
    //　　　　　:　defaultValue ..... boolean
    //　戻り値　:　[boolean] ..... 変換結果（失敗時defaultValue）
    //=================================================
    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        // INI互換（1/0も許可）
        if ("1".equals(value)) return true;
        if ("0".equals(value)) return false;

        return Boolean.parseBoolean(value);
    }

    //=================================================
    //　機　能　:　文字列をそのまま取得する
    //　引　数　:　value ..... String
    //　　　　　:　defaultValue ..... String
    //　戻り値　:　[String] ..... valueがnullならdefaultValue、それ以外はvalue
    //=================================================
    private static String parseString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    //=================================================
    //　機　能　:　nullを空文字に変換する
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... 空文字（または元値）
    //=================================================
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    //=================================================
    //　機　能　:　初期化済みか確認する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし（未初期化は例外）
    //=================================================
    private static void ensureInitialized() {
        if (pref == null || appContext == null || iniFile == null) {
            throw new IllegalStateException("AppSettings.init(context) を先に呼び出してください");
        }
    }

    // ================================
    // 設定値
    // ================================
    public static boolean BuzzerMute; // ブザーON/OFF
    public static int BuzzerLength;   // ブザー長さ
    public static int BuzzerVolume;   // ブザー音量

    public static boolean VibratorMute; // バイブON/OFF
    public static int VibratorLength;   // バイブ長さ
    public static int VibratorCount;    // バイブ回数
    public static int VibratorInterval; // バイブ間隔

    public static int CameraImageSize; // カメラ画像サイズ
    public static int CameraFlash;     // カメラフラッシュ設定
    public static int CameraLightMode; // カメラ露出設定

    public static String WebSvcURL_Honban; // 本番WebSvc URL
    public static String WebSvcURL_SCS;    // SCS WebSvc URL
    public static String WebSvcURL_Test;   // テストWebSvc URL

    //=================================
    //　機　能　:　AppSettingsのインスタンス化禁止
    //　引　数　:　なし
    //　戻り値　:　[AppSettings] ..... なし
    //=================================
    private AppSettings() {
        // Utilityクラスのためインスタンス化しない
    }
}
