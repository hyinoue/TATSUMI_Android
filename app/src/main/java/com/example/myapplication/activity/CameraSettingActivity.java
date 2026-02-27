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
//　処理概要　:　カメラ設定画面（Activity）
//　　　　　　:　撮影解像度・フラッシュ・光源モードをSpinnerで選択し、
//　　　　　　:　AppSettingsへ保存する。
//　関　　数　:　onCreate ................. 画面生成/初期化（設定読込/Spinner初期化/選択反映）
//　　　　　　:　bindSpinner .............. Spinnerへ選択肢をバインド
//　　　　　　:　setSpinnerSelection ...... Spinnerの選択位置を設定値から復元
//　　　　　　:　buildSizeOptions ......... 解像度選択肢生成
//　　　　　　:　buildFlashOptions ........ フラッシュ選択肢生成
//　　　　　　:　buildLightOptions ........ 光源モード選択肢生成
//　　　　　　:　setupBottomButtonTexts ... 下部ボタン文言設定（保存/終了）
//　　　　　　:　onFunctionBlue ........... 保存（選択値→AppSettings反映→保存→RESULT_OK）
//　　　　　　:　onFunctionYellow ......... 終了（キャンセル→RESULT_CANCELED）
//　　　　　　:　getSelectedValue ......... Spinnerの選択値（Option.value）取得
//　クラス　　:　Option ................... Spinner表示用の選択肢（value/label）
//============================================================

public class CameraSettingActivity extends BaseActivity {

    private static final int CAM_UXGA = 0;    // 画像サイズ: UXGA
    private static final int CAM_QUADVGA = 1; // 画像サイズ: QUADVGA
    private static final int CAM_XGA = 2;     // 画像サイズ: XGA
    private static final int CAM_SVGA = 3;    // 画像サイズ: SVGA
    private static final int CAM_VGA = 4;     // 画像サイズ: VGA
    private static final int CAM_QVGA = 5;    // 画像サイズ: QVGA

    private static final int CAM_FLASH_AUTO = 0;    // フラッシュ: AUTO
    private static final int CAM_FLASH_ENABLE = 1;  // フラッシュ: ON
    private static final int CAM_FLASH_DISABLE = 2; // フラッシュ: OFF

    private static final int CAM_LIGHT_AUTO = 0;   // 露出補正: AUTO
    private static final int CAM_OUTDOOR = 1;      // 露出補正: 屋外
    private static final int CAM_FLUORESCENT = 2;  // 露出補正: 蛍光灯
    private static final int CAM_INCANDESCE = 3;   // 露出補正: 白熱灯
    private static final int CAM_DIMLIGHT = 4;     // 露出補正: 暗所

    private Spinner spImageSize; // 画像サイズ選択
    private Spinner spFlash;     // フラッシュ選択
    private Spinner spLightMode; // 露出補正選択

    private List<Option> sizeOptions;  // 画像サイズ選択肢
    private List<Option> flashOptions; // フラッシュ選択肢
    private List<Option> lightOptions; // 露出補正選択肢

    //============================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... 画面再生成時の保存状態
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画面レイアウト設定
        setContentView(R.layout.activity_camera_setting);

        // 設定の初期化/読込
        AppSettings.init(this);
        AppSettings.load();

        // 画面部品取得
        spImageSize = findViewById(R.id.spImageSize);
        spFlash = findViewById(R.id.spFlash);
        spLightMode = findViewById(R.id.spLightMode);

        // Spinnerの選択肢生成
        sizeOptions = buildSizeOptions();
        flashOptions = buildFlashOptions();
        lightOptions = buildLightOptions();

        // Spinnerへバインド
        bindSpinner(spImageSize, sizeOptions);
        bindSpinner(spFlash, flashOptions);
        bindSpinner(spLightMode, lightOptions);

        // 保存済み設定値をSpinnerへ反映（未一致の場合は先頭を選択）
        setSpinnerSelection(spImageSize, sizeOptions, AppSettings.CameraImageSize);
        setSpinnerSelection(spFlash, flashOptions, AppSettings.CameraFlash);
        setSpinnerSelection(spLightMode, lightOptions, AppSettings.CameraLightMode);

