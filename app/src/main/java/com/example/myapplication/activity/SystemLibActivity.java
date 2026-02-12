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

//==================================
//　処理概要　:　SystemLibActivityクラス
//==================================

/**
 * システムライブラリ関連の画面Activity。
 *
 * <p>ブザー音/バイブレーションの設定を行い、他画面に反映する。</p>
 */
public class SystemLibActivity extends BaseActivity {

    private static final int BUZZER_VOLUME_MAX = 10;
    private static final int BUZZER_VOLUME_MID = 5;
    private static final int BUZZER_VOLUME_MIN = 1;

    private CheckBox chkBuzzerOnOff;
    private EditText etBuzzerLength;
    private Spinner spBuzzerVolume;
    private CheckBox chkVibrationOnOff;
    private EditText etVibLength;
    private EditText etVibCount;
    private EditText etVibInterval;
    private Button btnBuzzerTest;
    private Button btnVibrationTest;
    private List<VolumeOption> volumeOptions;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_lib);
        setupBottomButtons();
        bindViews();
        setupVolumeSpinner();
        loadSettingsToForm();
        setupTestButtons();
    }

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

    private void setupVolumeSpinner() {
        volumeOptions = new ArrayList<>();
        volumeOptions.add(new VolumeOption(BUZZER_VOLUME_MAX, "大"));
        volumeOptions.add(new VolumeOption(BUZZER_VOLUME_MID, "中"));
        volumeOptions.add(new VolumeOption(BUZZER_VOLUME_MIN, "小"));

        ArrayAdapter<VolumeOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                volumeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBuzzerVolume.setAdapter(adapter);
    }

    private void loadSettingsToForm() {
        chkBuzzerOnOff.setChecked(!AppSettings.BuzzerMute);
        etBuzzerLength.setText(String.valueOf(AppSettings.BuzzerLength));
        setSpinnerSelection(AppSettings.BuzzerVolume);

        chkVibrationOnOff.setChecked(!AppSettings.VibratorMute);
        etVibLength.setText(String.valueOf(AppSettings.VibratorLength));
        etVibCount.setText(String.valueOf(AppSettings.VibratorCount));
        etVibInterval.setText(String.valueOf(AppSettings.VibratorInterval));
    }

    private void setupTestButtons() {
        btnBuzzerTest.setOnClickListener(v -> {
            boolean mute = !chkBuzzerOnOff.isChecked();
            int length = readInt(etBuzzerLength, AppSettings.BuzzerLength);
            int volume = readSelectedVolume();
            playBuzzerPreview(mute, volume, length);
        });

        btnVibrationTest.setOnClickListener(v -> {
            boolean mute = !chkVibrationOnOff.isChecked();
            int length = readInt(etVibLength, AppSettings.VibratorLength);
            int count = readInt(etVibCount, AppSettings.VibratorCount);
            int interval = readInt(etVibInterval, AppSettings.VibratorInterval);
            playVibrationPreview(mute, count, length, interval);
        });
    }

    private void setSpinnerSelection(int value) {
        for (int i = 0; i < volumeOptions.size(); i++) {
            if (volumeOptions.get(i).value == value) {
                spBuzzerVolume.setSelection(i);
                return;
            }
        }
    }

    private int readSelectedVolume() {
        Object selected = spBuzzerVolume.getSelectedItem();
        if (selected instanceof VolumeOption) {
            return ((VolumeOption) selected).value;
        }
        return AppSettings.BuzzerVolume;
    }

    private int readInt(EditText editText, int fallback) {
        if (editText == null) return fallback;
        String text = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return fallback;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void playBuzzerPreview(boolean mute, int volume, int length) {
        if (mute) return;
        int safeLength = Math.max(0, length);
        int safeVolume = Math.max(0, Math.min(volume, 10));
        int toneVolume = Math.max(0, Math.min(safeVolume * 10, 100));
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, toneVolume);
        tone.startTone(ToneGenerator.TONE_PROP_NACK, safeLength);
        new Handler(Looper.getMainLooper()).postDelayed(tone::release, safeLength + 50L);
    }

    private void playVibrationPreview(boolean mute, int count, int length, int interval) {
        if (mute) return;
        int safeLength = Math.max(0, length);
        if (safeLength == 0) return;
        int safeCount = Math.max(1, count);
        int safeInterval = Math.max(0, interval);

        Vibrator vibrator = getSystemService(Vibrator.class);
        if (vibrator == null) return;

        long[] pattern = new long[safeCount * 2];
        for (int i = 0; i < safeCount; i++) {
            pattern[i * 2] = (i == 0) ? 0 : safeInterval;
            pattern[i * 2 + 1] = safeLength;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    private void setupBottomButtons() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (yellow != null) yellow.setText("終了");
        if (red != null) red.setText("");
        if (blue != null) blue.setText("保存");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
    }

    //==================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionBlue() {
        AppSettings.BuzzerMute = !chkBuzzerOnOff.isChecked();
        AppSettings.BuzzerLength = readInt(etBuzzerLength, AppSettings.BuzzerLength);
        AppSettings.BuzzerVolume = readSelectedVolume();

        AppSettings.VibratorMute = !chkVibrationOnOff.isChecked();
        AppSettings.VibratorLength = readInt(etVibLength, AppSettings.VibratorLength);
        AppSettings.VibratorCount = readInt(etVibCount, AppSettings.VibratorCount);
        AppSettings.VibratorInterval = readInt(etVibInterval, AppSettings.VibratorInterval);

        AppSettings.save();
        finish();
    }

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    private static class VolumeOption {
        private final int value;
        private final String label;

        private VolumeOption(int value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}