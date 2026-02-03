package com.example.myapplication.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.myapplication.R;
import com.example.myapplication.settings.AppSettings;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;


//============================================================
//　処理概要　:　CameraSettingActivityクラス
//============================================================

/**
 * カメラ設定画面Activity。
 *
 * <p>撮影解像度・フラッシュ・光源モードの設定をSpinnerで選択し、
 * {@link AppSettings} に保存する。</p>
 *
 * <p>保存時は呼び出し元にRESULT_OKを返す。</p>
 */
public class CameraSettingActivity extends BaseActivity {

    private static final int CAM_UXGA = 0;
    private static final int CAM_QUADVGA = 1;
    private static final int CAM_XGA = 2;
    private static final int CAM_SVGA = 3;
    private static final int CAM_VGA = 4;
    private static final int CAM_QVGA = 5;

    private static final int CAM_FLASH_AUTO = 0;
    private static final int CAM_FLASH_ENABLE = 1;
    private static final int CAM_FLASH_DISABLE = 2;

    private static final int CAM_LIGHT_AUTO = 0;
    private static final int CAM_OUTDOOR = 1;
    private static final int CAM_FLUORESCENT = 2;
    private static final int CAM_INCANDESCE = 3;
    private static final int CAM_DIMLIGHT = 4;

    private Spinner spImageSize;
    private Spinner spFlash;
    private Spinner spLightMode;

    private List<Option> sizeOptions;
    private List<Option> flashOptions;
    private List<Option> lightOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_setting);

        AppSettings.init(this);
        AppSettings.load();

        spImageSize = findViewById(R.id.spImageSize);
        spFlash = findViewById(R.id.spFlash);
        spLightMode = findViewById(R.id.spLightMode);

        sizeOptions = buildSizeOptions();
        flashOptions = buildFlashOptions();
        lightOptions = buildLightOptions();

        bindSpinner(spImageSize, sizeOptions);
        bindSpinner(spFlash, flashOptions);
        bindSpinner(spLightMode, lightOptions);

        setSpinnerSelection(spImageSize, sizeOptions, AppSettings.CameraImageSize);
        setSpinnerSelection(spFlash, flashOptions, AppSettings.CameraFlash);
        setSpinnerSelection(spLightMode, lightOptions, AppSettings.CameraLightMode);

        setupBottomButtonTexts();
    }

    private void bindSpinner(Spinner spinner, List<Option> options) {
        ArrayAdapter<Option> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSpinnerSelection(Spinner spinner, List<Option> options, int value) {
        int index = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value == value) {
                index = i;
                break;
            }
        }
        spinner.setSelection(index);
    }

    private List<Option> buildSizeOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option(CAM_UXGA, "高精細(1200x1600)"));
        options.add(new Option(CAM_QUADVGA, "精細(960x1280)"));
        options.add(new Option(CAM_XGA, "大(768x1024)"));
        options.add(new Option(CAM_SVGA, "中(600x800)"));
        options.add(new Option(CAM_VGA, "小(480x640)"));
        options.add(new Option(CAM_QVGA, "極小(240x320)"));
        return options;
    }

    private List<Option> buildFlashOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option(CAM_FLASH_AUTO, "自動"));
        options.add(new Option(CAM_FLASH_ENABLE, "オン"));
        options.add(new Option(CAM_FLASH_DISABLE, "オフ"));
        return options;
    }

    private List<Option> buildLightOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option(CAM_LIGHT_AUTO, "自然"));
        options.add(new Option(CAM_OUTDOOR, "屋外"));
        options.add(new Option(CAM_FLUORESCENT, "蛍光灯"));
        options.add(new Option(CAM_INCANDESCE, "白熱灯"));
        options.add(new Option(CAM_DIMLIGHT, "暗所"));
        return options;
    }

    private void setupBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (blue != null) blue.setText("保存");
        if (yellow != null) yellow.setText("終了");
        refreshBottomButtonsEnabled();
    }

    @Override
    protected void onFunctionBlue() {
        AppSettings.CameraImageSize = getSelectedValue(spImageSize);
        AppSettings.CameraFlash = getSelectedValue(spFlash);
        AppSettings.CameraLightMode = getSelectedValue(spLightMode);
        AppSettings.save();

        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onFunctionYellow() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private int getSelectedValue(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        if (item instanceof Option) {
            return ((Option) item).value;
        }
        return 0;
    }

    private static final class Option {
        private final int value;
        private final String label;

        private Option(int value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
