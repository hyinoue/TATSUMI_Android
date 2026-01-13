package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.SyukkaData;
import com.example.myapplication.model.SyukkaHeader;
import com.example.myapplication.model.SyukkaMeisai;
import com.example.myapplication.repo.SvcHandyRepository;
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
    // コンテナ番号（例：SESU010355 のような実在の番号）
    private static final String REAL_CONTAINER_NO = "SESU010355";
    // シール番号（実運用の桁・形式）
    private static final String REAL_SEAL_NO = "1234567";

    // 重量（運用ルールに合わせて）
    private static final int REAL_CONTAINER_JYURYO = 3800;
    private static final int REAL_DUNNAGE_JYURYO = 200;

    // 送る束の数（まずは1件でDB更新を確認 → 問題なければ全部送る等）
    private static final int SEND_BUNDLE_LIMIT = 1; // まずは1推奨。全部送りたいなら 0（=全件送信）に。

    private ExecutorService io;
    private TextView tvCenterStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // ==============================
// コンテナサイズ（20ft/40ft）をSpinnerに設定し、選択を保存
// ==============================
        Spinner spContainerSize = findViewById(R.id.spContainerSize);

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
        btnYellow.setText("再起動");

        // ※フルスクリーンは BaseActivity が実施（applyImmersive() の直書きは不要）

        io = Executors.newSingleThreadExecutor();
        tvCenterStatus = findViewById(R.id.tvCenterStatus);

        Button btnDataReceive = findViewById(R.id.btnDataReceive);
        btnDataReceive.setOnClickListener(v -> {
            setCenterStatus("送信テスト中...");
            io.execute(this::sendSyukkaDataReal);
        });

        // 積載束選択
        Button btnBundleSelect = findViewById(R.id.btnBundleSelect);
        btnBundleSelect.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, BundleSelectActivity.class);
            startActivity(intent);
        });

        // コンテナ入力
        Button btnContainerInput = findViewById(R.id.btnContainerInput);
        btnContainerInput.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ContainerInputActivity.class);
            startActivity(intent);
        });

        // 重量計算
        Button btnWeightCalc = findViewById(R.id.btnWeightCalc);
        btnWeightCalc.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, CalcJyuryoActivity.class);
            startActivity(intent);
        });

        // 照合コンテナ選定
        Button btnCollateContainerSelect = findViewById(R.id.btnCollateContainerSelect);
        btnCollateContainerSelect.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MenuActivity.this,
                    SyougoContainerActivity.class
            );
            startActivity(intent);
        });
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

            // ★ここが「実運用の形」：実際に運用で使う値を入れる
            b.containerNo = REAL_CONTAINER_NO;
            b.sealNo = REAL_SEAL_NO;
            b.containerJyuryo = REAL_CONTAINER_JYURYO;
            b.dunnageJyuryo = REAL_DUNNAGE_JYURYO;

            int limit = SEND_BUNDLE_LIMIT;
            if (limit <= 0 || limit > meisai.size()) limit = meisai.size();

            for (int i = 0; i < limit; i++) {
                SyukkaMeisai m = meisai.get(i);

                // そのまま追加（キーを変えない＝サーバDBに存在する前提を崩さない）
                SyukkaMeisai send = new SyukkaMeisai();
                send.heatNo = m.heatNo;
                send.sokuban = m.sokuban;
                send.syukkaSashizuNo = m.syukkaSashizuNo;
                send.bundleNo = m.bundleNo;    // ★bundleNo（小文字）重要
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

    private void setCenterStatus(String text) {
        tvCenterStatus.setText(text);
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

        SyukkaHeader h = data.header.get(0); // 先頭1件を見る

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
