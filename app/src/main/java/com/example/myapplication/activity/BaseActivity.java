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

/**
 * BaseActivity（SCANキー全画面制御なし版）
 * <p>
 * 仕様（元のまま）：
 * - frmBase.lblErrMsg相当のバナー表示（一定時間で自動消去）
 * - frmBase.pnlWaitLong/Short相当のローディングオーバーレイ
 * - 物理キーF1～F4 → 青/赤/緑/黄 の onFunctionXxx に集約（Text空なら動かさない）
 * - 画面下4色ボタン（存在する画面だけ）→ onFunctionXxx に集約（Text空なら動かさない）
 * <p>
 * 変更点：
 * - SCANキー(501/230/233/234)の制御は一切しない（端末の既定動作に任せる）
 */
public class BaseActivity extends AppCompatActivity {

    public enum MsgDispMode {MsgBox, Label}

    public interface QuestionCallback {
        void onResult(boolean yes);
    }

    /**
     * ラベル(バナー)表示時間(ms)
     */
    protected int labelDisplayTimeMs = 2500;

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

    @Override
    protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettings.init(this);
        AppSettings.load();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppSettings.load();
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
        bindVersionNameIfExists();
    }

    private void bindVersionNameIfExists() {
        TextView tvVersion = findViewById(R.id.tvVersion);
        if (tvVersion != null) {
            String versionName = getAppVersionName();
            tvVersion.setText(versionName.isEmpty() ? "" : "Ver " + versionName);
        }
    }

    // ===== frmBase: ErrorProcess 相当 =====
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

    // ===== frmBase: ShowXxxMsg 相当 =====
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
