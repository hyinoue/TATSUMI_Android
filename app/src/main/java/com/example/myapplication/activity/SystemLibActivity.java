package com.example.myapplication.activity;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.myapplication.R;
import com.example.myapplication.settings.AppSettings;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

//============================================================
//　処理概要　:　振動設定画面Activity
//　　　　　　:　ブザー音/バイブレーションの設定を画面で編集し、保存して他画面へ反映する。
//　関　　数　:　onCreate             ..... 画面生成/初期化
//　　　　　　:　bindViews           ..... 画面部品の取得
//　　　　　　:　setupVolumeSpinner  ..... ブザー音量スピナー設定
//　　　　　　:　loadSettingsToForm  ..... 設定値を画面へ反映
//　　　　　　:　setupTestButtons    ..... テストボタン(ブザー/バイブ)設定
//　　　　　　:　setSpinnerSelection ..... スピナー選択位置設定
//　　　　　　:　readSelectedVolume  ..... スピナー選択音量の取得
//　　　　　　:　readInt             ..... 数値入力の取得(フォールバックあり)
//　　　　　　:　playBuzzerPreview   ..... ブザープレビュー再生
//　　　　　　:　playVibrationPreview ..... バイブプレビュー実行
//　　　　　　:　setupBottomButtons  ..... 下部ボタン設定
//　　　　　　:　onFunctionBlue      ..... (青)保存処理
//　　　　　　:　onFunctionYellow    ..... (黄)終了処理
//　　　　　　:　VolumeOption        ..... 音量選択肢クラス(内部クラス)
//============================================================

public class SystemLibActivity extends BaseActivity {

    private static final int BUZZER_VOLUME_MAX = 10; // ブザー音量: 最大
    private static final int BUZZER_VOLUME_MID = 5;  // ブザー音量: 中
    private static final int BUZZER_VOLUME_MIN = 1;  // ブザー音量: 最小

    private CheckBox chkBuzzerOnOff;      // ブザーON/OFF
    private EditText etBuzzerLength;      // ブザー時間
    private Spinner spBuzzerVolume;       // ブザー音量
    private CheckBox chkVibrationOnOff;   // バイブON/OFF
    private EditText etVibLength;         // バイブ時間
    private EditText etVibCount;          // バイブ回数
    private EditText etVibInterval;       // バイブ間隔
    private Button btnBuzzerTest;         // ブザーテストボタン
    private Button btnVibrationTest;      // バイブテストボタン
    private List<VolumeOption> volumeOptions; // 音量選択肢