        // 下部ボタン文言設定
        setupBottomButtonTexts();
    }

    //============================================================
    //　機　能　:　スピナー部品を関連付ける
    //　引　数　:　spinner ..... スピナー
    //　　　　　:　options ..... 選択肢一覧
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void bindSpinner(Spinner spinner, List<Option> options) {
        // Option.toString()（label）を表示するAdapterを作成
        ArrayAdapter<Option> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Spinnerへセット
        spinner.setAdapter(adapter);
    }

    //============================================================
    //　機　能　:　スピナーの選択値を設定する
    //　引　数　:　spinner ..... スピナー
    //　　　　　:　options ..... 選択肢一覧
    //　　　　　:　value ..... 設定値
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setSpinnerSelection(Spinner spinner, List<Option> options, int value) {
        // 設定値と一致するOptionを探し、その位置を選択する
        int index = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value == value) {
                index = i;
                break;
            }
        }
        spinner.setSelection(index);
    }

    //============================================================
    //　機　能　:　サイズ選択肢を生成する
    //　引　数　:　なし
    //　戻り値　:　[List<Option>] ..... 解像度選択肢一覧
    //============================================================
    private List<Option> buildSizeOptions() {
        // 解像度（表示文言はユーザー向け）
        List<Option> options = new ArrayList<>();
        options.add(new Option(CAM_UXGA, "高精細(1200x1600)"));
        options.add(new Option(CAM_QUADVGA, "精細(960x1280)"));
        options.add(new Option(CAM_XGA, "大(768x1024)"));
        options.add(new Option(CAM_SVGA, "中(600x800)"));
        options.add(new Option(CAM_VGA, "小(480x640)"));
        options.add(new Option(CAM_QVGA, "極小(240x320)"));
        return options;
    }

    //============================================================
    //　機　能　:　フラッシュ選択肢を生成する
    //　引　数　:　なし
    //　戻り値　:　[List<Option>] ..... フラッシュ選択肢一覧
    //============================================================
    private List<Option> buildFlashOptions() {
        // フラッシュ設定
        List<Option> options = new ArrayList<>();
        options.add(new Option(CAM_FLASH_AUTO, "自動"));
        options.add(new Option(CAM_FLASH_ENABLE, "オン"));
        options.add(new Option(CAM_FLASH_DISABLE, "オフ"));
        return options;
    }

    //============================================================
    //　機　能　:　照明選択肢を生成する
    //　引　数　:　なし
    //　戻り値　:　[List<Option>] ..... 光源モード選択肢一覧
    //============================================================
    private List<Option> buildLightOptions() {
        // 光源（ホワイトバランス想定）
        List<Option> options = new ArrayList<>();
        options.add(new Option(CAM_LIGHT_AUTO, "自然"));
        options.add(new Option(CAM_OUTDOOR, "屋外"));
        options.add(new Option(CAM_FLUORESCENT, "蛍光灯"));
        options.add(new Option(CAM_INCANDESCE, "白熱灯"));
        options.add(new Option(CAM_DIMLIGHT, "暗所"));
        return options;
    }

    //============================================================
    //　機　能　:　下部ボタンの表示文言を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupBottomButtonTexts() {
        // 下部ボタン取得
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        // 文言設定（保存/終了）
        if (blue != null) blue.setText("保存");
        if (yellow != null) yellow.setText("終了");

        // 活性制御（BaseActivity側の共通処理想定）
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　青ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionBlue() {
        // Spinnerの選択値をAppSettingsへ反映
        AppSettings.CameraImageSize = getSelectedValue(spImageSize);
        AppSettings.CameraFlash = getSelectedValue(spFlash);
        AppSettings.CameraLightMode = getSelectedValue(spLightMode);

        // 永続化
        AppSettings.save();

        // 呼び出し元へ成功結果を返却
        setResult(RESULT_OK);
        finish();
    }

    //============================================================
    //　機　能　:　黄ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionYellow() {
        // 変更を保存せず終了
        setResult(RESULT_CANCELED);
        finish();
    }

    //============================================================
    //　機　能　:　選択値を取得する
    //　引　数　:　spinner ..... スピナー
    //　戻り値　:　[int] ..... Option.value（未取得時は0）
    //============================================================
    private int getSelectedValue(Spinner spinner) {
        // Spinnerの選択項目からOption.valueを取り出す
        Object item = spinner.getSelectedItem();
        if (item instanceof Option) {
            return ((Option) item).value;
        }
        return 0;
    }

    //============================================================
    //　処理概要　:　Spinner表示用の選択肢クラス
    //　　　　　　:　value（内部値）とlabel（表示名）を保持し、toStringでlabelを返す
    //============================================================
    private static final class Option {
        private final int value;
        private final String label;

        //============================================================
        //　機　能　:　オプションの初期化処理
        //　引　数　:　value ..... 設定値
        //　　　　　:　label ..... 表示ラベル
        //　戻り値　:　[Option] ..... なし
        //============================================================
        private Option(int value, String label) {
            this.value = value;
            this.label = label;
        }

        //============================================================
        //　機　能　:　表示文字列へ変換する
        //　引　数　:　なし
        //　戻り値　:　[String] ..... Spinner表示用文字列（label）
        //============================================================
        @Override
        public String toString() {
            return label;
        }
    }
}
