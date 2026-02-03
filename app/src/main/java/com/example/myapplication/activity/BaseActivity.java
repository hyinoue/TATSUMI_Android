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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.example.myapplication.settings.AppSettings;
import com.example.myapplication.settings.HandyUtil;
import com.google.android.material.button.MaterialButton;

import java.util.List;


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
 */

//============================================================
//　処理概要　:　BaseActivityクラス
//============================================================

public class BaseActivity extends AppCompatActivity {

    // ===== public types =====

    public enum MsgDispMode {MsgBox, Label}

    public interface QuestionCallback {
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

    // System UI制御
    private boolean systemUiListenersAttached = false;
    private boolean systemUiLayoutListenerAttached = false;
    private boolean systemUiInsetsAnimationListenerAttached = false;

    private static final int SYSTEM_UI_HIDE_RETRY_COUNT = 5;          // ←少し増やす（出っぱなし対策）
    private static final int SYSTEM_UI_HIDE_RETRY_DELAY_MS = 120;
    private static final int SYSTEM_UI_HIDE_LOOP_DELAY_MS = 400;      // ←少し短く（出っぱなし対策）

    private Runnable systemUiHideRunnable;
    private int systemUiHideRetryCount = 0;

    private Runnable systemUiKeepHiddenRunnable;

    // EditText focus 対策（多重アタッチ防止）
    private boolean editTextFocusHookAttached = false;

    @Override
    protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettings.init(this);
        AppSettings.load();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        applyFullScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFullScreen();
        startSystemUiHideLoop();
    }

    @Override
    protected void onPause() {
        stopSystemUiHideLoop();
        super.onPause();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterSetContentView();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterSetContentView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        afterSetContentView();
    }

    private void afterSetContentView() {
        ensureBaseOverlaysAttached();
        bindBottomButtonsIfExists();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyFullScreen();
            scheduleHideSystemBars(getWindow().getDecorView());
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        scheduleHideSystemBars(getWindow().getDecorView());
    }

    // ===== Full screen =====

