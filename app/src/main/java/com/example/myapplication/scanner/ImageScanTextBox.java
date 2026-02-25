package com.example.myapplication.scanner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　ImageScanTextBox ..... フォーカス中のみCode39をアプリ処理するEditText
//　　　　　　:　onFocusChanged ..... フォーカス変化に合わせてスキャナ開始/停止＋プロファイル反映
//　　　　　　:　onDetachedFromWindow ..... View破棄時にスキャナを破棄
//　　　　　　:　ensureScannerReady ..... スキャナ生成/Resume/プロファイル適用（フォーカスON時）
//　　　　　　:　pauseScanner ..... スキャナPause（フォーカスOFF時）
//　　　　　　:　destroyScanner ..... スキャナDestroy（View破棄時）
//　　　　　　:　resolveMaxLength ..... InputFilter.LengthFilterから最大長を推定
//　　　　　　:　findActivity ..... ContextからActivityを探索
//============================================================

//====================================
//　処理概要　:　ImageScanTextBoxクラス
//====================================
public class ImageScanTextBox extends AppCompatEditText {

    // デフォルトの待機（将来の拡張用。現状は未使用）
    private static final int DEFAULT_WAIT_DECODE_MS = 5000; // 読取待機時間デフォルト(ms)

    // 入力最小長（0以下なら1扱い）
    private int minLength = 0; // 最小文字数

    // デコード待機時間（将来の拡張用）
    private long waitDecodeMs = DEFAULT_WAIT_DECODE_MS; // 読取待機時間(ms)

    @Nullable
    private DensoScannerController scannerController; // DENSOスキャナ制御

    // BarcodeManager.create の多重呼び出し防止用
    private boolean scannerCreated = false; // スキャナ初期化済みフラグ

    //=================================================
    //　機　能　:　ImageScanTextBoxの初期化処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[ImageScanTextBox] ..... なし
    //=================================================
    public ImageScanTextBox(@NonNull Context context) {
        super(context);
        init();
    }

