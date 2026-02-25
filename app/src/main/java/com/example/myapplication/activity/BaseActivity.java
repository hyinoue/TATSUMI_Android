package com.example.myapplication.activity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.settings.AppSettings;
import com.example.myapplication.settings.HandyUtil;
import com.google.android.material.button.MaterialButton;

//============================================================
//　処理概要　:　BaseActivity（共通基底Activity）
//　　　　　　:　全画面共通のUI/操作を提供する。
//　　　　　　:　- バナー表示（一定時間で自動消去）
//　　　　　　:　- ローディングオーバーレイ（Long/Short）
//　　　　　　:　- 物理キーF1～F4 → 青/赤/緑/黄 の onFunctionXxx に集約（Text空なら動かさない）
//　　　　　　:　- 画面下4色ボタン（存在する画面だけ）→ onFunctionXxx に集約（Text空なら動かさない）
//　　　　　　:　- バージョン名表示（tvVersionがあれば "Ver x.y.z"）
//　関　　数　:　onCreate .................. 設定初期化/SoftInput制御
//　　　　　　:　onResume .................. 設定再読込
//　　　　　　:　setContentView ............ レイアウト設定後に共通UI紐付け
//　　　　　　:　afterSetContentView ....... Overlay付与/下部ボタン/Version表示
//　　　　　　:　errorProcess .............. 共通エラー処理（ローディング解除/バイブ+ブザー/ダイアログ）
//　　　　　　:　showErrorMsg/Warning/Info . メッセージ表示（MsgBox or Banner）
//　　　　　　:　showQuestion .............. Yes/No確認ダイアログ
//　　　　　　:　showBanner ................. バナー表示（一定時間で自動消去）
//　　　　　　:　showLoadingLong/Short ...... ローディング表示
//　　　　　　:　hideLoadingLong/Short ...... ローディング非表示
//　　　　　　:　dispatchKeyEvent .......... 物理キーF1～F4を関数に割当
//　　　　　　:　onFunctionRed/Blue/Green/Yellow 子画面でoverrideされる入口
//　　　　　　:　bindBottomButtonsIfExists . 下部4色ボタンを紐付け
//　　　　　　:　refreshBottomButtonsEnabled Text空＝無効化の反映
//　　　　　　:　ensureBaseOverlaysAttached Overlay(バナー/ローディング)をrootへ追加
//　　　　　　:　createBannerView .......... バナーView生成
//　　　　　　:　createLoadingOverlay ...... ローディングView生成
//　　　　　　:　dpToPx .................... dp→px変換
//　　　　　　:　getAppVersionName ......... アプリVersionName取得
//============================================================

public class BaseActivity extends AppCompatActivity {

    public enum MsgDispMode {MsgBox, Label}

    public interface QuestionCallback {
        void onResult(boolean yes);
    }

    /**
     * ラベル(バナー)表示時間(ms)
     */
    protected int labelDisplayTimeMs = 2500; // ラベル(バナー)表示時間(ms)

    private final Handler uiHandler = new Handler(Looper.getMainLooper()); // UIスレッドハンドラ

    // Label風バナー（frmBase.lblErrMsg相当）
    private TextView bannerView;         // バナー表示View
    private Runnable bannerHideRunnable; // バナー自動非表示Runnable

    // Wait overlay（frmBase.pnlWaitLong/Short相当）
    private FrameLayout overlayLong;  // 長時間処理用オーバーレイ
    private FrameLayout overlayShort; // 短時間処理用オーバーレイ

    // 画面下4色ボタン
    private MaterialButton btnBottomRed;    // 下部赤ボタン
    private MaterialButton btnBottomBlue;   // 下部青ボタン
    private MaterialButton btnBottomGreen;  // 下部緑ボタン
    private MaterialButton btnBottomYellow; // 下部黄ボタン
    private boolean bottomButtonsBound = false; // 下部ボタン紐付け済みフラグ

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 設定初期化/読み込み
        AppSettings.init(this);
        AppSettings.load();

