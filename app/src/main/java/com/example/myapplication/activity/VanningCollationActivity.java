package com.example.myapplication.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.grid.VanningCollationController;
import com.example.myapplication.grid.VanningCollationRow;
import com.example.myapplication.scanner.DensoScannerController;
import com.example.myapplication.scanner.OnScanListener;
import com.example.myapplication.settings.HandyUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//============================================================
//　処理概要　:　積込照合(バンニング照合)画面のActivity
//　　　　　　:　コンテナに紐づく積載束情報を表示し、現品番号(スキャナ入力)で照合を進め、
//　　　　　　:　全件照合完了後に確定処理(DB更新＋送信)を行う。
//　関　　数　:　onCreate                    ..... 画面生成/初期化
//　　　　　　:　bindViews                  ..... 画面部品の取得/初期設定
//　　　　　　:　setupBottomButtons         ..... 下部ボタンの表示設定
//　　　　　　:　setupRecycler              ..... 一覧(RecyclerView)設定
//　　　　　　:　initScanner                ..... スキャナ初期化(当画面専用/Code39制御)
//　　　　　　:　setupInputHandlers         ..... 入力(現品番号)イベント設定
//　　　　　　:　loadFromIntent             ..... 画面引数の取得/表示
//　　　　　　:　loadCollationData          ..... 照合対象データ取得/表示反映
//　　　　　　:　updateUiForContainers      ..... 照合対象有無に応じたUI制御
//　　　　　　:　handleGenpinInput          ..... 現品番号入力処理(抽出/チェック/照合更新)
//　　　　　　:　updateReadCount            ..... 読取(照合済)件数の表示更新
//　　　　　　:　onFunctionBlue             ..... (青)確定ボタン処理
//　　　　　　:　onFunctionRed              ..... (赤)処理なし
//　　　　　　:　onFunctionGreen            ..... (緑)処理なし
//　　　　　　:　onFunctionYellow           ..... (黄)終了ボタン処理
//　　　　　　:　procRegister               ..... 確定処理(完了チェック/DB更新/送信)
//　　　　　　:　showRegisterCompleteFlow   ..... 送信結果に応じた完了フロー表示
//　　　　　　:　showRegisterCompleteInfoAndFinish ..... 確定完了表示＆終了
//　　　　　　:　checkSyougouKanryo         ..... 未照合件数のチェック
//　　　　　　:　registerDb                 ..... DBトランザクション更新
//　　　　　　:　onResume                   ..... スキャナ開始/プロファイル反映
//　　　　　　:　onPause                    ..... スキャナ停止
//　　　　　　:　onDestroy                  ..... リソース解放
//　　　　　　:　dispatchKeyEvent           ..... キーイベント(スキャナ)委譲
//　　　　　　:　safeStr                    ..... null安全文字列
//　　　　　　:　trimSagyouYmd              ..... 作業日時表示用トリム
//　　　　　　:　VanningCollationAdapter    ..... 一覧表示用Adapter(内部クラス)
//============================================================

public class VanningCollationActivity extends BaseActivity {

    public static final String EXTRA_CONTAINER_ID = "extra_container_id"; // コンテナID受け渡しキー
    public static final String EXTRA_CONTAINER_NO = "extra_container_no"; // コンテナNo受け渡しキー
    public static final String EXTRA_BUNDLE_CNT = "extra_bundle_cnt";     // 束数受け渡しキー
    public static final String EXTRA_SAGYOU_YMD = "extra_sagyou_ymd";     // 作業日時受け渡しキー

    private EditText etContainerNo;     // コンテナNo
    private EditText etBundleCount;     // 積載束数
    private EditText etSagyouYmd;       // 作業日時
    private EditText etGenpinNo;        // 現品No入力
    private TextView tvReadCount;       // 読取件数表示
    private RecyclerView rvBundles;     // 照合一覧
    private MaterialButton btnBlue;     // 下部青ボタン
    private MaterialButton btnRed;      // 下部赤ボタン
    private MaterialButton btnGreen;    // 下部緑ボタン
    private MaterialButton btnYellow;   // 下部黄ボタン

    private ExecutorService io;                   // I/O処理スレッド
    private VanningCollationController controller; // 画面制御ロジック
    private VanningCollationAdapter adapter;      // 一覧アダプター

    // この画面専用スキャナ
    private DensoScannerController scanner; // DENSOスキャナ制御
    private boolean scannerCreated = false; // スキャナ初期化済みフラグ

