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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.connector.SvcHandyRepository;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.YoteiEntity;
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

    //============================================================
    //　処理概要　:　メインメニュー画面
    //　備　　考　:　積載束選定（Normal確定時）は BundleSelectActivity 側で
    //　　　　　　:　ContainerInputActivity に直接遷移する（メニュー経由しない）
    //============================================================

    private static final String TAG = "MENU";

    // ★★★ 実運用値に置き換えてください ★★★
    private static final String REAL_CONTAINER_NO = "SESU010355";
    private static final String REAL_SEAL_NO = "1234567";
    private static final int REAL_CONTAINER_JYURYO = 3800;
    private static final int REAL_DUNNAGE_JYURYO = 200;

    // 送る束の数（まずは1件でDB更新を確認 → 問題なければ全部送る等）
    private static final int SEND_BUNDLE_LIMIT = 1; // 全件送りたいなら 0

    private ExecutorService io;
    private ActivityResultLauncher<Intent> bundleSelectLauncher;

    // ===== Views =====
    private TextView tvCenterStatus;
    private Spinner spContainerSize;

    private Button btnDataReceive;
    private Button btnBundleSelect;
    private Button btnContainerInput;
    private Button btnWeightCalc;
    private Button btnCollateContainerSelect;

    private TextView lblDataReceiveTime;
    private TextView lblDataReceive;
    private TextView lblContainerInput;
    private TextView lblContainerPlan;
    private TextView lblContainerFin;
    private TextView lblBundlePlan;
    private TextView lblBundleFin;
    private TextView lblWeightPlan;
    private TextView lblWeightFin;
    private TextView lblZanContainer;
    private TextView lblZanBundle;
    private TextView lblZanWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // ★ DB生成確認用（一時コード）
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());

        // ★ DB生成確認用：バックグラウンドで1回だけ呼ぶ
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.yoteiDao().findAll();
            } catch (Exception e) {
                e.printStackTrace(); // Logcatに出やすくする
            }
        });

        io = Executors.newSingleThreadExecutor();

        setupActivityLaunchers();

        initViews();
        setupContainerSizeSpinner();

        // 下ボタン文言（クリック設定は BaseActivity が onFunctionXxx へ集約）
        setupBottomButtonTexts();

        // 画面内ボタン類のクリック
        wireActions();

        if (btnDataReceive != null) {
            btnDataReceive.requestFocus();
        }

        refreshInformation();
    }

    //============================================================
    //　機　能　:　ActivityResultLauncher の初期化
    //　説　明　:　BundleSelectActivity から戻ったときに表示更新だけ行う
    //　　　　　:　※Normal確定時は BundleSelectActivity 側で ContainerInput に
    //　　　　　:　　直接遷移するため、ここで次画面遷移はしない（チラ見え防止）
    //============================================================
    private void setupActivityLaunchers() {
        bundleSelectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 遷移はしない。表示更新のみ。
                    refreshInformation();
                }
        );
    }

    //============================================================
    //　機　能　:　View取得
    //============================================================
    private void initViews() {
        spContainerSize = findViewById(R.id.spContainerSize);

        btnDataReceive = findViewById(R.id.btnDataReceive);
        btnBundleSelect = findViewById(R.id.btnBundleSelect);
        btnContainerInput = findViewById(R.id.btnContainerInput);
        btnWeightCalc = findViewById(R.id.btnWeightCalc);
        btnCollateContainerSelect = findViewById(R.id.btnCollateContainerSelect);

        lblDataReceiveTime = findViewById(R.id.lblDataReceiveTime);
        lblDataReceive = findViewById(R.id.lblDataReceive);
        lblContainerInput = findViewById(R.id.lblContainerInput);
        lblContainerPlan = findViewById(R.id.lblContainerPlan);
        lblContainerFin = findViewById(R.id.lblContainerFin);
        lblBundlePlan = findViewById(R.id.lblBundlePlan);
        lblBundleFin = findViewById(R.id.lblBundleFin);
        lblWeightPlan = findViewById(R.id.lblWeightPlan);
        lblWeightFin = findViewById(R.id.lblWeightFin);
        lblZanContainer = findViewById(R.id.lblZanContainer);
        lblZanBundle = findViewById(R.id.lblZanBundle);
        lblZanWeight = findViewById(R.id.lblZanWeight);
    }

    //============================================================
    //　機　能　:　コンテナサイズSpinner（20ft/40ft）保存・復元
    //============================================================
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

    //============================================================
    //　機　能　:　下ボタン文言（画面ごと）
    //============================================================
    private void setupBottomButtonTexts() {
        MaterialButton blue = findViewById(R.id.btnBottomBlue);
        MaterialButton red = findViewById(R.id.btnBottomRed);
        MaterialButton green = findViewById(R.id.btnBottomGreen);
        MaterialButton yellow = findViewById(R.id.btnBottomYellow);

        if (blue != null) blue.setText("");
        if (red != null) red.setText("");
        if (green != null) green.setText("");
        if (yellow != null) {
            yellow.setText(BuildConfig.DEBUG ? "終了" : "再起動");
        }

        // 空文字は無効＋薄く（BaseActivity機能）
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　クリック処理（画面内ボタン）
    //============================================================
    private void wireActions() {

        // データ送受信
        btnDataReceive.setOnClickListener(v -> startDataSync());

        // 画面遷移（タップ）
        btnBundleSelect.setOnClickListener(v -> openBundleSelect(BundleSelectActivity.MODE_NORMAL));
        btnContainerInput.setOnClickListener(v -> openContainerInputIfWorkExists());
        btnWeightCalc.setOnClickListener(v -> openBundleSelect(BundleSelectActivity.MODE_JYURYO));
        btnCollateContainerSelect.setOnClickListener(v -> openCollateContainerSelect());
    }

    //============================================================
    //　機　能　:　下ボタン（黄色＝再起動）タップ/物理F4 の実処理
    //============================================================
    @Override
    protected void onFunctionYellow() {
        onRestartMenu();
    }

    //============================================================
    //　機　能　:　再起動確認～実行
    //============================================================
    private void onRestartMenu() {
        if (BuildConfig.DEBUG) {
            finish();
            return;
        }
        showQuestion("端末の再起動を実施します。\nよろしいですか？", yes -> {
            if (!yes) return;
            restartApp();
        });
    }

    //============================================================
    //　機　能　:　アプリ再起動
    //============================================================
    private void restartApp() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent == null) {
            recreate();
            return;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launchIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshInformation();
    }

    //============================================================
    //　機　能　:　サービスメニューへ遷移
    //============================================================
    private void goServiceMenu() {
        startActivity(new Intent(this, ServiceMenuActivity.class));
    }

    //============================================================
    //　機　能　:　積載束選定/重量計算 画面へ遷移
    //　説　明　:　MODE_NORMAL 確定時は BundleSelectActivity 側で
    //　　　　　:　ContainerInputActivity に直接遷移する（メニュー経由しない）
    //============================================================
    private void openBundleSelect(String mode) {
        Intent intent = new Intent(this, BundleSelectActivity.class);
        intent.putExtra(BundleSelectActivity.EXTRA_MODE, mode);

        if (bundleSelectLauncher != null) {
            bundleSelectLauncher.launch(intent);
        } else {
            startActivity(intent);
        }
    }

    //============================================================
    //　機　能　:　コンテナ情報入力へ遷移（Work存在チェック付き）
    //============================================================
    private void openContainerInputIfWorkExists() {
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            boolean hasWork = !db.syukkaMeisaiWorkDao().findAll().isEmpty();
            runOnUiThread(() -> {
                if (!hasWork) {
                    showErrorMsg("積載束選択が行われていません。先に積載束選択を実施してください。", MsgDispMode.Label);
                    return;
                }
                startActivity(new Intent(this, ContainerInputActivity.class));
            });
        });
    }

    //============================================================
    //　機　能　:　照合コンテナ選定へ遷移
    //============================================================
    private void openCollateContainerSelect() {
        startActivity(new Intent(this, CollateContainerSelectActivity.class));
    }

    //============================================================
    //　機　能　:　数字キー対応（この画面固有）
    //　説　明　:　0=サービスメニュー / 1=送受信 / 2=束選定 / 3=コンテナ入力 / 4=重量計算 / 5=照合
    //============================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // ★長押し連打防止
        if (event.getRepeatCount() > 0) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                goServiceMenu();
                return true;

            case KeyEvent.KEYCODE_1:
                startDataSync();
                return true;

            case KeyEvent.KEYCODE_2:
                openBundleSelect(BundleSelectActivity.MODE_NORMAL);
                return true;

            case KeyEvent.KEYCODE_3:
                openContainerInputIfWorkExists();
                return true;

            case KeyEvent.KEYCODE_4:
                openBundleSelect(BundleSelectActivity.MODE_JYURYO);
                return true;

            case KeyEvent.KEYCODE_5:
                openCollateContainerSelect();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //============================================================
    //　機　能　:　中央ステータス表示
    //============================================================
    private void setCenterStatus(String text) {
        if (tvCenterStatus != null) tvCenterStatus.setText(text);
    }

    //============================================================
    //　機　能　:　データ送受信開始
    //============================================================
    private void startDataSync() {
        setCenterStatus("データ送受信中...");
        io.execute(this::runDataSync);
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
                send.bundleNo = m.bundleNo;
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

    //============================================================
    //　機　能　:　データ送受信（実処理）
    //============================================================
    private void runDataSync() {
        showLoadingLong();
        try {
            DataSync sync = new DataSync(getApplicationContext());
            sync.runSync();
            runOnUiThread(() -> {
                setCenterStatus("データ送受信完了");
                showInfoMsg("データ送受信完了", MsgDispMode.Label);
            });
            refreshInformation();
        } catch (Exception ex) {
            Log.e(TAG, "DataSync failed", ex);
            String msg = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();
            runOnUiThread(() -> setCenterStatus("NG " + msg));
        } finally {
            hideLoadingLong();
        }
    }

    //============================================================
    //　機　能　:　メニュー表示情報の再取得（DB集計）
    //============================================================
    private void refreshInformation() {
        io.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SystemEntity system = db.systemDao().findById(1);
            String dataRecv = (system != null) ? system.dataRecvYmdhms : null;

            boolean hasUnsentSyukka = !db.syukkaContainerDao().findUnsent().isEmpty();
            boolean hasUnsentKakunin = false;
            if (!hasUnsentSyukka) {
                hasUnsentKakunin = !db.kakuninContainerDao().findUnsentCompleted().isEmpty();
            }

            boolean hasWork = !db.syukkaMeisaiWorkDao().findAll().isEmpty();
            List<YoteiEntity> yoteiRows = db.yoteiDao().findAll();

            long kanryoContainer = 0;
            long kanryoBundole = 0;
            long kanryoJyuryo = 0;
            long containerCount = 0;
            long goukeiBundole = 0;
            long goukeiJyuryo = 0;

            for (YoteiEntity row : yoteiRows) {
                kanryoContainer += intOrZero(row.kanryoContainer);
                kanryoBundole += intOrZero(row.kanryoBundole);
                kanryoJyuryo += intOrZero(row.kanryoJyuryo);
                containerCount += intOrZero(row.containerCount);
                goukeiBundole += intOrZero(row.goukeiBundole);
                goukeiJyuryo += intOrZero(row.goukeiJyuryo);
            }

            long kanryoJyuryoTon = Math.round(kanryoJyuryo / 1000.0);
            long goukeiJyuryoTon = Math.round(goukeiJyuryo / 1000.0);

            long zanContainer = containerCount - kanryoContainer;
            long zanBundle = goukeiBundole - kanryoBundole;
            long zanWeight = goukeiJyuryoTon - kanryoJyuryoTon;

            final String recvText = (dataRecv == null || dataRecv.trim().isEmpty())
                    ? "----/--/-- --:--"
                    : dataRecv;

            final boolean showUnsent = hasUnsentSyukka || hasUnsentKakunin;
            final boolean hasWorkFinal = hasWork;

            final long kanryoContainerFinal = kanryoContainer;
            final long kanryoBundoleFinal = kanryoBundole;
            final long kanryoJyuryoTonFinal = kanryoJyuryoTon;

            final long containerCountFinal = containerCount;
            final long goukeiBundoleFinal = goukeiBundole;
            final long goukeiJyuryoTonFinal = goukeiJyuryoTon;

            final long zanContainerFinal = zanContainer;
            final long zanBundleFinal = zanBundle;
            final long zanWeightFinal = zanWeight;

            runOnUiThread(() -> {
                if (lblDataReceiveTime != null) {
                    lblDataReceiveTime.setText("最終受信　" + recvText);
                }
                if (lblDataReceive != null) {
                    lblDataReceive.setVisibility(showUnsent ? View.VISIBLE : View.INVISIBLE);
                }
                if (lblContainerInput != null) {
                    lblContainerInput.setVisibility(hasWorkFinal ? View.VISIBLE : View.INVISIBLE);
                }

                if (lblContainerFin != null)
                    lblContainerFin.setText(formatNumber(kanryoContainerFinal));
                if (lblBundleFin != null) lblBundleFin.setText(formatNumber(kanryoBundoleFinal));
                if (lblWeightFin != null) lblWeightFin.setText(formatNumber(kanryoJyuryoTonFinal));

                if (lblContainerPlan != null)
                    lblContainerPlan.setText(formatNumber(containerCountFinal));
                if (lblBundlePlan != null) lblBundlePlan.setText(formatNumber(goukeiBundoleFinal));
                if (lblWeightPlan != null)
                    lblWeightPlan.setText(formatNumber(goukeiJyuryoTonFinal));

                if (lblZanContainer != null)
                    lblZanContainer.setText(formatRemaining(zanContainerFinal));
                if (lblZanBundle != null) lblZanBundle.setText(formatRemaining(zanBundleFinal));
                if (lblZanWeight != null) lblZanWeight.setText(formatRemaining(zanWeightFinal));
            });
        });
    }

    private String formatNumber(long value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    private String formatRemaining(long value) {
        if (value == 0) return "";
        return formatNumber(value);
    }

    private int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (io != null) io.shutdownNow();
    }
}
