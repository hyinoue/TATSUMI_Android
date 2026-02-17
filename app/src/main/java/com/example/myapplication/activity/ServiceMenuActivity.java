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
import com.example.myapplication.connector.SvcHandyWrapper;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.dao.KakuninMeisaiWorkDao;
import com.example.myapplication.db.dao.SystemDao;
import com.example.myapplication.db.dao.SyukkaContainerDao;
import com.example.myapplication.db.dao.SyukkaMeisaiDao;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.dao.YoteiDao;
import com.example.myapplication.log.FileLogger;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//====================================
//　処理概要　:　ServiceMenuActivityクラス
//====================================

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
    private ExecutorService io;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
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


        // ▼ メニューTextView取得
        menu1 = findViewById(R.id.menu1);
        menu2 = findViewById(R.id.menu2);
        menu3 = findViewById(R.id.menu3);
        menu4 = findViewById(R.id.menu4);
        menu5 = findViewById(R.id.menu5);
        menu6 = findViewById(R.id.menu6);
        menu7 = findViewById(R.id.menu7);

        // ▼ 画面タップで遷移
        menu1.setOnClickListener(v -> openDbTest());
        menu2.setOnClickListener(v -> clearData());
        menu3.setOnClickListener(v -> sendMaintenanceData());
        menu4.setOnClickListener(v -> openCommTest());
        menu5.setOnClickListener(v -> openImagerTest());
        menu6.setOnClickListener(v -> openSystemLib());
        menu7.setOnClickListener(v -> openServerSetting());

        setupBottomButtonTexts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearMenuFocus();
    }

    //フォーカス解除
    private void clearMenuFocus() {
        TextView[] menus = {menu1, menu2, menu3, menu4, menu5, menu6, menu7};
        for (TextView menu : menus) {
            if (menu != null) {
                menu.clearFocus();
                menu.setSelected(false);
            }
        }

        View root = findViewById(R.id.root);
        if (root != null) {
            root.setFocusable(true);
            root.setFocusableInTouchMode(true);
            root.requestFocus();
        }
    }

    /**
     * 物理キー（1〜8/0）で遷移
     * ※端末/キー割当によって拾えるKEYCODEが異なる場合があります。
     */
    //=================================
    //　機　能　:　キー押下時の処理
    //　引　数　:　keyCode ..... int
    //　　　　　:　event ..... KeyEvent
    //　戻り値　:　[boolean] ..... なし
    //=================================
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
                openCommTest();
                return true;

            case KeyEvent.KEYCODE_5:
                openImagerTest();
                return true;

            case KeyEvent.KEYCODE_6:
                openSystemLib();
                return true;

            case KeyEvent.KEYCODE_7:
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
    //============================
    //　機　能　:　db Testを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void openDbTest() {
        startActivity(new Intent(this, DbTestActivity.class));
    }

    //============================
    //　機　能　:　comm Testを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void openCommTest() {
        requestPasswordIfNeeded(() ->
                startActivity(new Intent(this, CommTestActivity.class)));
    }

    //============================
    //　機　能　:　imager Testを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void openImagerTest() {
        startActivity(new Intent(this, ImagerTestActivity.class));
    }

    //============================
    //　機　能　:　system Libを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void openSystemLib() {
        startActivity(new Intent(this, SystemLibActivity.class));
    }

    //==============================
    //　機　能　:　server Settingを開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================
    private void openServerSetting() {
        requestPasswordIfNeeded(() ->
                startActivity(new Intent(this, ServerSettingActivity.class)));
    }

    //============================
    //　機　能　:　clear Dataの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
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
                            systemDao.updateDataSync(SYSTEM_RENBAN, null, null, "ServiceMenu#clearData", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.JAPAN).format(new java.util.Date()));
                        });
                        runOnUiThread(() -> showInfoMsg("削除しました", MsgDispMode.MsgBox));
                    });
                });
    }

    //==================================
    //　機　能　:　maintenance Dataを送信する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    private void sendMaintenanceData() {
        io.execute(() -> {
            try (SvcHandyWrapper svc = new SvcHandyWrapper()) {
                File dbFile = getDatabasePath(AppDatabase.DB_NAME);
                if (!dbFile.exists()) {
                    FileLogger.error(this, "frmServiceMenu-LinkLabel_Click", "DBファイルが見つかりません。", null);
                    runOnUiThread(() -> showErrorMsg("DBファイルが見つかりません。", MsgDispMode.MsgBox));
                    return;
                }

                byte[] dbBytes = readFileBytes(dbFile);
                if (!svc.uploadBinaryFile(dbFile.getName(), dbBytes)) {
                    FileLogger.error(this, "frmServiceMenu-LinkLabel_Click", "DBファイルのアップロードに失敗しました。", null);
                    runOnUiThread(() -> showErrorMsg("DBファイルのアップロードに失敗しました。", MsgDispMode.MsgBox));
                    return;
                }

                File logFile = new File(getFilesDir(), "ErrorLog.txt");
                if (logFile.exists()) {
                    byte[] logBytes = readFileBytes(logFile);
                    if (!svc.uploadBinaryFile(logFile.getName(), logBytes)) {
                        FileLogger.error(this, "frmServiceMenu-LinkLabel_Click", "ログファイルのアップロードに失敗しました。", null);
                        runOnUiThread(() -> showErrorMsg("ログファイルのアップロードに失敗しました。", MsgDispMode.MsgBox));
                        return;
                    }
                }

                FileLogger.info(this, "frmServiceMenu-LinkLabel_Click", "ファイルをアップロードしました");
                runOnUiThread(() -> showInfoMsg("ファイルをアップロードしました", MsgDispMode.MsgBox));
            } catch (Exception ex) {
                String msg = resolveNetworkMessage(ex);
                FileLogger.error(this, "frmServiceMenu-LinkLabel_Click", msg, ex);
                runOnUiThread(() -> showErrorMsg(msg, MsgDispMode.MsgBox));
            }
        });
    }

    private String resolveNetworkMessage(Exception ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof javax.net.ssl.SSLHandshakeException
                    || t instanceof javax.net.ssl.SSLException) {
                return "Could not establish secure channel for SSL/TLS";
            }
            if (t instanceof java.net.UnknownHostException) {
                return "The remote name could not be resolved";
            }
            if (t instanceof java.net.SocketTimeoutException) {
                return "The operation has timed-out.";
            }
            t = t.getCause();
        }
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int offset = 0;
            int read;
            while (offset < buffer.length && (read = fis.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += read;
            }
            if (offset != buffer.length) {
                throw new IOException("ファイルの読み込みに失敗しました。");
            }
            return buffer;
        }
    }

    //============================
    //　機　能　:　confirm Exitの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void confirmExit() {
        showQuestion("アプリケーションを終了します。\nよろしいですか？", yes -> {
            if (yes) {
                finish();
            }
        });
    }

    //==========================================
    //　機　能　:　request Password If Neededの処理
    //　引　数　:　onSuccess ..... Runnable
    //　戻り値　:　[void] ..... なし
    //==========================================
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

    //============================================
    //　機　能　:　show Password Dialogの処理
    //　引　数　:　callback ..... PasswordCallback
    //　戻り値　:　[void] ..... なし
    //============================================
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
                    // キャンセル時はエラー表示を出さない
                })
                .show();
    }

    //=====================================
    //　機　能　:　bottom Button Textsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=====================================
    private void setupBottomButtonTexts() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (yellow != null) yellow.setText("終了");
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
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (io != null) {
            io.shutdown();
        }
    }

    private interface PasswordCallback {
        //==================================
        //　機　能　:　on Resultの処理
        //　引　数　:　success ..... boolean
        //　戻り値　:　[void] ..... なし
        //==================================
        void onResult(boolean success);
    }
}