        // SoftInputのレイアウト調整を行わない（既存仕様）
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();
        AppSettings.load();
    }

    //===================================
    //　機　能　:　setContentViewの拡張
    //　引　数　:　layoutResID ..... int
    //　戻り値　:　[void] ..... なし
    //===================================
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterSetContentView();
    }

    //===================================
    //　機　能　:　setContentViewの拡張
    //　引　数　:　view ..... View
    //　戻り値　:　[void] ..... なし
    //===================================
    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterSetContentView();
    }

    //===================================
    //　機　能　:　setContentViewの拡張
    //　引　数　:　view ..... View
    //　　　　　:　params ..... LayoutParams
    //　戻り値　:　[void] ..... なし
    //===================================
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        afterSetContentView();
    }

    //=========================================
    //　機　能　:　setContentView後の共通処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void afterSetContentView() {
        ensureBaseOverlaysAttached();
        bindBottomButtonsIfExists();
        bindVersionNameIfExists();
    }

    //=========================================
    //　機　能　:　Version表示（tvVersionがある場合）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void bindVersionNameIfExists() {
        TextView tvVersion = findViewById(R.id.tvVersion);
        if (tvVersion != null) {
            String versionName = getAppVersionName();
            tvVersion.setText(versionName.isEmpty() ? "" : "Ver " + versionName);
        }
    }

    //================================================================
    //　機　能　:　共通エラー処理（frmBase: ErrorProcess 相当）
    //　引　数　:　procName ..... String（ログ用途）
    //　　　　　:　ex ..... Exception
    //　戻り値　:　[void] ..... なし
    //================================================================
    protected void errorProcess(String procName, Exception ex) {
        hideLoadingLong();
        hideLoadingShort();
        playErrorFeedback();
        showDialog("エラー", "エラーが発生しました\n" + safeMessage(ex));
    }

    private String safeMessage(Exception ex) {
        if (ex == null) return "";
        String m = ex.getMessage();
        return (m == null) ? ex.getClass().getSimpleName() : m;
    }

    private void playErrorFeedback() {
        HandyUtil.playErrorBuzzer(this);
        HandyUtil.playVibrater(this);
    }

    //========================================================
    //　機　能　:　Error/Warning/Info 表示（frmBase: ShowXxxMsg 相当）
    //　引　数　:　msg ..... String
    //　　　　　:　mode ..... MsgDispMode（MsgBox or Label）
    //　戻り値　:　[void] ..... なし
    //========================================================
    public void showErrorMsg(String msg, MsgDispMode mode) {
        hideLoadingLong();
        hideLoadingShort();
        HandyUtil.playVibrater(this);
        if (mode == MsgDispMode.MsgBox) {
            HandyUtil.playErrorBuzzer(this);
            showDialog("エラー", msg);
        } else {
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this);
            showBanner(msg, BannerType.ERROR);
        }
    }

    public void showWarningMsg(String msg, MsgDispMode mode) {
        hideLoadingLong();
        hideLoadingShort();
        HandyUtil.playVibrater(this);
        if (mode == MsgDispMode.MsgBox) {
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this);
            showDialog("警告", msg);
        } else {
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this);
            showBanner(msg, BannerType.WARNING);
        }
    }

    public void showInfoMsg(String msg, MsgDispMode mode) {
        if (mode == MsgDispMode.MsgBox) {
            hideLoadingLong();
            hideLoadingShort();
            showDialog("情報", msg);
        } else {
            HandyUtil.playSuccessBuzzer(this);
            HandyUtil.playVibrater(this);
            showBanner(msg, BannerType.INFO);
        }
    }

    //=========================================
    //　機　能　:　確認ダイアログ表示（Yes/No）
    //　引　数　:　msg ..... String
    //　　　　　:　callback ..... QuestionCallback
    //　戻り値　:　[void] ..... なし
    //=========================================
    public void showQuestion(String msg, QuestionCallback callback) {
        hideLoadingLong();
        hideLoadingShort();

        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("はい", (d, w) -> {
                    if (callback != null) callback.onResult(true);
                })
                .setNegativeButton("いいえ", (d, w) -> {
                    if (callback != null) callback.onResult(false);
                })
                .show());
    }

    //=================================
    //　機　能　:　OKダイアログ表示
    //　引　数　:　title ..... String
    //　　　　　:　msg ..... String
    //　戻り値　:　[void] ..... なし
    //=================================
    private void showDialog(String title, String msg) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show());
    }

    // ===== Banner（Label表示相当） =====
    private enum BannerType {ERROR, WARNING, INFO}

    //=========================================
    //　機　能　:　バナー表示（一定時間で自動消去）
    //　引　数　:　msg ..... String
    //　　　　　:　type ..... BannerType
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void showBanner(String msg, BannerType type) {
        ensureBaseOverlaysAttached();

        runOnUiThread(() -> {
            if (bannerHideRunnable != null) uiHandler.removeCallbacks(bannerHideRunnable);

            bannerView.setText(msg);
            bannerView.bringToFront();

            if (type == BannerType.INFO) {
                bannerView.setBackgroundColor(Color.rgb(64, 64, 255)); // 青
                bannerView.setTextColor(Color.WHITE);
            } else {
                bannerView.setBackgroundColor(Color.rgb(255, 64, 64)); // 赤
                bannerView.setTextColor(Color.YELLOW);
            }

            bannerView.setVisibility(View.VISIBLE);

            bannerHideRunnable = () -> bannerView.setVisibility(View.GONE);
            uiHandler.postDelayed(bannerHideRunnable, labelDisplayTimeMs);
        });
    }

    //=================================
    //　機　能　:　loading Longを表示する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================
    protected void showLoadingLong() {
        ensureBaseOverlaysAttached();
        runOnUiThread(() -> {
            overlayLong.bringToFront();
            overlayLong.setVisibility(View.VISIBLE);
        });
    }

    //=================================
    //　機　能　:　loading Longを非表示にする
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================
    protected void hideLoadingLong() {
        if (overlayLong == null) return;
        runOnUiThread(() -> overlayLong.setVisibility(View.GONE));
    }

    //=================================
    //　機　能　:　loading Shortを表示する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================
    protected void showLoadingShort() {
        ensureBaseOverlaysAttached();
        runOnUiThread(() -> {
            overlayShort.bringToFront();
            overlayShort.setVisibility(View.VISIBLE);
        });
    }

    //=================================
    //　機　能　:　loading Shortを非表示にする
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================
    protected void hideLoadingShort() {
        if (overlayShort == null) return;
        runOnUiThread(() -> overlayShort.setVisibility(View.GONE));
    }

    //=========================================================
    //　機　能　:　Function keys（物理キー → onFunctionXxx に集約）
    //　引　数　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... True:消費
    //=========================================================
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            // 長押し連打防止
            if (event.getRepeatCount() > 0) return true;

            int keyCode = event.getKeyCode();

            // ★並び：左から「青・赤・緑・黄」
            // 物理キー：F1=青、F2=赤、F3=緑、F4=黄 に統一
            if (keyCode == KeyEvent.KEYCODE_F1) {
                if (canRun(btnBottomBlue)) onFunctionBlue();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_F2) {
                if (canRun(btnBottomRed)) onFunctionRed();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_F3) {
                if (canRun(btnBottomGreen)) onFunctionGreen();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_F4) {
                if (canRun(btnBottomYellow)) onFunctionYellow();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    //=========================================
    // 子画面で override（タップも物理キーもここに集約）
    //=========================================
    protected void onFunctionRed() {
    }

    protected void onFunctionBlue() {
    }

    protected void onFunctionGreen() {
    }

    protected void onFunctionYellow() {
    }

    //=========================================================
    //　機　能　:　画面下4色ボタンを紐付ける
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================================
    protected void bindBottomButtonsIfExists() {
        if (bottomButtonsBound) {
            refreshBottomButtonsEnabled();
            return;
        }

        MaterialButton red = asMaterialButton(findViewById(R.id.btnBottomRed));
        MaterialButton blue = asMaterialButton(findViewById(R.id.btnBottomBlue));
        MaterialButton green = asMaterialButton(findViewById(R.id.btnBottomGreen));
        MaterialButton yellow = asMaterialButton(findViewById(R.id.btnBottomYellow));

        // 4つ揃っている画面だけ対象
        if (red == null || blue == null || green == null || yellow == null) return;

        btnBottomRed = red;
        btnBottomBlue = blue;
        btnBottomGreen = green;
        btnBottomYellow = yellow;

        // タップ → onFunctionXxx へ集約（Text空なら動かさない）
        btnBottomRed.setOnClickListener(v -> {
            if (canRun(btnBottomRed)) onFunctionRed();
        });
        btnBottomBlue.setOnClickListener(v -> {
            if (canRun(btnBottomBlue)) onFunctionBlue();
        });
        btnBottomGreen.setOnClickListener(v -> {
            if (canRun(btnBottomGreen)) onFunctionGreen();
        });
        btnBottomYellow.setOnClickListener(v -> {
            if (canRun(btnBottomYellow)) onFunctionYellow();
        });

        bottomButtonsBound = true;
        refreshBottomButtonsEnabled();
    }

    //=========================================
    //　機　能　:　下部ボタンの有効/無効を反映する
    //　　　　　:　Textが空なら無効
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    protected void refreshBottomButtonsEnabled() {
        if (!bottomButtonsBound) return;

        applyEnabled(btnBottomRed, isActiveByText(btnBottomRed));
        applyEnabled(btnBottomBlue, isActiveByText(btnBottomBlue));
        applyEnabled(btnBottomGreen, isActiveByText(btnBottomGreen));
        applyEnabled(btnBottomYellow, isActiveByText(btnBottomYellow));
    }

    private boolean canRun(MaterialButton btn) {
        // 「下部ボタンが無い画面」でも物理キーは通す（元仕様）
        return !bottomButtonsBound || isActive(btn);
    }

    private boolean isActiveByText(MaterialButton btn) {
        if (btn == null) return false;
        CharSequence t = btn.getText();
        return t != null && t.toString().trim().length() > 0;
    }

    private boolean isActive(MaterialButton btn) {
        if (btn == null) return false;
        if (btn.getVisibility() != View.VISIBLE) return false;
        if (!btn.isEnabled()) return false;
        return isActiveByText(btn);
    }

    private void applyEnabled(MaterialButton btn, boolean enabled) {
        if (btn == null) return;
        btn.setEnabled(enabled);
    }

    private MaterialButton asMaterialButton(View v) {
        return (v instanceof MaterialButton) ? (MaterialButton) v : null;
    }

    //=========================================
    //　機　能　:　Overlay(バナー/ローディング)をrootへ付与する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================
    private void ensureBaseOverlaysAttached() {
        if (bannerView != null && overlayLong != null && overlayShort != null) return;

        runOnUiThread(() -> {
            View root = findViewById(android.R.id.content);
            if (!(root instanceof ViewGroup)) return;

            ViewGroup content = (ViewGroup) root;

            if (bannerView == null) {
                bannerView = createBannerView();
                content.addView(bannerView);
            }
            if (overlayLong == null) {
                overlayLong = createLoadingOverlay(true);
                content.addView(overlayLong);
            }
            if (overlayShort == null) {
                overlayShort = createLoadingOverlay(false);
                content.addView(overlayShort);
            }
        });
    }

    //============================
    //　機　能　:　Banner Viewを生成する
    //　引　数　:　なし
    //　戻り値　:　[TextView] ..... バナーView
    //============================
    private TextView createBannerView() {
        TextView tv = new TextView(this);
        tv.setVisibility(View.GONE);
        tv.setTextSize(18);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(16, 16, 16, 16);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(90)
        );
        lp.gravity = Gravity.BOTTOM;
        tv.setLayoutParams(lp);

        tv.setBackgroundColor(Color.RED);
        tv.setTextColor(Color.YELLOW);
        return tv;
    }

    //============================
    //　機　能　:　Loading Overlayを生成する
    //　引　数　:　isLong ..... boolean（Long/Short）
    //　戻り値　:　[FrameLayout] ..... overlay
    //============================
    private FrameLayout createLoadingOverlay(boolean isLong) {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true); // 背面操作ブロック
        overlay.setBackgroundColor(Color.rgb(0, 64, 0));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlay.setLayoutParams(lp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        boxLp.gravity = Gravity.CENTER;
        box.setLayoutParams(boxLp);

        TextView t1 = new TextView(this);
        t1.setTextSize(24);
        t1.setTextColor(Color.WHITE);
        t1.setGravity(Gravity.CENTER_HORIZONTAL);
        t1.setText(isLong ? "処理中です" : "処理中");
        t1.setPadding(0, dpToPx(8), 0, dpToPx(8));
        box.addView(t1);

        if (isLong) {
            TextView t2 = new TextView(this);
            t2.setTextSize(24);
            t2.setTextColor(Color.WHITE);
            t2.setGravity(Gravity.CENTER_HORIZONTAL);
            t2.setText("しばらくお待ちください…");
            t2.setPadding(0, dpToPx(8), 0, dpToPx(8));
            box.addView(t2);
        }

        overlay.addView(box);
        return overlay;
    }

    //============================
    //　機　能　:　dpをpxへ変換する
    //　引　数　:　dp ..... int
    //　戻り値　:　[int] ..... px
    //============================
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    //============================
    //　機　能　:　アプリVersionNameを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... VersionName（取得失敗は空）
    //============================
    protected String getAppVersionName() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi;
            if (Build.VERSION.SDK_INT >= 33) {
                pi = pm.getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                //noinspection deprecation
                pi = pm.getPackageInfo(getPackageName(), 0);
            }
            return pi.versionName;
        } catch (Exception e) {
            return "";
        }
    }
}