    protected void applyFullScreen() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Edge-to-edge（InsetsControllerで制御）
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View decorView = getWindow().getDecorView();
        applyImmersiveFlags(decorView);  // 旧フラグ（OEM保険）
        hideSystemBars(decorView);       // 新方式中心
        ensureSystemUiListeners(decorView);
    }

    /**
     * 旧方式（OEM端末の保険）
     */
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

    private void ensureSystemUiListeners(View decorView) {
        if (systemUiListenersAttached) return;

        // 旧コールバック：表示されたら隠す（保険）
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0
                    || (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                scheduleHideSystemBars(decorView);
            }
        });

        // ★重要：insetsが来たときに「bars visible」を検知して即hide
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, insets) -> {
            boolean barsVisible =
                    insets.isVisible(WindowInsetsCompat.Type.navigationBars())
                            || insets.isVisible(WindowInsetsCompat.Type.statusBars());

            if (barsVisible) {
                // ここで「出っぱなし」になるので、即座に複数回叩く
                forceHideNowAndSoon(decorView);
            }

            // 通常の保険
            scheduleHideSystemBars(decorView);

            // ★insetsは改造しない（改造するとOEMで出っぱなしが悪化する例がある）
            return insets;
        });

        // レイアウト変化でも隠す（IME表示時など）
        if (!systemUiLayoutListenerAttached) {
            decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                scheduleHideSystemBars(decorView);
            });
            systemUiLayoutListenerAttached = true;
        }

        // ★IMEアニメ中にも毎フレームhide（Android 13で効果大）
        if (!systemUiInsetsAnimationListenerAttached) {
            ViewCompat.setWindowInsetsAnimationCallback(
                    decorView,
                    new WindowInsetsAnimationCompat.Callback(
                            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
                    ) {
                        @Override
                        public WindowInsetsAnimationCompat.BoundsCompat onStart(
                                WindowInsetsAnimationCompat animation,
                                WindowInsetsAnimationCompat.BoundsCompat bounds
                        ) {
                            hideSystemBars(decorView);
                            return bounds;
                        }

                        @Override
                        public WindowInsetsCompat onProgress(
                                WindowInsetsCompat insets,
                                List<WindowInsetsAnimationCompat> runningAnimations
                        ) {
                            hideSystemBars(decorView);
                            return insets;
                        }

                        @Override
                        public void onEnd(WindowInsetsAnimationCompat animation) {
                            forceHideNowAndSoon(decorView);
                            scheduleHideSystemBars(decorView);
                        }
                    }
            );
            systemUiInsetsAnimationListenerAttached = true;
        }

        systemUiListenersAttached = true;
    }

    /**
     * Android 13対策：hideを強化（InsetsController中心＋旧フラグ保険）
     */
    private void hideSystemBars(View decorView) {
        applyImmersiveFlags(decorView); // OEM保険

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), decorView);
        if (controller != null) {
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

            // 明示的に分けて隠す（端末依存で systemBars() だけだと効かないことがある）
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.hide(WindowInsetsCompat.Type.navigationBars());
        }
    }

    /**
     * 「出たら出っぱなし」を潰す：今すぐ＋少し後にも数回hide
     */
    private void forceHideNowAndSoon(View decorView) {
        uiHandler.post(() -> {
            applyImmersiveFlags(decorView);
            hideSystemBars(decorView);
        });
        uiHandler.postDelayed(() -> {
            applyImmersiveFlags(decorView);
            hideSystemBars(decorView);
        }, 60);
        uiHandler.postDelayed(() -> {
            applyImmersiveFlags(decorView);
            hideSystemBars(decorView);
        }, 160);
        uiHandler.postDelayed(() -> {
            applyImmersiveFlags(decorView);
            hideSystemBars(decorView);
        }, 320);
    }

    private void scheduleHideSystemBars(View decorView) {
        if (systemUiHideRunnable != null) {
            uiHandler.removeCallbacks(systemUiHideRunnable);
        }
        systemUiHideRetryCount = 0;

        systemUiHideRunnable = new Runnable() {
            @Override
            public void run() {
                applyImmersiveFlags(decorView);
                hideSystemBars(decorView);

                // しつこく数回叩く（出っぱなし対策）
                if (systemUiHideRetryCount < SYSTEM_UI_HIDE_RETRY_COUNT) {
                    systemUiHideRetryCount++;
                    uiHandler.postDelayed(this, SYSTEM_UI_HIDE_RETRY_DELAY_MS);
                }
            }
        };
        uiHandler.post(systemUiHideRunnable);
    }

    private void startSystemUiHideLoop() {
        if (systemUiKeepHiddenRunnable != null) return;

        View decorView = getWindow().getDecorView();
        systemUiKeepHiddenRunnable = new Runnable() {
            @Override
            public void run() {
                applyImmersiveFlags(decorView);
                hideSystemBars(decorView);
                uiHandler.postDelayed(this, SYSTEM_UI_HIDE_LOOP_DELAY_MS);
            }
        };
        uiHandler.post(systemUiKeepHiddenRunnable);
    }

    private void stopSystemUiHideLoop() {
        if (systemUiKeepHiddenRunnable == null) return;
        uiHandler.removeCallbacks(systemUiKeepHiddenRunnable);
        systemUiKeepHiddenRunnable = null;
    }


    // ===== frmBase: ErrorProcess 相当 =====

    protected void errorProcess(String procName, Exception ex) {
        hideLoadingLong();
        hideLoadingShort();
        showErrorMsg("エラーが発生しました\n" + safeMessage(ex), MsgDispMode.MsgBox);
    }

    protected void errorProcess(String procName, String message, Exception ex) {
        hideLoadingLong();
        hideLoadingShort();
        showErrorMsg("エラーが発生しました\n" + procName + "\n" + message + "\n" + safeMessage(ex), MsgDispMode.MsgBox);
    }

    private String safeMessage(Exception ex) {
        if (ex == null) return "";
        String m = ex.getMessage();
        return (m == null) ? ex.getClass().getSimpleName() : m;
    }

    // ===== frmBase: ShowXxxMsg 相当 =====

    public void showErrorMsg(String msg, MsgDispMode mode) {
        hideLoadingLong();
        hideLoadingShort();
        if (mode == MsgDispMode.MsgBox) {
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this);
            showDialog("エラー", msg);
        } else {
            HandyUtil.playErrorBuzzer(this);
            HandyUtil.playVibrater(this, 1);
            showBanner(msg, BannerType.ERROR);
        }
    }

    public void showWarningMsg(String msg, MsgDispMode mode) {
        hideLoadingLong();
        hideLoadingShort();
        HandyUtil.playErrorBuzzer(this);
        HandyUtil.playVibrater(this);
        if (mode == MsgDispMode.MsgBox) {
            showDialog("警告", msg);
        } else {
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

    private void showDialog(String title, String msg) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show());
    }

    // ===== Banner（Label表示相当） =====

    private enum BannerType {ERROR, WARNING, INFO}

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

    protected void showLoadingLong() {
        ensureBaseOverlaysAttached();
        runOnUiThread(() -> {
            overlayLong.bringToFront();
            overlayLong.setVisibility(View.VISIBLE);
        });
    }

    protected void hideLoadingLong() {
        if (overlayLong == null) return;
        runOnUiThread(() -> overlayLong.setVisibility(View.GONE));
    }

    protected void showLoadingShort() {
        ensureBaseOverlaysAttached();
        runOnUiThread(() -> {
            overlayShort.bringToFront();
            overlayShort.setVisibility(View.VISIBLE);
        });
    }

    protected void hideLoadingShort() {
        if (overlayShort == null) return;
        runOnUiThread(() -> overlayShort.setVisibility(View.GONE));
    }

    // ===== Function keys（物理キー → onFunctionXxx に集約） =====

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
    protected void onFunctionRed() {
    }

    protected void onFunctionBlue() {
    }

    protected void onFunctionGreen() {
    }

    protected void onFunctionYellow() {
    }

    // ===== ★画面下4色ボタン連動 =====

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

    // ===== Overlays attach =====

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

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ===== version helper =====

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
