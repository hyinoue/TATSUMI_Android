package com.example.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.connector.SvcHandyRepository;
import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.SyukkaData;
import com.example.myapplication.model.SyukkaHeader;
import com.example.myapplication.model.SyukkaMeisai;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuActivity extends BaseActivity {

    private static final String TAG = "MENU";

    // ★★★ 実運用値に置き換えてください ★★★
    private static final String REAL_CONTAINER_NO = "SESU010355";
    private static final String REAL_SEAL_NO = "1234567";
    private static final int REAL_CONTAINER_JYURYO = 3800;
    private static final int REAL_DUNNAGE_JYURYO = 200;

    // 送る束の数（まずは1件でDB更新を確認 → 問題なければ全部送る等）
    private static final int SEND_BUNDLE_LIMIT = 1; // 全件送りたいなら 0

    private ExecutorService io;

    // ===== Views =====
    private TextView tvCenterStatus;
    private Spinner spContainerSize;

    private Button btnDataReceive;
    private Button btnBundleSelect;
    private Button btnContainerInput;
    private Button btnWeightCalc;
    private Button btnCollateContainerSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        io = Executors.newSingleThreadExecutor();

        initViews();
        setupContainerSizeSpinner();

        // 下ボタン文言（クリック設定は BaseActivity が onFunctionXxx へ集約）
        setupBottomButtonTexts();

        // 画面内ボタン類のクリック
        wireActions();
    }

    // ==============================
    // View取得
    // ==============================
    private void initViews() {
        tvCenterStatus = findViewById(R.id.tvCenterStatus);
        spContainerSize = findViewById(R.id.spContainerSize);

        btnDataReceive = findViewById(R.id.btnDataReceive);
        btnBundleSelect = findViewById(R.id.btnBundleSelect);
        btnContainerInput = findViewById(R.id.btnContainerInput);
        btnWeightCalc = findViewById(R.id.btnWeightCalc);
        btnCollateContainerSelect = findViewById(R.id.btnCollateContainerSelect);
    }

    // ==============================
    // コンテナサイズSpinner（20ft/40ft）保存・復元
    // ==============================
    private void setupContainerSizeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.container_sizes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spContainerSize.setAdapter(adapter);

        final SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // 保存済みを復元（デフォルト 20ft）
        String savedSize = prefs.getString("container_size", "20ft");
        int pos = adapter.getPosition(savedSize);
        if (pos >= 0) spContainerSize.setSelection(pos);

        // 変更されたら保存
        spContainerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                prefs.edit().putString("container_size", selected).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // ==============================
    // 下ボタン文言（画面ごと）
    // ==============================
    private void setupBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        if (yellow != null) yellow.setText("再起動");

        // 空文字は無効＋薄く（BaseActivity機能）
        refreshBottomButtonsEnabled();
    }

    // ==============================
    // クリック処理（画面内ボタン）
    // ==============================
    private void wireActions() {

        // 送信テスト
        btnDataReceive.setOnClickListener(v -> {
            setCenterStatus("送信テスト中...");
            io.execute(this::sendSyukkaDataReal);
        });

        // 画面遷移（タップ）
        btnBundleSelect.setOnClickListener(v -> goBundleSelect());
        btnContainerInput.setOnClickListener(v -> goContainerInput());
        btnWeightCalc.setOnClickListener(v -> goBundleSelect());
        btnCollateContainerSelect.setOnClickListener(v -> goCollateContainerSelect());
    }

    // ==============================
    // 下ボタン（黄色＝再起動）タップ/物理F4 の実処理
    // ==============================
    @Override
    protected void onFunctionYellow() {
        onRestartMenu();
    }

    private void onRestartMenu() {
        recreate();
    }

    // ==============================
    // 画面遷移（共通メソッド化）
    // ==============================
    private void goServiceMenu() {
        startActivity(new Intent(this, ServiceMenuActivity.class));
    }

    private void goBundleSelect() {
        startActivity(new Intent(this, BundleSelectActivity.class));
    }

    private void goContainerInput() {
        startActivity(new Intent(this, ContainerInputActivity.class));
    }

    private void goCollateContainerSelect() {
        startActivity(new Intent(this, CollateContainerSelectActivity.class));
    }

    // ==============================
    // 数字キー対応（この画面固有なので残す）
    // 0 → サービスメニュー
    // 2 → 積載束選定
    // 3 → コンテナ情報入力
    // 4 → 重量計算
    // 5 → 照合コンテナ選定
    // ==============================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // ★長押し連打防止（数字キー側も抑止したいなら残す）
        if (event.getRepeatCount() > 0) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                goServiceMenu();
                return true;

            case KeyEvent.KEYCODE_2:
                goBundleSelect();
                return true;

            case KeyEvent.KEYCODE_3:
                goContainerInput();
                return true;

            case KeyEvent.KEYCODE_4:
                goBundleSelect();
                return true;

            case KeyEvent.KEYCODE_5:
                goCollateContainerSelect();
                return true;
        }

        // ★ここがポイント：BaseActivity の dispatchKeyEvent が F1-F4 を拾うので、
        // 数字キー以外は super に流してOK
        return super.onKeyDown(keyCode, event);
    }

    // ===========================================================
    // 機　能　:　AssemblyInfoのﾀｲﾄﾙの取得
    //'引　数　:　strCd      ..... ｺｰﾄﾞ
    //　　　　　:　lngRow     ..... 行№(参照渡し)
    //　　　　: (blnSetFlg) ..... True –ｾｯﾄする(ﾃﾞﾌｫﾙﾄ)、False –ｾｯﾄしない
    //　戻り値　:　[String]  ..... ﾀｲﾄﾙ
    // ===========================================================
    private void setCenterStatus(String text) {
        if (tvCenterStatus != null) tvCenterStatus.setText(text);
    }

    /**
     * 実運用形：サーバから取得した実在の束明細を使って SendSyukkaData を実行
     */
    private void sendSyukkaDataReal() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);

        try {
            SvcHandyRepository repo = new SvcHandyRepository();

            // 1) 作業日
            Date sagyouYmd = repo.getSagyouYmd();

            // 2) 実在の出荷データ（束明細）を取得（ログ用にBEFORE）
            SyukkaData before = repo.getSyukkaData(sagyouYmd);
            logSyukkaHeaderList("BEFORE", before);
            List<SyukkaMeisai> meisai = (before == null) ? null : before.meisai;

            if (meisai == null || meisai.isEmpty()) {
                runOnUiThread(() -> setCenterStatus("NG GetSyukkaData: 明細0件"));
                return;
            }

            // 3) 送信用データ作成（束明細は“取得した実在データ”を流用）
            BunningData b = new BunningData();
            b.syukkaYmd = sagyouYmd;

            b.containerNo = REAL_CONTAINER_NO;
            b.sealNo = REAL_SEAL_NO;
            b.containerJyuryo = REAL_CONTAINER_JYURYO;
            b.dunnageJyuryo = REAL_DUNNAGE_JYURYO;

            int limit = SEND_BUNDLE_LIMIT;
            if (limit <= 0 || limit > meisai.size()) limit = meisai.size();

            for (int i = 0; i < limit; i++) {
                SyukkaMeisai m = meisai.get(i);

                SyukkaMeisai send = new SyukkaMeisai();
                send.heatNo = m.heatNo;
                send.sokuban = m.sokuban;
                send.syukkaSashizuNo = m.syukkaSashizuNo;
                send.bundleNo = m.bundleNo;  // ★bundleNo（小文字）重要
                send.jyuryo = m.jyuryo;
                send.bookingNo = m.bookingNo;

                b.bundles.add(send);
            }

            // 4) 送信
            boolean ok = repo.sendSyukkaData(b);

            if (ok) {
                SyukkaData after = repo.getSyukkaData(sagyouYmd);
                logSyukkaHeaderList("AFTER", after);

                String msg = "OK SendSyukkaDataResult=true"
                        + "\n作業日=" + fmt.format(sagyouYmd)
                        + "\ncontainerNo=" + b.containerNo
                        + "\nsealNo=" + b.sealNo
                        + "\n送信束数=" + b.bundles.size()
                        + "\n先頭束 heatNo=" + b.bundles.get(0).heatNo
                        + " sokuban=" + b.bundles.get(0).sokuban
                        + " bookingNo=" + b.bundles.get(0).bookingNo;

                Log.i(TAG, msg);
                runOnUiThread(() -> setCenterStatus(msg));
            } else {
                runOnUiThread(() -> setCenterStatus("NG SendSyukkaDataResult=false（サーバ側条件NGの可能性）"));
            }

        } catch (Exception ex) {
            Log.e(TAG, "SendSyukkaData NG", ex);
            String msg = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();
            runOnUiThread(() -> setCenterStatus("NG " + msg));
        }
    }

    private void logSyukkaHeaderList(String label, SyukkaData data) {
        if (data == null) {
            Log.i(TAG, label + " HEADER: data=null");
            return;
        }
        if (data.header == null || data.header.isEmpty()) {
            Log.i(TAG, label + " HEADER: header list is empty");
            return;
        }

        SyukkaHeader h = data.header.get(0);

        Log.i(TAG, label + " HEADER:"
                + " bookingNo=" + h.bookingNo
                + " syukkaYmd=" + (h.syukkaYmd != null ? h.syukkaYmd.toString() : "null")
                + " containerCount=" + h.containerCount
                + " totalBundole=" + h.totalBundole
                + " totalJyuryo=" + h.totalJyuryo
                + " kanryoContainerCnt=" + h.kanryoContainerCnt
                + " kanryoBundleSum=" + h.kanryoBundleSum
                + " knaryoJyuryoSum=" + h.knaryoJyuryoSum
                + " lastUpdYmdHms=" + (h.lastUpdYmdHms != null ? h.lastUpdYmdHms.toString() : "null")
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (io != null) io.shutdownNow();
    }
}
