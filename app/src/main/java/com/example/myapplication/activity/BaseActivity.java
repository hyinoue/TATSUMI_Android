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
import androidx.core.view.WindowCompat;

import com.example.myapplication.R;
import com.example.myapplication.settings.HandyUtil;
import com.google.android.material.button.MaterialButton;


/**
 * 動作はそのままに、重複を削って短くした BaseActivity
 * <p>
 * 仕様（元のまま）：
 * - ActionBar非表示＋全画面（status/navigation bar hide、swipeで一時表示）
 * - frmBase.lblErrMsg相当のバナー表示（一定時間で自動消去）
 * - frmBase.pnlWaitLong/Short相当のローディングオーバーレイ
 * - 物理キーF1～F4 → 青/赤/緑/黄 の onFunctionXxx に集約（Text空なら動かさない）
 * - 画面下4色ボタン（存在する画面だけ）→ onFunctionXxx に集約（Text空なら動かさない）
 * <p>
 * Android 13対策：
 * - EditTextフォーカス/IME表示のタイミングでナビバーが「出っぱなし」になる端末があるため、
 * (1) WindowInsetsControllerCompat を中心に hide
 * (2) insetsで「barsがvisible」になった瞬間を検知→即hideを複数回
 * (3) IMEアニメ中も onProgress でhideを当て続ける
 * <p>
 * このクラスは共通UI処理の集約点として使い、各Activityは必要なところだけオーバーライドする。
 * - setContentView後のフルスクリーン適用とボタンバインドを自動実行
 * - F1〜F4/画面下ボタンの入力を onFunctionXxx に一本化
 * - バナー/Waitオーバーレイ表示のための共通APIを提供
 * - 画面横断で使うユーティリティ（バージョン表示、フォーカス制御等）を保持
 */

//=============================
//　処理概要　:　BaseActivityクラス
//=============================

public class BaseActivity extends AppCompatActivity {

    // ===== public types =====

    public enum MsgDispMode {MsgBox, Label}

    public interface QuestionCallback {
        //==============================
        //　機　能　:　on Resultの処理
        //　引　数　:　yes ..... boolean
        //　戻り値　:　[void] ..... なし
        //==============================
        void onResult(boolean yes);
    }

    // ===== settings =====

    /**
     * ラベル(バナー)表示時間(ms)
     */
    protected int labelDisplayTimeMs = 2500;

    // ===== internal =====

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Label風バナー（frmBase.lblErrMsg相当）
    private TextView bannerView;
    private Runnable bannerHideRunnable;

    // Wait overlay（frmBase.pnlWaitLong/Short相当）
    private FrameLayout overlayLong;
    private FrameLayout overlayShort;

    // 画面下4色ボタン
    private MaterialButton btnBottomRed;
    private MaterialButton btnBottomBlue;
    private MaterialButton btnBottomGreen;
    private MaterialButton btnBottomYellow;
    private boolean bottomButtonsBound = false;