    //============================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... 画面再生成時の保存状態
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_lib);

        // 下部ボタン設定
        setupBottomButtons();

        // 画面部品の取得
        bindViews();

        // 音量スピナー設定
        setupVolumeSpinner();

        // 設定値を画面に反映
        loadSettingsToForm();

        // テストボタンイベント設定
        setupTestButtons();
    }

    //============================================================
    //　機　能　:　画面部品の取得
    //　引　数　:　なし
    //　戻り値　:　[void]  ..... なし
    //============================================================
    private void bindViews() {
        chkBuzzerOnOff = findViewById(R.id.chkBuzzerOnOff);
        etBuzzerLength = findViewById(R.id.etBuzzerLengthMs);
        spBuzzerVolume = findViewById(R.id.spBuzzerVolume);
        chkVibrationOnOff = findViewById(R.id.chkVibrationOnOff);
        etVibLength = findViewById(R.id.etVibLengthMs);
        etVibCount = findViewById(R.id.etVibCount);
        etVibInterval = findViewById(R.id.etVibIntervalMs);
        btnBuzzerTest = findViewById(R.id.btnBuzzerTest);
        btnVibrationTest = findViewById(R.id.btnVibrationTest);
    }

    //============================================================
    //　機　能　:　ブザー音量スピナー設定
    //　引　数　:　なし
    //　戻り値　:　[void]  ..... なし
    //============================================================
    private void setupVolumeSpinner() {
        // 表示ラベルと実値(1/5/10)の対応を作成
        volumeOptions = new ArrayList<>();
        volumeOptions.add(new VolumeOption(BUZZER_VOLUME_MAX, "大"));
        volumeOptions.add(new VolumeOption(BUZZER_VOLUME_MID, "中"));
        volumeOptions.add(new VolumeOption(BUZZER_VOLUME_MIN, "小"));

        // スピナーにアダプタを設定
        ArrayAdapter<VolumeOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                volumeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBuzzerVolume.setAdapter(adapter);
    }

    //============================================================
    //　機　能　:　設定値を画面へ反映
    //　引　数　:　なし
    //　戻り値　:　[void]  ..... なし
    //============================================================
    private void loadSettingsToForm() {
        // ブザー設定を画面へ反映（Muteは画面上はON/OFFの反転表現）
        chkBuzzerOnOff.setChecked(!AppSettings.BuzzerMute);
        etBuzzerLength.setText(String.valueOf(AppSettings.BuzzerLength));
        setSpinnerSelection(AppSettings.BuzzerVolume);

        // バイブ設定を画面へ反映（Muteは画面上はON/OFFの反転表現）
        chkVibrationOnOff.setChecked(!AppSettings.VibratorMute);
        etVibLength.setText(String.valueOf(AppSettings.VibratorLength));
        etVibCount.setText(String.valueOf(AppSettings.VibratorCount));
        etVibInterval.setText(String.valueOf(AppSettings.VibratorInterval));
    }

    //============================================================
    //　機　能　:　テストボタン(ブザー/バイブ)設定
    //　引　数　:　なし
    //　戻り値　:　[void]  ..... なし
    //============================================================
    private void setupTestButtons() {
        // ブザーテスト：画面入力値でプレビュー再生
        btnBuzzerTest.setOnClickListener(v -> {
            boolean mute = !chkBuzzerOnOff.isChecked();
            int length = readInt(etBuzzerLength, AppSettings.BuzzerLength);
            int volume = readSelectedVolume();
            playBuzzerPreview(mute, volume, length);
        });

        // バイブテスト：画面入力値でプレビュー実行
        btnVibrationTest.setOnClickListener(v -> {
            boolean mute = !chkVibrationOnOff.isChecked();
            int length = readInt(etVibLength, AppSettings.VibratorLength);
            int count = readInt(etVibCount, AppSettings.VibratorCount);
            int interval = readInt(etVibInterval, AppSettings.VibratorInterval);
            playVibrationPreview(mute, count, length, interval);
        });
    }

    //============================================================
    //　機　能　:　スピナー選択位置設定
    //　引　数　:　value ..... 設定したい音量値(実値)
    //　戻り値　:　[void]  ..... なし
    //============================================================
    private void setSpinnerSelection(int value) {
        // 実値一致する選択肢を探して選択
        for (int i = 0; i < volumeOptions.size(); i++) {
            if (volumeOptions.get(i).value == value) {
                spBuzzerVolume.setSelection(i);
                return;
            }
        }
    }

    //============================================================
    //　機　能　:　スピナー選択音量の取得
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 音量値(実値)
    //============================================================
    private int readSelectedVolume() {
        Object selected = spBuzzerVolume.getSelectedItem();
        if (selected instanceof VolumeOption) {
            return ((VolumeOption) selected).value;
        }
        // 取得できない場合は現在設定値を返す
        return AppSettings.BuzzerVolume;
    }

    //============================================================
    //　機　能　:　数値入力の取得(フォールバックあり)
    //　引　数　:　editText ..... 入力欄
    //　　　　　:　fallback ..... 取得失敗時に返す値
    //　戻り値　:　[int] ..... 入力値(不正/未入力時はfallback)
    //============================================================
    private int readInt(EditText editText, int fallback) {
        if (editText == null) return fallback;

        // 入力文字列を取得
        String text = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return fallback;

        // 数値変換（変換失敗時はfallback）
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    //============================================================
    //　機　能　:　ブザープレビュー再生
    //　引　数　:　mute   ..... True:ミュート(鳴らさない)
    //　　　　　:　volume ..... 音量(0～10)
    //　　　　　:　length ..... 鳴動時間(ms)
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void playBuzzerPreview(boolean mute, int volume, int length) {
        // ミュートなら何もしない
        if (mute) return;

        // 入力値の安全化
        int safeLength = Math.max(0, length);
        int safeVolume = Math.max(0, Math.min(volume, 10));

        // ToneGeneratorは0～100のボリュームなので換算
        int toneVolume = Math.max(0, Math.min(safeVolume * 10, 100));

        // 通知音でNACKトーンを鳴らす
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, toneVolume);
        tone.startTone(ToneGenerator.TONE_PROP_NACK, safeLength);

        // 再生後に必ずrelease（少し余裕を持たせる）
        new Handler(Looper.getMainLooper()).postDelayed(tone::release, safeLength + 50L);
    }

    //============================================================
    //　機　能　:　バイブプレビュー実行
    //　引　数　:　mute     ..... True:ミュート(振動しない)
    //　　　　　:　count    ..... 回数
    //　　　　　:　length   ..... 1回の振動時間(ms)
    //　　　　　:　interval ..... 振動間隔(ms)
    //　戻り値　:　[void]   ..... なし
    //============================================================
    private void playVibrationPreview(boolean mute, int count, int length, int interval) {
        // ミュートなら何もしない
        if (mute) return;

        // 入力値の安全化
        int safeLength = Math.max(0, length);
        if (safeLength == 0) return;           // 0msは振動しない
        int safeCount = Math.max(1, count);    // 最低1回
        int safeInterval = Math.max(0, interval);

        // Vibrator取得（APIにより取得方法が異なるが、ここではクラス指定で取得）
        Vibrator vibrator = getSystemService(Vibrator.class);
        if (vibrator == null) return;

        // [待ち, 振動, 待ち, 振動, ...] のパターンを作成
        long[] pattern = new long[safeCount * 2];
        for (int i = 0; i < safeCount; i++) {
            pattern[i * 2] = (i == 0) ? 0 : safeInterval;
            pattern[i * 2 + 1] = safeLength;
        }

        // APIレベルにより振動指定方法を切り替え
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    //============================================================
    //　機　能　:　下部ボタンの表示内容を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupBottomButtons() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton green = findViewById(R.id.btnBottomGreen);

        // ボタン表示を設定
        if (yellow != null) yellow.setText("終了");
        if (red != null) red.setText("");
        if (blue != null) blue.setText("保存");
        if (green != null) green.setText("");

        // ボタン活性制御を反映
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　青ボタン押下時の処理を行う(保存)
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionBlue() {
        // 画面入力値を設定へ反映（Muteは画面上はON/OFFの反転表現）
        AppSettings.BuzzerMute = !chkBuzzerOnOff.isChecked();
        AppSettings.BuzzerLength = readInt(etBuzzerLength, AppSettings.BuzzerLength);
        AppSettings.BuzzerVolume = readSelectedVolume();

        AppSettings.VibratorMute = !chkVibrationOnOff.isChecked();
        AppSettings.VibratorLength = readInt(etVibLength, AppSettings.VibratorLength);
        AppSettings.VibratorCount = readInt(etVibCount, AppSettings.VibratorCount);
        AppSettings.VibratorInterval = readInt(etVibInterval, AppSettings.VibratorInterval);

        // 永続化して画面終了
        AppSettings.save();
        finish();
    }

    //============================================================
    //　機　能　:　黄ボタン押下時の処理を行う(終了)
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //============================================================
    //　機　能　:　音量選択肢クラス(内部クラス)
    //　引　数　:　なし
    //　戻り値　:　[なし]
    //============================================================
    private static class VolumeOption {
        private final int value;
        private final String label;

        //============================================================
        //　機　能　:　音量選択肢生成
        //　引　数　:　value ..... 音量値(実値)
        //　　　　　:　label ..... 表示ラベル
        //　戻り値　:　[void]  ..... なし
        //============================================================
        private VolumeOption(int value, String label) {
            this.value = value;
            this.label = label;
        }

        //============================================================
        //　機　能　:　スピナー表示文字列の返却
        //　引　数　:　なし
        //　戻り値　:　[String] ..... 表示ラベル
        //============================================================
        @Override
        public String toString() {
            return label;
        }
    }
}
