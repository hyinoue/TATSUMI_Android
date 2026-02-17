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


//======================================
//　処理概要　:　ServerSettingActivityクラス
//======================================

/**
 * サーバ設定画面Activity。
 *
 * <p>現状は設定UI表示と終了ボタンのみを扱う。</p>
 * <p>将来的なサーバ接続先/同期設定の入力画面として拡張される前提。</p>
 */
public class ServerSettingActivity extends BaseActivity {

    private static final int SYSTEM_RENBAN = 1;
    private static final int CONNECTION_TIMEOUT_MS = 10000;

    private boolean testSuccess = false;
    private Spinner spServerUrl;
    private Button btnTestConn;
    private ExecutorService io;
    private List<ServerOption> serverOptions;

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_setting);
        setupBottomButtons();
        bindViews();
        setupServerSpinner();
        setupTestButton();
        io = Executors.newSingleThreadExecutor();
        loadCurrentSelection();
    }
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

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

    //==================================
    //　機　能　:　on Function Yellowの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //==================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    @Override
    protected void onFunctionBlue() {
        if (!testSuccess) {
            showErrorMsg("接続テストが行われていません。設定保存前に接続テストを行ってください。", MsgDispMode.MsgBox);
            return;
        }

        String selectedUrl = getSelectedUrl();
        if (TextUtils.isEmpty(selectedUrl)) {
            showErrorMsg("接続先が未設定です。", MsgDispMode.MsgBox);
            return;
        }

        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);
            String currentUrl = (system != null) ? system.webSvcUrl : null;
            if (system == null) {
                system = new SystemEntity();
                system.renban = SYSTEM_RENBAN;
            }

            if (!equalsIgnoreCase(selectedUrl, currentUrl)) {
                String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.JAPAN)
                        .format(new java.util.Date());
                system.webSvcUrl = selectedUrl;
                system.updateProcName = "ServerSetting#onFunctionBlue";
                system.updateYmd = now;
                db.systemDao().upsert(system);
            }

            runOnUiThread(this::finish);
        });
    }

    @Override
    protected void onDestroy() {
        if (io != null) {
            io.shutdownNow();
        }
        super.onDestroy();
    }

    private void bindViews() {
        spServerUrl = findViewById(R.id.spServerUrl);
        btnTestConn = findViewById(R.id.btnTestConn);
    }

    private void setupServerSpinner() {
        serverOptions = new ArrayList<>();
        addServerOption(AppSettings.WebSvcURL_Honban, "本番環境");
        addServerOption(AppSettings.WebSvcURL_SCS, "SCS Azureテスト環境");
        addServerOption(AppSettings.WebSvcURL_Test, "SCS 社内テスト環境");

        if (serverOptions.isEmpty()) {
            serverOptions.add(new ServerOption("", "未設定"));
        }

        ArrayAdapter<ServerOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                serverOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spServerUrl.setAdapter(adapter);
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

    private void addServerOption(String url, String label) {
        if (TextUtils.isEmpty(url)) return;
        serverOptions.add(new ServerOption(url, label));
    }

    private void setupTestButton() {
        if (btnTestConn == null) return;
        btnTestConn.setOnClickListener(v -> runConnectionTest());
    }

    private void loadCurrentSelection() {
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);
            String currentUrl = (system != null) ? system.webSvcUrl : null;

            runOnUiThread(() -> selectUrl(currentUrl));
        });
    }

    private void selectUrl(String url) {
        if (serverOptions == null || serverOptions.isEmpty()) return;
        if (TextUtils.isEmpty(url)) {
            spServerUrl.setSelection(0);
            return;
        }
        for (int i = 0; i < serverOptions.size(); i++) {
            if (equalsIgnoreCase(url, serverOptions.get(i).url)) {
                spServerUrl.setSelection(i);
                return;
            }
        }
        spServerUrl.setSelection(0);
    }

    private String getSelectedUrl() {
        Object selected = spServerUrl.getSelectedItem();
        if (selected instanceof ServerOption) {
            return ((ServerOption) selected).url;
        }
        return null;
    }

    private void runConnectionTest() {
        String url = getSelectedUrl();
        if (TextUtils.isEmpty(url)) {
            showErrorMsg("接続先が未設定です。", MsgDispMode.MsgBox);
            return;
        }

        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL target = new URL(url);
                connection = (HttpURLConnection) target.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.setReadTimeout(CONNECTION_TIMEOUT_MS);

                int code = connection.getResponseCode();
                InputStream stream = (code >= 200 && code < 400)
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                if (stream != null) {
                    readResponse(stream);
                }

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

    private void readResponse(InputStream stream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                // 読み捨て
            }
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.equalsIgnoreCase(right);
    }

    private static class ServerOption {
        private final String url;
        private final String label;

        private ServerOption(String url, String label) {
            this.url = url;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}