    //=======================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... android.os.Bundle
    //　戻り値　:　[void] ..... なし
    //=======================================================
    @Override
    protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        applyFullScreen();
    }

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();
        applyFullScreen();
    }

    //============================
    //　機　能　:　画面一時停止時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onPause() {
        super.onPause();
    }

    //==================================
    //　機　能　:　content Viewを設定する
    //　引　数　:　layoutResID ..... int
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterSetContentView();
    }

    //==============================
    //　機　能　:　content Viewを設定する
    //　引　数　:　view ..... View
    //　戻り値　:　[void] ..... なし
    //==============================
    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterSetContentView();
    }

    //================================================
    //　機　能　:　content Viewを設定する
    //　引　数　:　view ..... View
    //　　　　　:　params ..... ViewGroup.LayoutParams
    //　戻り値　:　[void] ..... なし
    //================================================
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        afterSetContentView();
    }
    //======================================
    //　機　能　:　after Set Content Viewの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================================

    private void afterSetContentView() {
        ensureBaseOverlaysAttached();
        bindBottomButtonsIfExists();
    }

    //=======================================
    //　機　能　:　on Window Focus Changedの処理
    //　引　数　:　hasFocus ..... boolean
    //　戻り値　:　[void] ..... なし
    //=======================================
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyFullScreen();
        }
    }

    //===================================
    //　機　能　:　on User Interactionの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
    }

    // ===== Full screen =====
    //=================================
    //　機　能　:　apply Full Screenの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================

    protected void applyFullScreen() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Edge-to-edge（InsetsControllerで制御）
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View decorView = getWindow().getDecorView();
        applyImmersiveFlags(decorView);
    }

    /**
     * ナビゲーションバー非表示
     */
    //=====================================
    //　機　能　:　apply Immersive Flagsの処理
    //　引　数　:　decorView ..... View
    //　戻り値　:　[void] ..... なし
    //=====================================
    private void applyImmersiveFlags(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }


    // ===== frmBase: ErrorProcess 相当 =====
    //==================================
    //　機　能　:　error Processの処理
    //　引　数　:　procName ..... String
    //　　　　　:　ex ..... Exception
    //　戻り値　:　[void] ..... なし
    //==================================

    protected void errorProcess(String procName, Exception ex) {
        hideLoadingLong();
        hideLoadingShort();
        playErrorFeedback();
        showDialog("エラー", "エラーが発生しました\n" + safeMessage(ex));
    }
    //==================================
    //　機　能　:　error Processの処理
    //　引　数　:　procName ..... String
    //　　　　　:　message ..... String
    //　　　　　:　ex ..... Exception
    //　戻り値　:　[void] ..... なし
    //==================================

    protected void errorProcess(String procName, String message, Exception ex) {
        hideLoadingLong();
        hideLoadingShort();
        playErrorFeedback();
        String detail = "エラーが発生しました\n" + procName + "\n" + message + "\n" + safeMessage(ex);
        showDialog("エラー", detail);
    }
    //===============================
    //　機　能　:　safe Messageの処理
    //　引　数　:　ex ..... Exception
    //　戻り値　:　[String] ..... なし
    //===============================

    private String safeMessage(Exception ex) {
        if (ex == null) return "";
        String m = ex.getMessage();
        return (m == null) ? ex.getClass().getSimpleName() : m;
    }
    //===================================
    //　機　能　:　play Error Feedbackの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================

    private void playErrorFeedback() {
        HandyUtil.playErrorBuzzer(this);
        HandyUtil.playVibrater(this);
    }

    // ===== frmBase: ShowXxxMsg 相当 =====
    //===================================
    //　機　能　:　show Error Msgの処理
    //　引　数　:　msg ..... String
    //　　　　　:　mode ..... MsgDispMode
    //　戻り値　:　[void] ..... なし
    //===================================

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
    //===================================
    //　機　能　:　show Warning Msgの処理
    //　引　数　:　msg ..... String
    //　　　　　:　mode ..... MsgDispMode
    //　戻り値　:　[void] ..... なし
    //===================================

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
    //===================================
    //　機　能　:　show Info Msgの処理
    //　引　数　:　msg ..... String
    //　　　　　:　mode ..... MsgDispMode
    //　戻り値　:　[void] ..... なし
    //===================================

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
    //============================================
    //　機　能　:　show Questionの処理
    //　引　数　:　msg ..... String
    //　　　　　:　callback ..... QuestionCallback
    //　戻り値　:　[void] ..... なし
    //============================================

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
    //===============================
    //　機　能　:　show Dialogの処理
    //　引　数　:　title ..... String
    //　　　　　:　msg ..... String
    //　戻り値　:　[void] ..... なし
    //===============================

    private void showDialog(String title, String msg) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show());
    }

    // ===== Banner（Label表示相当） =====

    private enum BannerType {ERROR, WARNING, INFO}
    //==================================
    //　機　能　:　show Bannerの処理
    //　引　数　:　msg ..... String
    //　　　　　:　type ..... BannerType
    //　戻り値　:　[void] ..... なし
    //==================================

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

    // ===== Loading overlay =====
    //=================================
    //　機　能　:　show Loading Longの処理
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
    //　機　能　:　hide Loading Longの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================

    protected void hideLoadingLong() {
        if (overlayLong == null) return;
        runOnUiThread(() -> overlayLong.setVisibility(View.GONE));
    }
    //==================================
    //　機　能　:　show Loading Shortの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================

    protected void showLoadingShort() {
        ensureBaseOverlaysAttached();
        runOnUiThread(() -> {
            overlayShort.bringToFront();
            overlayShort.setVisibility(View.VISIBLE);
        });
    }
    //==================================
    //　機　能　:　hide Loading Shortの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================

    protected void hideLoadingShort() {
        if (overlayShort == null) return;
        runOnUiThread(() -> overlayShort.setVisibility(View.GONE));
    }

    // ===== Function keys（物理キー → onFunctionXxx に集約） =====

    //==================================
    //　機　能　:　dispatch Key Eventの処理
    //　引　数　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... なし
    //==================================
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

    // 子画面で override（タップも物理キーもここに集約）
    //===============================
    //　機　能　:　on Function Redの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===============================
    protected void onFunctionRed() {
    }
    //================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    protected void onFunctionBlue() {
    }
    //=================================
    //　機　能　:　on Function Greenの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================

    protected void onFunctionGreen() {
    }
    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================

    protected void onFunctionYellow() {
    }

    // ===== ★画面下4色ボタン連動 =====
    //=============================================
    //　機　能　:　bind Bottom Buttons If Existsの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================================

    protected void bindBottomButtonsIfExists() {
        if (bottomButtonsBound) {
            refreshBottomButtonsEnabled();
            return;
        }

        MaterialButton red = asMaterialButton(findViewById(R.id.btnBottomRed));
        MaterialButton blue = asMaterialButton(findViewById(R.id.btnBottomBlue));
        MaterialButton green = asMaterialButton(findViewById(R.id.btnBottomGreen));
        MaterialButton yellow = asMaterialButton(findViewById(R.id.btnBottomYellow));

        if (red == null || blue == null || green == null || yellow == null) return;

        btnBottomRed = red;
        btnBottomBlue = blue;
        btnBottomGreen = green;
        btnBottomYellow = yellow;

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
    //========================================
    //　機　能　:　bottom Buttons Enabledを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================================

    protected void refreshBottomButtonsEnabled() {
        if (!bottomButtonsBound) return;

        applyEnabled(btnBottomRed, isActiveByText(btnBottomRed));
        applyEnabled(btnBottomBlue, isActiveByText(btnBottomBlue));
        applyEnabled(btnBottomGreen, isActiveByText(btnBottomGreen));
        applyEnabled(btnBottomYellow, isActiveByText(btnBottomYellow));
    }
    //=====================================
    //　機　能　:　can Runの処理
    //　引　数　:　btn ..... MaterialButton
    //　戻り値　:　[boolean] ..... なし
    //=====================================

    private boolean canRun(MaterialButton btn) {
        // 「下部ボタンが無い画面」でも物理キーは通す（元仕様）
        return !bottomButtonsBound || isActive(btn);
    }
    //=====================================
    //　機　能　:　active By Textを判定する
    //　引　数　:　btn ..... MaterialButton
    //　戻り値　:　[boolean] ..... なし
    //=====================================

    private boolean isActiveByText(MaterialButton btn) {
        if (btn == null) return false;
        CharSequence t = btn.getText();
        return t != null && t.toString().trim().length() > 0;
    }
    //=====================================
    //　機　能　:　activeを判定する
    //　引　数　:　btn ..... MaterialButton
    //　戻り値　:　[boolean] ..... なし
    //=====================================

    private boolean isActive(MaterialButton btn) {
        if (btn == null) return false;
        if (btn.getVisibility() != View.VISIBLE) return false;
        if (!btn.isEnabled()) return false;
        return isActiveByText(btn);
    }
    //=====================================
    //　機　能　:　apply Enabledの処理
    //　引　数　:　btn ..... MaterialButton
    //　　　　　:　enabled ..... boolean
    //　戻り値　:　[void] ..... なし
    //=====================================

    private void applyEnabled(MaterialButton btn, boolean enabled) {
        if (btn == null) return;
        btn.setEnabled(enabled);
    }
    //======================================
    //　機　能　:　as Material Buttonの処理
    //　引　数　:　v ..... View
    //　戻り値　:　[MaterialButton] ..... なし
    //======================================

    private MaterialButton asMaterialButton(View v) {
        return (v instanceof MaterialButton) ? (MaterialButton) v : null;
    }

    // ===== Overlays attach =====
    //=============================================
    //　機　能　:　ensure Base Overlays Attachedの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================================

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
    //================================
    //　機　能　:　banner Viewを作成する
    //　引　数　:　なし
    //　戻り値　:　[TextView] ..... なし
    //================================

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
    //===================================
    //　機　能　:　loading Overlayを作成する
    //　引　数　:　isLong ..... boolean
    //　戻り値　:　[FrameLayout] ..... なし
    //===================================

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
    //===========================
    //　機　能　:　dp To Pxの処理
    //　引　数　:　dp ..... int
    //　戻り値　:　[int] ..... なし
    //===========================

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ===== version helper =====
    //==================================
    //　機　能　:　app Version Nameを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //==================================

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
