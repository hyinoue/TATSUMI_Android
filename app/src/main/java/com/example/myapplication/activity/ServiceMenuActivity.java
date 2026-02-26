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
import java.util.concurrent.atomic.AtomicBoolean;

//============================================================
//　処理概要　:　サービスメニュー画面Activity
//　　　　　　:　保守/診断向け機能への入口をまとめた画面。
//　　　　　　:　DB確認、データクリア、保守データ送信、通信/バーコードテスト、設定画面遷移を行う。
//　　　　　　:　アクセス制限が必要な機能はパスワード確認を行う。
//　関　　数　:　onCreate                 ..... 画面生成/初期化
//　　　　　　:　onResume                 ..... 画面復帰時(メニューフォーカス解除)
//　　　　　　:　clearMenuFocus           ..... メニューのフォーカス解除
//　　　　　　:　onKeyDown                ..... 物理キー入力によるメニュー遷移
//　　　　　　:　openDbTest               ..... DB確認画面へ遷移
//　　　　　　:　openCommTest             ..... 通信テスト画面へ遷移(要PW)
//　　　　　　:　openImagerTest           ..... バーコード(イメージャ)テスト画面へ遷移
//　　　　　　:　openSystemLib            ..... システムライブラリ設定画面へ遷移
//　　　　　　:　openServerSetting        ..... サーバ設定画面へ遷移(要PW)
//　　　　　　:　clearData                ..... 端末内データの削除
//　　　　　　:　sendMaintenanceData      ..... DB/ログの保守送信(アップロード)
//　　　　　　:　resolveNetworkMessage    ..... 例外から通信エラーメッセージ生成
//　　　　　　:　readFileBytes            ..... ファイル読み込み(byte配列)
//　　　　　　:　confirmExit              ..... 終了確認
//　　　　　　:　requestPasswordIfNeeded  ..... PW確認後に処理実行
//　　　　　　:　showPasswordDialog       ..... PW入力ダイアログ表示
//　　　　　　:　setupBottomButtonTexts   ..... 下部ボタン表示設定
//　　　　　　:　onFunctionYellow         ..... (黄)終了処理
//　　　　　　:　onDestroy                ..... リソース解放
//　　　　　　:　PasswordCallback         ..... PWダイアログ結果通知IF
//============================================================

public class ServiceMenuActivity extends BaseActivity {

    private static final int SYSTEM_RENBAN = 1; // システム連番
    private static final Set<String> SERVICE_PASSWORDS =
            new HashSet<>(Arrays.asList("2441", "4546", "4549", "4522", "4523")); // サービスメニューPW候補

    // 画面メニュー（TextView）
    private TextView menu1; // データ確認
    private TextView menu2; // データクリア
    private TextView menu3; // メンテナンスデータ送信
    private TextView menu4; // 通信テスト
    private TextView menu5; // バーコードテスト
    private TextView menu6; // 振動設定
    private TextView menu7; // サーバー切替

