package com.example.myapplication.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.example.myapplication.R;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.time.DateTimeFormatUtil;
import com.example.myapplication.settings.AppSettings;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//============================================================
//　処理概要　:　サーバ接続先設定画面Activity
//　　　　　　:　サーバ接続先(URL)の選択、接続テスト、設定保存を行う。
//　　　　　　:　保存前に接続テスト成功を必須とし、DB(SystemEntity)へ接続先URLを保存する。
//　関　　数　:　onCreate              ..... 画面生成/初期化
//　　　　　　:　setupBottomButtons    ..... 下部ボタン設定
//　　　　　　:　onFunctionYellow      ..... (黄)終了処理
//　　　　　　:　onFunctionBlue        ..... (青)保存処理(テスト必須/DB保存)
//　　　　　　:　onDestroy             ..... リソース解放
//　　　　　　:　bindViews             ..... 画面部品の取得
//　　　　　　:　setupServerSpinner    ..... 接続先スピナー設定
//　　　　　　:　addServerOption       ..... 接続先候補の追加
//　　　　　　:　setupTestButton       ..... 接続テストボタン設定
//　　　　　　:　loadCurrentSelection  ..... 現在設定値の読み込み/選択反映
//　　　　　　:　selectUrl             ..... 指定URLをスピナー選択へ反映
//　　　　　　:　getSelectedUrl        ..... 選択中URLの取得
//　　　　　　:　runConnectionTest     ..... 接続テスト実行(GET)
//　　　　　　:　readResponse          ..... レスポンス読み捨て
//　　　　　　:　equalsIgnoreCase      ..... 大文字小文字無視比較
//　　　　　　:　ServerOption          ..... 接続先候補クラス(内部クラス)
//============================================================

public class ServerSettingActivity extends BaseActivity {

    private static final int SYSTEM_RENBAN = 1;          // システム連番
    private static final int CONNECTION_TIMEOUT_MS = 10000; // 接続タイムアウト(ms)

    private boolean testSuccess = false; // 接続テスト成功フラグ
    private Spinner spServerUrl;         // 接続先サーバー選択
    private Button btnTestConn;          // 接続テストボタン
    private ExecutorService io;          // I/O処理スレッド
    private List<ServerOption> serverOptions; // 接続先候補一覧

