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

import java.util.Locale;

/**
 * バーコードテスト画面（簡易版）Activity。
 * <p>
 * 方針：
 * - 読み取り種別は全許可（ALL）
 * - 受信データは常にアプリ処理する（フォーカス条件なし）
 * - 端末によっては dispatchKeyEvent 経由が必要なので scanner に渡す
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

        // “何でも読める” ポリシー
        scanner = new DensoScannerController(
                this,
                new OnScanListener() {
                    @Override
                    public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {

                        // 種別表示
                        if (etKind != null) {
                            String display = getBarcodeDisplayNameCompat(aim, denso);
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

                        // 次読み取り用
                        if (etBarcode != null) {
                            etBarcode.setText("");
                            etBarcode.requestFocus();
                        }
                    }
                },
                new DensoScannerController.ScanPolicy() {

                    @Override
                    public boolean canAcceptResult() {
                        // この画面は常に受ける
                        return true;
                    }

                    @Override
                    public DensoScannerController.SymbologyProfile getSymbologyProfile() {
                        // 何でもOK
                        return DensoScannerController.SymbologyProfile.ALL;
                    }

                    @Override
                    public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                        // 何でもOK
                        return true;
                    }
                }
        );

        scanner.onCreate();
    }

    private void setupBottomButtons() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        if (yellow != null) yellow.setText("終了");

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
        if (scanner != null) scanner.refreshProfile("onResume");
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // 端末によってはこれが必要（必要な場合だけ true が返る）
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * controllerのprivateに依存しない “種別表示” 判定
     */
    private String getBarcodeDisplayNameCompat(@Nullable String aim, @Nullable String denso) {
        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        if (a.startsWith("]A")) return "Code39";
        if (a.startsWith("]G")) return "Code93";
        if (a.startsWith("]C")) return "Code128";
        if (a.startsWith("]F")) return "Codabar(NW7)";
        if (a.startsWith("]I")) return "ITF(2of5)";
        if (a.startsWith("]E")) return "EAN/UPC";

        if (a.contains("CODE39") || d.contains("CODE39")) return "Code39";
        if (a.contains("CODE93") || d.contains("CODE93")) return "Code93";
        if (a.contains("CODE128") || d.contains("CODE128")) return "Code128";
        if (a.contains("QR") || d.contains("QR")) return "QR";
        if (a.contains("DATAMATRIX") || d.contains("DATAMATRIX")) return "DataMatrix";
        if (a.contains("PDF") || d.contains("PDF")) return "PDF417";

        return "";
    }
}