    private ExecutorService io; // I/O処理スレッド
    private final AtomicBoolean isMaintenanceSendRunning = new AtomicBoolean(false); // メンテ送信中フラグ

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_menu);

        // DB/IO用の単一スレッドを生成
        io = Executors.newSingleThreadExecutor();

        // 下ボタン（include）を取得
        View bottom = findViewById(R.id.includeBottomButtons);

        // 各ボタンを取得
        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        // 文字設定（画面ごとにここだけ変える）
        btnBlue.setText("");
        btnRed.setText("");
        btnGreen.setText("");
        btnYellow.setText("終了");

        // メニューTextView取得
        menu1 = findViewById(R.id.menu1);
        menu2 = findViewById(R.id.menu2);
        menu3 = findViewById(R.id.menu3);
        menu4 = findViewById(R.id.menu4);
        menu5 = findViewById(R.id.menu5);
        menu6 = findViewById(R.id.menu6);
        menu7 = findViewById(R.id.menu7);

        // メニュータップで遷移
        menu1.setOnClickListener(v -> openDbTest());
        menu2.setOnClickListener(v -> clearData());
        menu3.setOnClickListener(v -> sendMaintenanceData());
        menu4.setOnClickListener(v -> openCommTest());
        menu5.setOnClickListener(v -> openImagerTest());
        menu6.setOnClickListener(v -> openSystemLib());
        menu7.setOnClickListener(v -> openServerSetting());

        // 下部ボタン表示設定（黄：終了）
        setupBottomButtonTexts();
    }

    //================================================================
    //　機　能　:　画面復帰時の処理(メニューフォーカス解除)
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================================================
    @Override
    protected void onResume() {
        super.onResume();
        clearMenuFocus();
    }

    //================================================================
    //　機　能　:　メニューのフォーカス解除
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void clearMenuFocus() {
        // 全メニューのフォーカス/選択状態を解除
        TextView[] menus = {menu1, menu2, menu3, menu4, menu5, menu6, menu7};
        for (TextView menu : menus) {
            if (menu != null) {
                menu.clearFocus();
                menu.setSelected(false);
            }
        }

        // ルートにフォーカスを当て直す（物理キー入力の取りこぼし防止）
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
        // 長押しリピートは無視（連続実行防止）
        if (event.getRepeatCount() > 0) {
            return true;
        }

        // 数字キーでメニュー遷移
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

    //=============================
    //　機　能　:　「1.データ確認」を開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================
    private void openDbTest() {
        startActivity(new Intent(this, DbTestActivity.class));
    }

    //==============================
    //　機　能　:　「４.通信テスト」を開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================
    private void openCommTest() {
        // 通信テストは要パスワード
        requestPasswordIfNeeded(() ->
                startActivity(new Intent(this, CommTestActivity.class)));
    }

    //===================================
    //　機　能　:　「５.バーコードテスト」を開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================
    private void openImagerTest() {
        startActivity(new Intent(this, ImagerTestActivity.class));
    }

    //============================
    //　機　能　:　「６.振動設定」を開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void openSystemLib() {
        startActivity(new Intent(this, SystemLibActivity.class));
    }

    //================================
    //　機　能　:　「７.サーバー切替」を開く
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    private void openServerSetting() {
        // サーバ設定は要パスワード
        requestPasswordIfNeeded(() ->
                startActivity(new Intent(this, ServerSettingActivity.class)));
    }

    //================================
    //　機　能　:　「２.データクリア」の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    private void clearData() {
        showQuestion("端末内のデータをクリアします。（クリアすると作業中の情報が削除されます）\nよろしいですか？",
                yes -> {
                    if (!yes) {
                        return;
                    }

                    // DB削除は別スレッドで実行
                    io.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                        db.runInTransaction(() -> {
                            // DAO取得
                            SyukkaContainerDao syukkaContainerDao = db.syukkaContainerDao();
                            SyukkaMeisaiDao syukkaMeisaiDao = db.syukkaMeisaiDao();
                            SyukkaMeisaiWorkDao syukkaMeisaiWorkDao = db.syukkaMeisaiWorkDao();
                            YoteiDao yoteiDao = db.yoteiDao();
                            KakuninContainerDao kakuninContainerDao = db.kakuninContainerDao();
                            KakuninMeisaiDao kakuninMeisaiDao = db.kakuninMeisaiDao();
                            KakuninMeisaiWorkDao kakuninMeisaiWorkDao = db.kakuninMeisaiWorkDao();
                            SystemDao systemDao = db.systemDao();

                            // 作業系データを削除
                            syukkaMeisaiWorkDao.deleteAll();
                            syukkaMeisaiDao.deleteAll();
                            syukkaContainerDao.deleteAll();
                            yoteiDao.deleteAll();
                            kakuninMeisaiWorkDao.deleteAll();
                            kakuninMeisaiDao.deleteAll();
                            kakuninContainerDao.deleteAll();

                            // 同期情報を更新（履歴/監査用）
                            systemDao.updateDataSync(
                                    SYSTEM_RENBAN,
                                    null,
                                    null,
                                    "ServiceMenu#clearData",
                                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.JAPAN)
                                            .format(new java.util.Date())
                            );
                        });

                        // 完了表示はUIスレッドで
                        runOnUiThread(() -> showInfoMsg("削除しました", MsgDispMode.MsgBox));
                    });
                });
    }

    //========================================
    //　機　能　:　「３.メンテナンスデータ送信」の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================================
    private void sendMaintenanceData() {
        // 二重実行防止
        if (!isMaintenanceSendRunning.compareAndSet(false, true)) {
            return;
        }

        // 送信処理は別スレッドで実行
        io.execute(() -> {
            try (SvcHandyWrapper svc = new SvcHandyWrapper()) {

                // DBファイルを取得
                File dbFile = getDatabasePath(AppDatabase.DB_NAME);
                if (!dbFile.exists()) {
                    FileLogger.error(this, "ServiceMenuActivity-LinkLabel_Click", "DBファイルが見つかりません。", null);
                    runOnUiThread(() -> showErrorMsg("DBファイルが見つかりません。", MsgDispMode.MsgBox));
                    return;
                }

                // DBファイルを読み込み＆アップロード
                byte[] dbBytes = readFileBytes(dbFile);
                if (!svc.uploadBinaryFile(dbFile.getName(), dbBytes)) {
                    FileLogger.error(this, "ServiceMenuActivity-LinkLabel_Click", "DBファイルのアップロードに失敗しました。", null);
                    runOnUiThread(() -> showErrorMsg("DBファイルのアップロードに失敗しました。", MsgDispMode.MsgBox));
                    return;
                }

                // ログファイルが存在する場合のみアップロード
                File logFile = new File(getFilesDir(), "ErrorLog.txt");
                if (logFile.exists()) {
                    byte[] logBytes = readFileBytes(logFile);
                    if (!svc.uploadBinaryFile(logFile.getName(), logBytes)) {
                        FileLogger.error(this, "ServiceMenuActivity-LinkLabel_Click", "ログファイルのアップロードに失敗しました。", null);
                        runOnUiThread(() -> showErrorMsg("ログファイルのアップロードに失敗しました。", MsgDispMode.MsgBox));
                        return;
                    }
                }

                // 送信完了
                FileLogger.info(this, "ServiceMenuActivity-LinkLabel_Click", "ファイルをアップロードしました");
                runOnUiThread(() -> showInfoMsg("ファイルをアップロードしました", MsgDispMode.MsgBox));

            } catch (Exception ex) {
                // 例外内容からユーザ向けメッセージを整形
                String msg = resolveNetworkMessage(ex);
                FileLogger.error(this, "ServiceMenuActivity-LinkLabel_Click", msg, ex);
                runOnUiThread(() -> showErrorMsg(msg, MsgDispMode.MsgBox));
            } finally {
                // フラグを必ず戻す
                isMaintenanceSendRunning.set(false);
            }
        });
    }

    //===========================================
    //　機　能　:　例外から通信エラーメッセージ生成
    //　引　数　:　ex ..... 例外
    //　戻り値　:　[String] ..... メッセージ
    //===========================================
    private String resolveNetworkMessage(Exception ex) {
        // 原因例外を辿って代表的な通信例外を判定
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
        // 既知に当たらない場合は例外メッセージを返す
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    //===============================================
    //　機　能　:　ファイル読み込み(byte配列)
    //　引　数　:　file ..... 対象ファイル
    //　戻り値　:　[byte[]] ..... 読み込んだバイト配列
    //===============================================
    private byte[] readFileBytes(File file) throws IOException {
        // サイズ分を一括で読み込む（途中で読めない場合は例外）
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int offset = 0;
            int read;
            while (offset < buffer.length
                    && (read = fis.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += read;
            }
            if (offset != buffer.length) {
                throw new IOException("ファイルの読み込みに失敗しました。");
            }
            return buffer;
        }
    }

    //============================
    //　機　能　:　終了確認を行う
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
    //　機　能　:　必要時のみパスワード入力を要求する
    //　引　数　:　onSuccess ..... Runnable
    //　戻り値　:　[void] ..... なし
    //==========================================
    private void requestPasswordIfNeeded(Runnable onSuccess) {
        // パスワード入力ダイアログを表示し、成功時のみ処理を実行
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
    //　機　能　:　パスワード入力ダイアログを表示する
    //　引　数　:　callback ..... PasswordCallback
    //　戻り値　:　[void] ..... なし
    //============================================
    private void showPasswordDialog(PasswordCallback callback) {
        // パスワード入力欄を作成
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("パスワード")
                .setMessage("パスワードを入力(ヒント：内線番号)")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    // 入力値を取得して判定
                    String pwd = input.getText() != null ? input.getText().toString().trim() : "";
                    boolean ok = SERVICE_PASSWORDS.contains(pwd);
                    if (callback != null) {
                        callback.onResult(ok);
                    }
                })
                .setNegativeButton("キャンセル", (dialog, which) -> {
                    // キャンセル時は何もしない（エラー表示なし）
                })
                .show();
    }

    //=====================================
    //　機　能　:　下部ボタンの表示文言を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=====================================
    private void setupBottomButtonTexts() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        // 黄のみ使用（終了）
        if (yellow != null) yellow.setText("終了");
        refreshBottomButtonsEnabled();
    }

    //==================================
    //　機　能　:　黄ボタン押下時の処理を行う
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

        // Executor解放
        if (io != null) {
            io.shutdown();
        }
    }

    //============================================
    //　機　能　:　PWダイアログ結果通知IF
    //　引　数　:　なし
    //　戻り値　:　[なし]
    //============================================
    private interface PasswordCallback {
        //==================================
        //　機　能　:　結果受信時の処理を行う
        //　引　数　:　success ..... boolean
        //　戻り値　:　[void] ..... なし
        //==================================
        void onResult(boolean success);
    }
}