    //============================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... 画面再生成時の保存状態
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_setting);

        // 下部ボタン設定（終了/保存）
        setupBottomButtons();

        // 画面部品の取得
        bindViews();

        // 接続先スピナー設定
        setupServerSpinner();

        // 接続テストボタン設定
        setupTestButton();

        // DB/通信処理用スレッド生成
        io = Executors.newSingleThreadExecutor();

        // 現在設定の読み込みと選択反映
        loadCurrentSelection();
    }

    //============================================================
    //　機　能　:　下部ボタンの表示内容を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupBottomButtons() {
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        if (yellow != null) {
            yellow.setText("終了");
        }
        if (blue != null) {
            blue.setText("保存");
        }
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　黄ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //============================================================
    //　機　能　:　青ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionBlue() {
        // 保存前に接続テスト成功が必要
        if (!testSuccess) {
            showErrorMsg("接続テストが行われていません。設定保存前に接続テストを行ってください。", MsgDispMode.MsgBox);
            return;
        }

        // 選択URLを取得
        String selectedUrl = getSelectedUrl();
        if (TextUtils.isEmpty(selectedUrl)) {
            showErrorMsg("接続先が未設定です。", MsgDispMode.MsgBox);
            return;
        }

        // DB保存は別スレッドで実行
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // Systemテーブルから現在値取得（なければ新規）
            SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);
            String currentUrl = (system != null) ? system.webSvcUrl : null;
            if (system == null) {
                system = new SystemEntity();
                system.renban = SYSTEM_RENBAN;
            }

            // 変更がある場合のみ更新
            if (!equalsIgnoreCase(selectedUrl, currentUrl)) {
                String now = DateTimeFormatUtil.nowDbYmdHms();
                system.webSvcUrl = selectedUrl;
                system.updateProcName = "ServerSetting#onFunctionBlue";
                system.updateYmd = now;
                db.systemDao().upsert(system);
            }

            // 保存後に終了
            runOnUiThread(this::finish);
        });
    }

    //============================================================
    //　機　能　:　リソース解放
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onDestroy() {
        if (io != null) {
            io.shutdownNow();
        }
        super.onDestroy();
    }

    //============================================================
    //　機　能　:　画面部品の取得
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void bindViews() {
        spServerUrl = findViewById(R.id.spServerUrl);
        btnTestConn = findViewById(R.id.btnTestConn);
    }

    //============================================================
    //　機　能　:　接続先スピナー設定
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupServerSpinner() {
        serverOptions = new ArrayList<>();

        // 設定クラスのURL定義から候補を作成
        addServerOption(AppSettings.WebSvcURL_Honban, "本番環境");
        addServerOption(AppSettings.WebSvcURL_SCS, "SCS Azureテスト環境");
        addServerOption(AppSettings.WebSvcURL_Test, "SCS 社内テスト環境");

        // 候補が無い場合の保険
        if (serverOptions.isEmpty()) {
            serverOptions.add(new ServerOption("", "未設定"));
        }

        // スピナーへ設定
        ArrayAdapter<ServerOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                serverOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spServerUrl.setAdapter(adapter);

        // 選択変更時はテスト結果をリセット（再テスト必須）
        spServerUrl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                testSuccess = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                testSuccess = false;
            }
        });
    }

    //============================================================
    //　機　能　:　接続先候補の追加
    //　引　数　:　url   ..... 接続先URL
    //　　　　　:　label ..... 表示ラベル
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void addServerOption(String url, String label) {
        if (TextUtils.isEmpty(url)) return;
        serverOptions.add(new ServerOption(url, label));
    }

    //============================================================
    //　機　能　:　接続テストボタン設定
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupTestButton() {
        if (btnTestConn == null) return;
        btnTestConn.setOnClickListener(v -> runConnectionTest());
    }

    //============================================================
    //　機　能　:　現在設定値の読み込み/選択反映
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void loadCurrentSelection() {
        // DB取得は別スレッドで実行
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);
            String currentUrl = (system != null) ? system.webSvcUrl : null;

            // UIへ反映
            runOnUiThread(() -> selectUrl(currentUrl));
        });
    }

    //============================================================
    //　機　能　:　指定URLをスピナー選択へ反映
    //　引　数　:　url ..... 選択させたいURL
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void selectUrl(String url) {
        if (serverOptions == null || serverOptions.isEmpty()) return;

        // URL未設定の場合は先頭（未設定/先頭候補）
        if (TextUtils.isEmpty(url)) {
            spServerUrl.setSelection(0);
            return;
        }

        // 一致する候補があればその位置を選択
        for (int i = 0; i < serverOptions.size(); i++) {
            if (equalsIgnoreCase(url, serverOptions.get(i).url)) {
                spServerUrl.setSelection(i);
                return;
            }
        }

        // 見つからない場合は先頭
        spServerUrl.setSelection(0);
    }

    //============================================================
    //　機　能　:　選択中URLの取得
    //　引　数　:　なし
    //　戻り値　:　[String] ..... URL（取得不可時はnull）
    //============================================================
    private String getSelectedUrl() {
        Object selected = spServerUrl.getSelectedItem();
        if (selected instanceof ServerOption) {
            return ((ServerOption) selected).url;
        }
        return null;
    }

    //============================================================
    //　機　能　:　接続テスト実行(GET)
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void runConnectionTest() {
        String url = getSelectedUrl();
        if (TextUtils.isEmpty(url)) {
            showErrorMsg("接続先が未設定です。", MsgDispMode.MsgBox);
            return;
        }

        // 通信は別スレッドで実行
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 接続を作成
                URL target = new URL(url);
                connection = (HttpURLConnection) target.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.setReadTimeout(CONNECTION_TIMEOUT_MS);

                // 応答コード取得
                int code = connection.getResponseCode();

                // 応答本文は読み捨て（ストリームを消費しておく）
                InputStream stream = (code >= 200 && code < 400)
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                if (stream != null) {
                    readResponse(stream);
                }

                // 結果表示
                if (code >= 200 && code < 400) {
                    runOnUiThread(() -> {
                        showInfoMsg("接続しました", MsgDispMode.MsgBox);
                        testSuccess = true;
                    });
                } else {
                    runOnUiThread(() -> showErrorMsg("接続に失敗しました\n" + url, MsgDispMode.MsgBox));
                }
            } catch (Exception ex) {
                runOnUiThread(() -> errorProcess("btnTestConn_Click", ex));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    //============================================================
    //　機　能　:　レスポンス読み捨て
    //　引　数　:　stream ..... 入力ストリーム
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void readResponse(InputStream stream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                // 読み捨て
            }
        }
    }

    //============================================================
    //　機　能　:　大文字小文字無視比較
    //　引　数　:　left  ..... 比較文字列1
    //　　　　　:　right ..... 比較文字列2
    //　戻り値　:　[boolean] ..... True:一致、False:不一致
    //============================================================
    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.equalsIgnoreCase(right);
    }

    //============================================================
    //　機　能　:　接続先候補クラス(内部クラス)
    //　引　数　:　なし
    //　戻り値　:　[なし]
    //============================================================
    private static class ServerOption {
        private final String url;
        private final String label;

        //============================================================
        //　機　能　:　接続先候補生成
        //　引　数　:　url ..... 接続先URL
        //　　　　　:　label ..... 表示ラベル
        //　戻り値　:　[void] ..... なし
        //============================================================
        private ServerOption(String url, String label) {
            this.url = url;
            this.label = label;
        }

        //============================================================
        //　機　能　:　スピナー表示文字列の返却
        //　引　数　:　なし
        //　戻り値　:　[String] ..... 表示ラベル
        //============================================================
        @Override
        public String toString() {
            return label;
        }
    }
}
