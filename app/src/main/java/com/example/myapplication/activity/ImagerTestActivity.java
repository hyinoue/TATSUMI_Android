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

//============================================================
//　処理概要　:　バーコードテスト画面
//　　　　　　:　対象入力欄（etBarcode）にフォーカス中のみスキャン結果を受け付ける。
//　　　　　　:　読取種別は全許可（ALL）で、読取データは履歴として画面に追記する。
//　　　　　　:　端末差異により dispatchKeyEvent 経由でスキャンキー入力が来る場合があるため、
//　　　　　　:　scanner にイベントを渡してハンドリングする。
//　関　　数　:　onCreate                    ..... 画面生成/初期化、スキャナ初期化
//　　　　　　:　setupBottomButtons          ..... 下部ボタン設定
//　　　　　　:　onFunctionYellow            ..... (黄)終了
//　　　　　　:　onResume                    ..... 画面復帰時にスキャナ再開/フォーカス復元
//　　　　　　:　onPause                     ..... 画面非表示時にスキャナ一時停止
//　　　　　　:　onDestroy                   ..... 画面破棄時にスキャナ破棄
//　　　　　　:　dispatchKeyEvent            ..... 物理キーイベントをスキャナへ委譲
//　　　　　　:　getBarcodeDisplayNameCompat ..... AIM/DENSO文字列から種別表示名を推定
//============================================================

public class ImagerTestActivity extends BaseActivity {

    // ===== UI =====

    /**
     * 読取対象（このEditTextにフォーカス中のみスキャンを受け付ける）
     */
    private EditText etBarcode; // 読取対象入力欄

    /**
     * 読取種別表示（Code39/QRなどの判定結果を表示）
     */
    private EditText etKind; // 読取種別表示欄

    /**
     * 履歴表示領域（スクロール用）
     */
    private ScrollView svKindContent; // 履歴スクロール領域

    /**
     * 履歴の本文（読取データを改行区切りで追記）
     */
    private TextView tvKindContent; // 読取履歴表示

    // ===== Scanner =====

    /**
     * DENSOスキャナ制御（本画面専用）
     */
    private DensoScannerController scanner; // DENSOスキャナ制御

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画面レイアウトを設定
        setContentView(R.layout.activity_imager_test);

        // ---- View取得 ----
        etBarcode = findViewById(R.id.etBarcode);
        etKind = findViewById(R.id.etKind);
        svKindContent = findViewById(R.id.scrollKind);
        tvKindContent = findViewById(R.id.tvKindContent);

        // ---- 下部ボタン設定 ----
        setupBottomButtons();

        // ---- 入力欄初期化（フォーカス固定・キーボード抑止）----
        if (etBarcode != null) {
            // スキャナ入力前提のためソフトキーボードは表示しない
            etBarcode.setShowSoftInputOnFocus(false);

            // 初期状態で空にしてフォーカスを当てる（読取開始しやすくする）
            etBarcode.setText("");
            etBarcode.requestFocus();
        }

        if (etKind != null) {
            // 種別表示用もキーボードは不要
            etKind.setShowSoftInputOnFocus(false);

            // 初期表示は空
            etKind.setText("");
        }

        if (tvKindContent != null) {
            // 履歴表示を初期化
            tvKindContent.setText("");
        }

