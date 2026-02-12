package com.example.myapplication.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.scanner.DensoScannerController;
import com.example.myapplication.scanner.OnScanListener;
import com.google.android.material.button.MaterialButton;


/**
 * バーコードテスト画面（簡易版）Activity。
 *
 * <p>読み取り制御は {@link DensoScannerController} に委譲し、
 * 本画面ではスキャン結果の種別表示と履歴表示に限定している。</p>
 *
 * <p>主な挙動:</p>
 * <ul>
 *     <li>読み取りデータを履歴テキストに追記し、常に最下部へスクロール。</li>
 *     <li>種別(AIM/DENSO)に応じた表示名を別欄に出力。</li>
 *     <li>入力欄のフォーカスを戻して次の読み取りに備える。</li>
 * </ul>
 */

//===================================
//　処理概要　:　ImagerTestActivityクラス
//===================================

public class ImagerTestActivity extends BaseActivity {

    // ===== UI =====
    private EditText etBarcode;
    private EditText etKind;
    private ScrollView svKindContent;
    private TextView tvKindContent;

    // ===== Scanner common =====
    private DensoScannerController scanner;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imager_test);

        etBarcode = findViewById(R.id.etBarcode);
        etKind = findViewById(R.id.etKind);
        svKindContent = findViewById(R.id.scrollKind);
        tvKindContent = findViewById(R.id.tvKindContent);

        setupBottomButtons();

        // 入力欄（フォーカス固定・キーボード抑止）
        if (etBarcode != null) {
            etBarcode.setShowSoftInputOnFocus(false);
            etBarcode.setText("");
            etBarcode.requestFocus();
        }
        if (etKind != null) {
            etKind.setShowSoftInputOnFocus(false);
            etKind.setText("");
        }
        if (tvKindContent != null) {
            tvKindContent.setText("");
        }

        // 共通スキャナ（受信時の表示処理だけここで実装）
        scanner = new DensoScannerController(this, new OnScanListener() {
            //========================================
            //　機　能　:　スキャン受信時の処理
            //　引　数　:　normalizedData ..... String
            //　　　　　:　aim ..... String
            //　　　　　:　denso ..... String
            //　戻り値　:　[void] ..... なし
            //========================================
            @Override
            public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {

                // 種別表示（C#互換寄せ）
                if (etKind != null) {
                    String display = scanner.getBarcodeDisplayName(aim, denso);
                    etKind.setText(!TextUtils.isEmpty(display) ? display : "");
                }

                // 履歴追記（データだけ）
                if (tvKindContent != null) {
                    String current = tvKindContent.getText() != null ? tvKindContent.getText().toString() : "";
                    if (TextUtils.isEmpty(current)) {
                        tvKindContent.setText(normalizedData);
                    } else {
                        tvKindContent.setText(current + "\n" + normalizedData);
                    }
                }

                // 最下部へスクロール
                if (svKindContent != null) {
                    svKindContent.post(() -> svKindContent.fullScroll(ScrollView.FOCUS_DOWN));
                }

                // 入力欄をクリアしてフォーカス戻し（次読み取り用）
                if (etBarcode != null) {
                    etBarcode.setText("");
                    etBarcode.requestFocus();
                }
            }
        });

        // Manager生成開始
        scanner.onCreate();
    }
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupBottomButtons() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        if (yellow != null) {
            yellow.setText("終了");
        }
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
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

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) scanner.onResume();
        if (etBarcode != null) etBarcode.requestFocus();
    }

    //============================
    //　機　能　:　画面一時停止時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    //============================
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onDestroy() {
        if (scanner != null) scanner.onDestroy();
        super.onDestroy();
    }

    //==================================
    //　機　能　:　dispatch Key Eventの処理
    //　引　数　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... なし
    //==================================
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // トリガーキーは共通側で処理
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
