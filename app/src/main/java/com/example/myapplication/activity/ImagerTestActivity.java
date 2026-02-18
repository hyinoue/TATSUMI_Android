package com.example.myapplication.activity;

import android.os.Bundle;
import android.text.TextUtils;
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
 * <p>
 * 方針：
 * - SCANキーの全画面制御はしない（dispatchKeyEventで握らない）
 * - 読み取り種別は全許可（ALLOW_ALL_POLICY）
 */
public class ImagerTestActivity extends BaseActivity {

    // ===== UI =====
    private EditText etBarcode;
    private EditText etKind;
    private ScrollView svKindContent;
    private TextView tvKindContent;

    // ===== Scanner =====
    private DensoScannerController scanner;

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

        // ★この画面は “何でも読める” 前提（全許可）
        scanner = new DensoScannerController(this, new OnScanListener() {
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
        }, DensoScannerController.ALLOW_ALL_POLICY);

        // Manager生成開始
        scanner.onCreate();
    }

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

    @Override
    protected void onFunctionYellow() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) scanner.onResume();
        if (etBarcode != null) etBarcode.requestFocus();
    }

    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) scanner.onDestroy();
        super.onDestroy();
    }
}