        // ---- スキャナ初期化 ----
        // “何でも読める” ポリシー（フォーカス中はALL、非フォーカスはNONE）
        scanner = new DensoScannerController(
                this,

                //===============================
                // Scan結果受け取り
                //===============================
                new OnScanListener() {
                    @Override
                    public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {

                        // ---- 種別表示（AIM/DENSOから推定して表示） ----
                        if (etKind != null) {
                            String display = getBarcodeDisplayNameCompat(aim, denso);
                            etKind.setText(!TextUtils.isEmpty(display) ? display : "");
                        }

                        // ---- 履歴追記（読み取ったデータのみ） ----
                        if (tvKindContent != null) {
                            String current = tvKindContent.getText() != null
                                    ? tvKindContent.getText().toString()
                                    : "";

                            // 初回はそのまま、2回目以降は改行して追記
                            if (TextUtils.isEmpty(current)) {
                                tvKindContent.setText(normalizedData);
                            } else {
                                tvKindContent.setText(current + "\n" + normalizedData);
                            }
                        }

                        // ---- 最下部へスクロール（新しい履歴が見えるように） ----
                        if (svKindContent != null) {
                            svKindContent.post(() -> svKindContent.fullScroll(ScrollView.FOCUS_DOWN));
                        }

                        // ---- 次読み取りのために入力欄を空にしてフォーカス維持 ----
                        if (etBarcode != null) {
                            etBarcode.setText("");
                            etBarcode.requestFocus();
                        }
                    }
                },

                //===============================
                // 受け入れ可否/プロファイル制御ポリシー
                //===============================
                new DensoScannerController.ScanPolicy() {

                    @Override
                    public boolean canAcceptResult() {
                        // 対象入力欄が存在し、フォーカスがあり、かつ有効なときのみ受け付ける
                        return etBarcode != null && etBarcode.hasFocus() && etBarcode.isEnabled();
                    }

                    @Override
                    public DensoScannerController.SymbologyProfile getSymbologyProfile() {
                        // フォーカス中のみ「何でもOK」、それ以外は無効
                        return canAcceptResult()
                                ? DensoScannerController.SymbologyProfile.ALL
                                : DensoScannerController.SymbologyProfile.NONE;
                    }

                    @Override
                    public boolean isSymbologyAllowed(@Nullable String aim, @Nullable String denso, @Nullable String displayName) {
                        // SymbologyProfileで制御しているため、ここは常に許可
                        return true;
                    }
                }
        );

        // DensoScannerControllerのライフサイクル開始
        scanner.onCreate();
    }

    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    private void setupBottomButtons() {
        // 黄：終了のみ使用
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        if (yellow != null) yellow.setText("終了");

        // その他は空にして無効化（BaseActivityでdisable＋薄表示）
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
        // 画面終了
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

        // スキャナ再開（読み取り可能状態へ）
        if (scanner != null) scanner.onResume();

        // プロファイル再反映（フォーカス状態等の変化があった場合に備える）
        if (scanner != null) scanner.refreshProfile("onResume");

        // 読取欄へフォーカスを戻す
        if (etBarcode != null) etBarcode.requestFocus();
    }

    //============================
    //　機　能　:　画面非表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onPause() {
        // スキャナ停止（バックグラウンドでの読み取り抑止）
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
        // スキャナ破棄（リソース解放）
        if (scanner != null) scanner.onDestroy();
        super.onDestroy();
    }

    //=================================
    //　機　能　:　dispatch Key Eventの処理
    //　引　数　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... true:処理済み / false:未処理
    //=================================
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // 端末によってはSCANキー等がKeyEventとして飛んでくるため、scannerに委譲する
        // （必要な場合のみ true が返る）
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    //=========================================================
    //　機　能　:　バーコード種別の表示名を推定して返す
    //　引　数　:　aim ..... String (AIM識別子)
    //　　　　　:　denso ..... String (DENSO独自識別/ログ用)
    //　戻り値　:　[String] ..... 種別表示名（不明時は空文字）
    //=========================================================

    /**
     * controllerのprivateに依存しない “種別表示” 判定
     */
    private String getBarcodeDisplayNameCompat(@Nullable String aim, @Nullable String denso) {
        // null対策＋大文字化（判定を単純化）
        String a = aim == null ? "" : aim.toUpperCase(Locale.ROOT);
        String d = denso == null ? "" : denso.toUpperCase(Locale.ROOT);

        // ---- AIM識別子（]A0 等）での判定 ----
        // ※AIMのプレフィックスは代表例。読み取り機種/設定で変わる場合あり。
        if (a.startsWith("]A")) return "Code39";
        if (a.startsWith("]G")) return "Code93";
        if (a.startsWith("]C")) return "Code128";
        if (a.startsWith("]F")) return "Codabar(NW7)";
        if (a.startsWith("]I")) return "ITF(2of5)";
        if (a.startsWith("]E")) return "EAN/UPC";

        // ---- 文字列含有でのフォールバック判定 ----
        if (a.contains("CODE39") || d.contains("CODE39")) return "Code39";
        if (a.contains("CODE93") || d.contains("CODE93")) return "Code93";
        if (a.contains("CODE128") || d.contains("CODE128")) return "Code128";
        if (a.contains("QR") || d.contains("QR")) return "QR";
        if (a.contains("DATAMATRIX") || d.contains("DATAMATRIX")) return "DataMatrix";
        if (a.contains("PDF") || d.contains("PDF")) return "PDF417";

        // 判定不能
        return "";
    }
}
