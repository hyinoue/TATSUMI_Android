package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.dao.KakuninMeisaiWorkDao;
import com.example.myapplication.db.dao.SystemDao;
import com.example.myapplication.db.dao.SyukkaContainerDao;
import com.example.myapplication.db.dao.SyukkaMeisaiDao;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.dao.YoteiDao;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//============================================================
//　処理概要　:　ServiceMenuActivityクラス
//============================================================

/**
 * サービスメニュー画面Activity。
 *
 * <p>保守/診断向けの機能への入口をまとめた画面で、
 * DB確認、データクリア、通信/バーコードテストなどを提供する。</p>
 *
 * <p>アクセス制限が必要な機能はパスワード確認を通して遷移する。</p>
 */
public class ServiceMenuActivity extends BaseActivity {

    private static final int SYSTEM_RENBAN = 1;
    private static final Set<String> SERVICE_PASSWORDS =
            new HashSet<>(Arrays.asList("2441", "4546", "4549", "4522", "4523"));

    // 画面メニュー（TextView）
    private TextView menu1;
    private TextView menu2;
    private TextView menu3;
    private TextView menu4;
    private TextView menu5;
    private TextView menu6;
    private TextView menu7;
    private TextView menu8; // XMLに menu8（サーバー切替）を追加した場合
    private ExecutorService io;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_menu);

        io = Executors.newSingleThreadExecutor();

        // ▼ 下ボタン（include）を取得
        View bottom = findViewById(R.id.includeBottomButtons);

        // ▼ 各ボタンを取得
        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        // ▼ 文字設定（画面ごとにここだけ変える）
        btnBlue.setText("");
        btnRed.setText("");
        btnGreen.setText("");
        btnYellow.setText("終了");

        btnYellow.setOnClickListener(v -> finish());

        // ▼ メニューTextView取得
        menu1 = findViewById(R.id.menu1);
        menu2 = findViewById(R.id.menu2);
        menu3 = findViewById(R.id.menu3);
        menu4 = findViewById(R.id.menu4);
        menu5 = findViewById(R.id.menu5);
        menu6 = findViewById(R.id.menu6);
        menu7 = findViewById(R.id.menu7);
        // menu8 は XMLに追加している場合のみ存在
        View v8 = findViewById(R.id.menu8);
        if (v8 instanceof TextView) {
            menu8 = (TextView) v8;
        }

        // ▼ 画面タップで遷移
        menu1.setOnClickListener(v -> openDbTest());
        menu2.setOnClickListener(v -> clearData());
        menu3.setOnClickListener(v -> sendMaintenanceData());
        menu4.setOnClickListener(v -> downloadProgram());
        menu5.setOnClickListener(v -> openCommTest());
        menu6.setOnClickListener(v -> openImagerTest());
        menu7.setOnClickListener(v -> openSystemLib());
        if (menu8 != null) {
            menu8.setOnClickListener(v -> openServerSetting());
        }

        setupBottomButtonTexts();
    }

    /**
     * 物理キー（1〜8/0）で遷移
     * ※端末/キー割当によって拾えるKEYCODEが異なる場合があります。
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                openDbTest();
                return true;

            case KeyEvent.KEYCODE_2:
                clearData();
                return true;

            case KeyEvent.KEYCODE_3:
                sendMaintenanceData();
                return true;

            case KeyEvent.KEYCODE_4:
                downloadProgram();
                return true;

            case KeyEvent.KEYCODE_5:
                openCommTest();
                return true;

            case KeyEvent.KEYCODE_6:
                openImagerTest();
                return true;

            case KeyEvent.KEYCODE_7:
                openSystemLib();
                return true;

            case KeyEvent.KEYCODE_8:
                openServerSetting();
                return true;

            case KeyEvent.KEYCODE_0:
                confirmExit();
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // -------------------------
    // 画面遷移（ここだけ置換）
    // -------------------------

    private void openDbTest() {
        startActivity(new Intent(this, DbTestActivity.class));
    }

    private void openCommTest() {
        requestPasswordIfNeeded(() ->
                startActivity(new Intent(this, CommTestActivity.class)));
    }

    private void openImagerTest() {
        startActivity(new Intent(this, ImagerTestActivity.class));
    }

    private void openSystemLib() {
        startActivity(new Intent(this, SystemLibActivity.class));
    }

    private void openServerSetting() {
        requestPasswordIfNeeded(() ->
                startActivity(new Intent(this, ServerSettingActivity.class)));
    }

    private void clearData() {
        showQuestion("端末内のデータをクリアします。（クリアすると作業中の情報が削除されます）\nよろしいですか？",
                yes -> {
                    if (!yes) {
                        return;
                    }
                    io.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                        db.runInTransaction(() -> {
                            SyukkaContainerDao syukkaContainerDao = db.syukkaContainerDao();
                            SyukkaMeisaiDao syukkaMeisaiDao = db.syukkaMeisaiDao();
                            SyukkaMeisaiWorkDao syukkaMeisaiWorkDao = db.syukkaMeisaiWorkDao();
                            YoteiDao yoteiDao = db.yoteiDao();
                            KakuninContainerDao kakuninContainerDao = db.kakuninContainerDao();
                            KakuninMeisaiDao kakuninMeisaiDao = db.kakuninMeisaiDao();
                            KakuninMeisaiWorkDao kakuninMeisaiWorkDao = db.kakuninMeisaiWorkDao();
                            SystemDao systemDao = db.systemDao();

                            syukkaMeisaiWorkDao.deleteAll();
                            syukkaMeisaiDao.deleteAll();
                            syukkaContainerDao.deleteAll();
                            yoteiDao.deleteAll();
                            kakuninMeisaiWorkDao.deleteAll();
                            kakuninMeisaiDao.deleteAll();
                            kakuninContainerDao.deleteAll();
                            systemDao.updateDataSync(SYSTEM_RENBAN, null, null);
                        });
                        runOnUiThread(() -> showInfoMsg("削除しました", MsgDispMode.MsgBox));
                    });
                });
    }

    private void sendMaintenanceData() {
        showInfoMsg("メンテナンスデータ送信は未実装です。", MsgDispMode.MsgBox);
    }

    private void downloadProgram() {
        showInfoMsg("プログラムダウンロードは未実装です。", MsgDispMode.MsgBox);
    }

    private void confirmExit() {
        showQuestion("アプリケーションを終了します。\nよろしいですか？", yes -> {
            if (yes) {
                finish();
            }
        });
    }

    private void requestPasswordIfNeeded(Runnable onSuccess) {
        showPasswordDialog(success -> {
            if (!success) {
                showErrorMsg("パスワードが違います", MsgDispMode.MsgBox);
                return;
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    private void showPasswordDialog(PasswordCallback callback) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("パスワード")
                .setMessage("パスワードを入力(ヒント：内線番号)")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String pwd = input.getText() != null ? input.getText().toString().trim() : "";
                    boolean ok = SERVICE_PASSWORDS.contains(pwd);
                    if (callback != null) {
                        callback.onResult(ok);
                    }
                })
                .setNegativeButton("キャンセル", (dialog, which) -> {
                    if (callback != null) {
                        callback.onResult(false);
                    }
                })
                .show();
    }

    private void setupBottomButtonTexts() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (yellow != null) yellow.setText("終了");
        refreshBottomButtonsEnabled();
    }

    @Override
    protected void onFunctionYellow() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (io != null) {
            io.shutdown();
        }
    }

    private interface PasswordCallback {
        void onResult(boolean success);
    }
}