    //=================================================
    //　機　能　:　ImageScanTextBoxの初期化処理
    //　引　数　:　context ..... Context
    //　　　　　:　attrs ..... AttributeSet
    //　戻り値　:　[ImageScanTextBox] ..... なし
    //=================================================
    public ImageScanTextBox(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    //=================================================
    //　機　能　:　ImageScanTextBoxの初期化処理
    //　引　数　:　context ..... Context
    //　　　　　:　attrs ..... AttributeSet
    //　　　　　:　defStyleAttr ..... int
    //　戻り値　:　[ImageScanTextBox] ..... なし
    //=================================================
    public ImageScanTextBox(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    //============================
    //　機　能　:　初期設定を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void init() {
        // フォーカス時にソフトキーボードを表示しない（スキャナ入力前提）
        setShowSoftInputOnFocus(false);
    }

    //=================================================
    //　機　能　:　フォーカス変化時の処理
    //　引　数　:　focused ..... boolean
    //　　　　　:　direction ..... int
    //　　　　　:　previouslyFocusedRect ..... Rect
    //　戻り値　:　[void] ..... なし
    //=================================================
    @Override
    protected void onFocusChanged(boolean focused, int direction, @Nullable android.graphics.Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        // フォーカスON：スキャナ準備＆再開
        if (focused) {
            ensureScannerReady();
        } else {
            // フォーカスOFF：スキャナ停止（照射/受信抑止）
            pauseScanner();
        }

        // ★フォーカス変化直後に設定反映（CODE39_ONLY / NONE 切替）
        if (scannerController != null) {
            scannerController.refreshProfile("ImageScanTextBox.onFocusChanged");
        }
    }

    //=================================================
    //　機　能　:　ViewがWindowから外れた時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    @Override
    protected void onDetachedFromWindow() {
        // View破棄時にScanner/Managerを確実に解放
        destroyScanner();
        super.onDetachedFromWindow();
    }

    /**
     * ★重要：SCANキーをここで消費しない
     * （消費すると端末側のトリガー処理が動かず「反応しない」になりがち）
     * <p>
     * ※なので onKeyDown/onKeyUp はオーバーライドしない（=端末に渡す）
     */

    //=================================================
    //　機　能　:　スキャナを利用可能状態にする
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void ensureScannerReady() {

        // ContextからActivityを取得（BarcodeManager.createに必要）
        Activity activity = findActivity(getContext());
        if (activity == null) return;

        // Controller未生成なら作成（通知処理＋ポリシー設定）
        if (scannerController == null) {
            scannerController = new DensoScannerController(
                    activity,

                    // スキャン結果受信時の処理
                    (normalizedData, aim, denso) -> {
                        if (TextUtils.isEmpty(normalizedData)) return;

                        // 最小長チェック（0以下なら1扱い）
                        int effectiveMinLength = minLength <= 0 ? 1 : minLength;
                        if (normalizedData.length() < effectiveMinLength) return;

                        // EditTextの最大長（LengthFilter）を推定し、超過分は切り捨て
                        int maxLength = resolveMaxLength();
                        String value = normalizedData;
                        if (maxLength > 0 && value.length() > maxLength) {
                            value = value.substring(0, maxLength);
                        }

                        // テキスト反映＋カーソル末尾
                        setText(value);
                        setSelection(value.length());

                        // 既存処理に流したい場合：Enterを擬似発火（イベント駆動UI向け）
                        dispatchKeyEvent(new android.view.KeyEvent(
                                android.view.KeyEvent.ACTION_DOWN,
                                android.view.KeyEvent.KEYCODE_ENTER
                        ));
                        dispatchKeyEvent(new android.view.KeyEvent(
                                android.view.KeyEvent.ACTION_UP,
                                android.view.KeyEvent.KEYCODE_ENTER
                        ));
                    },

                    // フォーカス中のみ Code39 を許可するポリシー
                    new DensoScannerController.ScanPolicy() {
                        @Override
                        public boolean canAcceptResult() {
                            // フォーカス＋有効時のみ受信/照射許可
                            return hasFocus() && isEnabled();
                        }

                        @NonNull
                        @Override
                        public DensoScannerController.SymbologyProfile getSymbologyProfile() {
                            // フォーカス中のみCODE39_ONLY、それ以外はNONE
                            return (hasFocus() && isEnabled())
                                    ? DensoScannerController.SymbologyProfile.CODE39_ONLY
                                    : DensoScannerController.SymbologyProfile.NONE;
                        }

                        @Override
                        public boolean isSymbologyAllowed(@Nullable String aim,
                                                          @Nullable String denso,
                                                          @Nullable String displayName) {
                            // 念のため Code39 以外は弾く
                            return DensoScannerController.isCode39(aim, denso, displayName);
                        }
                    }
            );
        }

        // BarcodeManager.create は1回だけ呼ぶ
        if (!scannerCreated) {
            scannerController.onCreate();
            scannerCreated = true;
        }

        // 再開（claim/設定適用の準備）
        scannerController.onResume();

        // 現在のフォーカス状態に応じてプロファイルを反映
        scannerController.refreshProfile("ImageScanTextBox.ensureScannerReady");
    }

    //=================================================
    //　機　能　:　スキャナを一時停止する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void pauseScanner() {
        if (scannerController != null) {
            scannerController.onPause();
        }
    }

    //=================================================
    //　機　能　:　スキャナを破棄する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================================
    private void destroyScanner() {
        if (scannerController != null) {
            scannerController.onDestroy();
            scannerController = null;
        }
        scannerCreated = false;
    }

    //=================================================
    //　機　能　:　EditTextの最大長を推定する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 最大長（不明/未設定は0）
    //=================================================
    private int resolveMaxLength() {

        // 設定されているInputFilterを取得
        InputFilter[] filters = getFilters();
        if (filters == null) return 0;

        // LengthFilterを探して最大長を推定
        for (InputFilter filter : filters) {
            if (filter instanceof InputFilter.LengthFilter) {

                // filter.filter(...) は Spanned(dest) を要求するため現在値を使う
                Editable current = getText(); // Editable は Spanned
                Spanned dest = (current != null) ? current : new SpannableStringBuilder("");

                // 1文字追加できるかを試し、追加不可なら現在長が上限とみなす
                CharSequence out = filter.filter(
                        "A",
                        0,
                        1,
                        dest,
                        0,
                        dest.length()
                );

                // outが空（追加不可）なら、dest.length()が最大長
                if (out != null && out.length() == 0) {
                    return dest.length();
                }
            }
        }

        return 0;
    }

    //=================================================
    //　機　能　:　ContextからActivityを探索する
    //　引　数　:　context ..... Context
    //　戻り値　:　[Activity] ..... Activity（見つからなければnull）
    //=================================================
    @Nullable
    private Activity findActivity(@Nullable Context context) {

        // ContextWrapperを辿ってActivityを探す
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }

        return null;
    }
}