    private String containerId; // 照合対象コンテナID
    private boolean confirmed;  // 確定済みフラグ

    //================================================================
    //　機　能　:　画面生成/初期化
    //　引　数　:　savedInstanceState ..... 状態(復元用)
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vanning_collation);

        // DB/IO用の単一スレッドを生成
        io = Executors.newSingleThreadExecutor();

        // 画面部品の取得
        bindViews();

        // 下部ボタン設定
        setupBottomButtons();

        // 一覧設定
        setupRecycler();

        // 入力イベント設定（フォーカスイベントでスキャナprofile更新）
        setupInputHandlers();

        // 画面引数の取得/表示
        loadFromIntent();

        // 照合対象データの取得/表示
        loadCollationData();

        // RecyclerViewの行間調整（上方向に詰める）
        RecyclerView rvBundles = findViewById(R.id.rvBundles);
        rvBundles.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position > 0) {
                    outRect.top = -2;
                }
            }
        });
    }

    //================================================================
    //　機　能　:　画面部品の取得/初期設定
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void bindViews() {
        etContainerNo = findViewById(R.id.etContainerNo);
        etBundleCount = findViewById(R.id.etSekisaiBundleCount);
        etSagyouYmd = findViewById(R.id.etSagyouDateTime);
        etGenpinNo = findViewById(R.id.etGenpinNo);
        tvReadCount = findViewById(R.id.tvReadCount);
        rvBundles = findViewById(R.id.rvBundles);
        btnBlue = findViewById(R.id.btnBottomBlue);
        btnRed = findViewById(R.id.btnBottomRed);
        btnGreen = findViewById(R.id.btnBottomGreen);
        btnYellow = findViewById(R.id.btnBottomYellow);

        // 表示専用項目は編集不可
        if (etContainerNo != null) etContainerNo.setEnabled(false);
        if (etBundleCount != null) etBundleCount.setEnabled(false);
        if (etSagyouYmd != null) etSagyouYmd.setEnabled(false);
    }

    //================================================================
    //　機　能　:　下部ボタンの表示設定
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void setupBottomButtons() {
        if (btnBlue != null) btnBlue.setText("確定");
        if (btnRed != null) btnRed.setText("");
        if (btnGreen != null) btnGreen.setText("");
        if (btnYellow != null) btnYellow.setText("終了");

        // ボタン活性制御を反映
        refreshBottomButtonsEnabled();
    }

    //================================================================
    //　機　能　:　一覧(RecyclerView)設定
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void setupRecycler() {
        adapter = new VanningCollationAdapter();
        rvBundles.setLayoutManager(new LinearLayoutManager(this));
        rvBundles.setAdapter(adapter);

        // 一覧にフォーカスが来ても現品番号入力に戻す
        rvBundles.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etGenpinNo != null) {
                etGenpinNo.requestFocus();
            }
        });
    }

    //================================================================
    //　機　能　:　スキャナ初期化(当画面専用/Code39制御)
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void initScanner() {
        if (scannerCreated) return;

        // 当画面は、etGenpinNoフォーカス中のみ Code39 受け取り（policyで制御）
        scanner = new DensoScannerController(
                this,
                new OnScanListener() {
                    @Override
                    public void onScan(String normalizedData, @Nullable String aim, @Nullable String denso) {
                        runOnUiThread(() -> {
                            // スキャン結果を現品番号に反映し、入力確定処理を実行
                            if (etGenpinNo != null) etGenpinNo.setText(normalizedData);
                            handleGenpinInput();
                        });
                    }
                },
                DensoScannerController.createFocusCode39Policy(etGenpinNo)
        );

        scanner.onCreate();
        scannerCreated = true;
    }

    //================================================================
    //　機　能　:　入力(現品番号)イベント設定
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void setupInputHandlers() {
        if (etGenpinNo == null) return;

        // フォーカス変化時に、プロファイルを即時反映（NONE⇔CODE39_ONLY）
        etGenpinNo.setOnFocusChangeListener((v, hasFocus) -> {
            if (scanner != null) scanner.refreshProfile("GenpinFocus=" + hasFocus);
        });

        // Enterキーで入力確定（手入力時も想定）
        etGenpinNo.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                handleGenpinInput();
                return true;
            }
            return false;
        });
    }

    //================================================================
    //　機　能　:　画面引数の取得/表示
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void loadFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        // Intentから値取得
        containerId = intent.getStringExtra(EXTRA_CONTAINER_ID);
        String containerNo = intent.getStringExtra(EXTRA_CONTAINER_NO);
        int bundleCnt = intent.getIntExtra(EXTRA_BUNDLE_CNT, 0);
        String sagyouYmd = intent.getStringExtra(EXTRA_SAGYOU_YMD);

        // 画面表示へ反映
        if (etContainerNo != null) etContainerNo.setText(safeStr(containerNo));
        if (etBundleCount != null) etBundleCount.setText(String.valueOf(bundleCnt));
        if (etSagyouYmd != null) etSagyouYmd.setText(trimSagyouYmd(sagyouYmd));
    }

    //================================================================
    //　機　能　:　照合対象データ取得/表示反映
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void loadCollationData() {
        showLoadingShort();

        // DBアクセスは別スレッドで実行
        io.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                // 照合制御クラスを生成し、対象コンテナの明細をロード
                controller = new VanningCollationController(db.kakuninMeisaiDao(), db.kakuninMeisaiWorkDao());
                controller.load(containerId);

                // UI更新はメインスレッドで実行
                runOnUiThread(() -> {
                    hideLoadingShort();
                    adapter.submitList(controller.getDisplayRows());
                    updateReadCount();
                    updateUiForContainers();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation loadCollationData", ex);
                });
            }
        });
    }

    //================================================================
    //　機　能　:　照合対象有無に応じたUI制御
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void updateUiForContainers() {
        boolean hasRows = controller != null && !controller.getDetails().isEmpty();

        // 対象が無い場合は入力不可・確定ボタン非表示
        if (!hasRows) {
            showWarningMsg("照合対象の積載束情報がありません。", MsgDispMode.MsgBox);
            if (etGenpinNo != null) etGenpinNo.setEnabled(false);
            if (btnBlue != null) btnBlue.setText("");
        } else {
            // 対象がある場合は入力可・確定ボタン表示
            if (etGenpinNo != null) {
                etGenpinNo.setEnabled(true);
                etGenpinNo.requestFocus();
            }
            if (btnBlue != null) btnBlue.setText("確定");
        }

        // ボタン活性制御を反映
        refreshBottomButtonsEnabled();

        // 有効/無効が変わったのでprofile再反映
        if (scanner != null) scanner.refreshProfile("updateUiForContainers");
    }

    //================================================================
    //　機　能　:　現品番号入力処理(抽出/チェック/照合更新)
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void handleGenpinInput() {
        if (controller == null) return;

        // 入力値を取得（前後空白除去）
        String input = etGenpinNo != null && etGenpinNo.getText() != null
                ? etGenpinNo.getText().toString().trim()
                : "";

        // 未入力チェック
        if (TextUtils.isEmpty(input)) {
            showWarningMsg("現品番号が未入力です", MsgDispMode.MsgBox);
            if (etGenpinNo != null) etGenpinNo.requestFocus();
            return;
        }

        showLoadingShort();

        // 解析/照合は別スレッドで実行
        io.execute(() -> {
            String heatNo;
            String sokuban;

            try {
                // 現品番号の桁数に応じて熱番/束番を抽出
                if (input.length() == 13) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 13);
                } else if (input.length() == 14) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14);
                } else if (input.length() == 18) {
                    heatNo = input.substring(1, 7);
                    sokuban = input.substring(7, 14).trim();
                } else {
                    // 入力桁数不正
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg("現品番号は13桁か14桁か18桁で入力してください", MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                // 明細存在/照合可否チェック
                String errMsg = controller.checkSokuDtl(heatNo, sokuban);
                if (!TextUtils.isEmpty(errMsg) && !"OK".equals(errMsg)) {
                    runOnUiThread(() -> {
                        hideLoadingShort();
                        showWarningMsg(errMsg, MsgDispMode.MsgBox);
                        if (etGenpinNo != null) etGenpinNo.requestFocus();
                    });
                    return;
                }

                // 照合状態を更新
                controller.updateSyougo(heatNo, sokuban);

                // UIを更新
                runOnUiThread(() -> {
                    hideLoadingShort();
                    adapter.submitList(controller.getDisplayRows());
                    updateReadCount();
                    if (etGenpinNo != null) {
                        // 次の入力に備えてクリア
                        etGenpinNo.setText("");
                        etGenpinNo.requestFocus();
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation handleGenpinInput", ex);
                });
            }
        });
    }

    //================================================================
    //　機　能　:　読取(照合済)件数の表示更新
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void updateReadCount() {
        int count = controller != null ? controller.getSyougouSumiCount() : 0;
        if (tvReadCount != null) {
            tvReadCount.setText(String.format(Locale.JAPAN, "%2d", count));
        }
    }

    //================================================================
    //　機　能　:　(青)確定ボタン処理
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onFunctionBlue() {
        // 既に確定済みなら二重実行しない
        if (confirmed) return;
        procRegister();
    }

    //================================================================
    //　機　能　:　(赤)処理なし
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onFunctionRed() {
    }

    //================================================================
    //　機　能　:　(緑)処理なし
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onFunctionGreen() {
    }

    //================================================================
    //　機　能　:　(黄)終了ボタン処理
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onFunctionYellow() {
        // 確定済みならそのまま終了
        if (confirmed) {
            finish();
            return;
        }

        // 未確定なら確認ダイアログを表示
        showQuestion("確定処理が行われていません。現在の内容は破棄されます。画面を終了してもよろしいですか？",
                yes -> {
                    if (yes) {
                        finish();
                    } else if (etGenpinNo != null) {
                        etGenpinNo.requestFocus();
                    }
                });
    }

    //================================================================
    //　機　能　:　確定処理(完了チェック/DB更新/送信)
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void procRegister() {
        if (controller == null) return;

        showLoadingShort();

        // DB更新/送信は別スレッドで実行
        io.execute(() -> {
            try {
                // 未照合が残っていないか確認
                if (!checkSyougouKanryo()) {
                    runOnUiThread(this::hideLoadingShort);
                    return;
                }

                // DBへ確定内容を反映
                registerDb();

                // 送信処理（照合のみ送信）
                DataSync sync = new DataSync(getApplicationContext());
                boolean sent = sync.sendSyougoOnly();

                runOnUiThread(() -> {
                    hideLoadingShort();
                    if (!sent) {
                        showRegisterCompleteFlow("照合データの更新に失敗しました");
                        return;
                    }
                    showRegisterCompleteFlow(null);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    hideLoadingShort();
                    errorProcess("VanningCollation procRegister", ex);
                });
            }
        });
    }

    //================================================================
    //　機　能　:　送信結果に応じた完了フロー表示
    //　引　数　:　sendErrorMessage ..... 送信エラーメッセージ(null/空なら成功扱い)
    //　戻り値　:　[void]
    //================================================================
    private void showRegisterCompleteFlow(@Nullable String sendErrorMessage) {
        // 送信成功の場合は完了表示へ
        if (TextUtils.isEmpty(sendErrorMessage)) {
            showRegisterCompleteInfoAndFinish();
            return;
        }

        // 送信失敗の場合はエラー表示後、完了表示へ遷移
        HandyUtil.playErrorBuzzer(this);
        HandyUtil.playVibrater(this);
        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage(sendErrorMessage)
                .setCancelable(false)
                .setPositiveButton("OK", (d1, w1) -> {
                    d1.dismiss();
                    getWindow().getDecorView().post(this::showRegisterCompleteInfoAndFinish);
                })
                .show();
    }

    //================================================================
    //　機　能　:　確定完了表示＆終了
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void showRegisterCompleteInfoAndFinish() {
        HandyUtil.playSuccessBuzzer(this);
        HandyUtil.playVibrater(this);
        new AlertDialog.Builder(this)
                .setTitle("情報")
                .setMessage("積載束照合を確定しました")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    // 二重実行防止フラグON
                    confirmed = true;

                    // 呼び出し元へOK返却
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    //================================================================
    //　機　能　:　未照合件数のチェック
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... True:照合完了、False:未完了あり
    //================================================================
    private boolean checkSyougouKanryo() {
        int remaining = controller != null ? controller.getUncollatedCount() : 0;
        if (remaining != 0) {
            runOnUiThread(() -> {
                showWarningMsg("照合が完了していません", MsgDispMode.MsgBox);
                if (etGenpinNo != null) etGenpinNo.requestFocus();
            });
            return false;
        }
        return true;
    }

    //================================================================
    //　機　能　:　DBトランザクション更新
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    private void registerDb() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        db.runInTransaction(() -> controller.markContainerCollated(db.kakuninContainerDao()));
    }

    //================================================================
    //　機　能　:　画面復帰時処理(スキャナ開始/プロファイル反映)
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onResume() {
        super.onResume();

        // viewツリー準備後にスキャナ初期化/開始
        getWindow().getDecorView().post(() -> {
            initScanner();
            if (scanner != null) scanner.onResume();
            if (scanner != null) scanner.refreshProfile("onResume");
        });
    }

    //================================================================
    //　機　能　:　画面離脱時処理(スキャナ停止)
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onPause() {
        if (scanner != null) scanner.onPause();
        super.onPause();
    }

    //================================================================
    //　機　能　:　画面破棄時処理(リソース解放)
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Override
    protected void onDestroy() {
        // スキャナ解放
        if (scanner != null) scanner.onDestroy();
        scannerCreated = false;

        // スレッド解放
        if (io != null) io.shutdownNow();

        super.onDestroy();
    }

    //================================================================
    //　機　能　:　キーイベント(スキャナ)委譲
    //　引　数　:　event ..... キーイベント
    //　戻り値　:　[boolean] ..... True:処理済、False:未処理
    //================================================================
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // スキャナ側で処理できるキーはここで消費
        if (scanner != null && scanner.handleDispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    //================================================================
    //　機　能　:　null安全文字列
    //　引　数　:　value ..... 文字列
    //　戻り値　:　[String] ..... nullの場合は空文字、それ以外はそのまま返却
    //================================================================
    private String safeStr(String value) {
        return value == null ? "" : value;
    }

    //================================================================
    //　機　能　:　作業日時表示用トリム
    //　引　数　:　value ..... 作業日時文字列
    //　戻り値　:　[String] ..... 16文字までに丸めた作業日時
    //================================================================
    private String trimSagyouYmd(String value) {
        if (value == null) return "";
        return value.length() <= 16 ? value : value.substring(0, 16);
    }

    //================================================================
    //　機　能　:　一覧表示用Adapter(内部クラス)
    //　引　数　:　なし
    //　戻り値　:　[なし]
    //================================================================
    private static class VanningCollationAdapter extends RecyclerView.Adapter<VanningCollationAdapter.ViewHolder> {
        private final List<VanningCollationRow> rows = new ArrayList<>();

        //================================================================
        //　機　能　:　表示行リストの更新
        //　引　数　:　newRows ..... 表示する行リスト
        //　戻り値　:　[void]
        //================================================================
        void submitList(List<VanningCollationRow> newRows) {
            // 既存行をクリアして差し替え
            rows.clear();
            if (newRows != null) rows.addAll(newRows);

            // 全件更新
            notifyDataSetChanged();
        }

        //================================================================
        //　機　能　:　ViewHolder生成
        //　引　数　:　parent ..... 親ViewGroup
        //　　　　　:　viewType ... View種別
        //　戻り値　:　[ViewHolder] ..... 生成したViewHolder
        //================================================================
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vanning_collation_row, parent, false);
            return new ViewHolder(view);
        }

        //================================================================
        //　機　能　:　行データの表示反映
        //　引　数　:　holder ..... ViewHolder
        //　　　　　:　position ... 位置
        //　戻り値　:　[void]
        //================================================================
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            VanningCollationRow row = rows.get(position);

            // 各項目を表示へ反映
            holder.tvPNo.setText(row.pNo);
            holder.tvBNo.setText(row.bNo);
            holder.tvIndex.setText(row.index);
            holder.tvJyuryo.setText(row.jyuryo);
            holder.tvConfirmed.setText(row.confirmed);
        }

        //================================================================
        //　機　能　:　行数取得
        //　引　数　:　なし
        //　戻り値　:　[int] ..... 行数
        //================================================================
        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvPNo;
            final TextView tvBNo;
            final TextView tvIndex;
            final TextView tvJyuryo;
            final TextView tvConfirmed;

            //================================================================
            //　機　能　:　ViewHolder生成(行View内の部品取得)
            //　引　数　:　itemView ..... 行View
            //　戻り値　:　[void]
            //================================================================
            ViewHolder(android.view.View itemView) {
                super(itemView);

                // 行View内の表示部品を取得
                tvPNo = itemView.findViewById(R.id.tvRowPNo);
                tvBNo = itemView.findViewById(R.id.tvRowBNo);
                tvIndex = itemView.findViewById(R.id.tvRowIndex);
                tvJyuryo = itemView.findViewById(R.id.tvRowJyuryo);
                tvConfirmed = itemView.findViewById(R.id.tvRowConfirmed);
            }
        }
    }
}
