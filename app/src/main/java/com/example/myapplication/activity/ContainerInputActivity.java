package com.example.myapplication.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.SyukkaContainerEntity;
import com.example.myapplication.settings.HandyUtil;
import com.example.myapplication.time.DateTimeFormatUtil;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

//============================================================
//　処理概要　:　コンテナ情報入力画面（Activity）
//　関　　数　:　onCreate ....................... 画面生成時の初期化
//　　　　　　:　onDestroy ...................... 画面終了時の後処理
//　　　　　　:　bindViews ...................... 画面部品のバインド
//　　　　　　:　setupBottomButtons ............. 下部ボタン設定
//　　　　　　:　setupInputHandlers ............. 入力イベント設定
//　　　　　　:　setupEnterFocus ................ Enter/IMEでフォーカス移動設定
//　　　　　　:　isEnterAction .................. Enter/IME判定
//　　　　　　:　setupPhotoHandlers ............. 写真撮影/プレビュー設定
//　　　　　　:　loadInitialData ................ 初期データ読込（DB/引継/設定）
//　　　　　　:　initForm ....................... 画面項目初期化
//　　　　　　:　loadPassedValues ............... 前画面引継値読込
//　　　　　　:　updateCheckDigit ............... チェックデジット更新
//　　　　　　:　persistContainerWeights ........ コンテナ/ダンネージ重量を保存（Preferences）
//　　　　　　:　calcJyuryo ..................... 総重量/残重量計算
//　　　　　　:　launchCamera ................... カメラ起動（権限チェック含む）
//　　　　　　:　launchCameraInternal ........... カメラ起動（内部処理）
//　　　　　　:　showPreview .................... 画像プレビュー表示
//　　　　　　:　restorePhoto ................... 引継URIから画像復元
//　　　　　　:　onFunctionBlue ................. 確定（登録）処理入口
//　　　　　　:　confirmSealThenRegister ........ シール写真確認→登録
//　　　　　　:　onFunctionYellow ............... 終了
//　　　　　　:　validateRequiredFields ......... 入力必須チェック
//　　　　　　:　getRemainingWeight ............. 残重量取得
//　　　　　　:　needsContainerPhotoConfirm ..... コンテナ写真未撮影確認要否
//　　　　　　:　needsSealPhotoConfirm .......... シール写真未撮影確認要否
//　　　　　　:　hasImage ....................... 画像有無判定
//　　　　　　:　procRegister ................... DB登録＋画像保存＋送信
//　　　　　　:　showCompleteFlow ............... 完了/エラーフロー表示
//　　　　　　:　showCompleteInfoAndFinish ...... 完了メッセージ後終了
//　　　　　　:　buildSendFailedMessage ......... 送信失敗メッセージ生成
//　　　　　　:　registerDb ..................... DB登録（Transaction）
//　　　　　　:　buildContainerNo ............... コンテナNo生成（チェックデジット付）
//　　　　　　:　resolveContainerSize ........... コンテナサイズ取得
//　　　　　　:　saveImageFile .................. 画像ファイル保存
//　　　　　　:　getImageFile ................... 画像保存先ファイル取得
//　　　　　　:　nowAsText ...................... 現在日時文字列生成
//　　　　　　:　resolveDefaultContainerWeight .. 既定コンテナ重量取得
//　　　　　　:　resolveDefaultDunnageWeight .... 既定ダンネージ重量取得
//　　　　　　:　resolveMaxContainerWeight ...... 最大積載重量取得
//　　　　　　:　safeInt ..... 安全な数値変換処理
//　　　　　　:　getIntFromEdit ................. EditText→int取得
//　　　　　　:　safeText ....................... EditText→String安全取得
//　　　　　　:　defaultString .................. null→""変換
//　　　　　　:　formatNumber ................... 数値整形（カンマ区切り）
//　　　　　　:　toast .......................... トースト表示
//　　　　　　:　finish ......................... 終了時：引継値保存＋返却
//　　　　　　:　saveContainerValues ............ 引継用Mapへ入力値保存
//============================================================

public class ContainerInputActivity extends BaseActivity {

    private static final String TAG = "ContainerInput";       // ログタグ
    private static final int SYSTEM_RENBAN = 1;              // システム連番

    public static final String EXTRA_BUNDLE_VALUES = "container_input_bundle_values"; // 束入力値受け渡しキー
    public static final String EXTRA_CONTAINER_VALUES = "container_input_values";    // コンテナ入力値受け渡しキー

    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo";          // コンテナ重量キー
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";              // ダンネージ重量キー
    private static final String PREFS_CONTAINER_JYURYO = "prefs_container_jyuryo";  // コンテナ重量設定キー
    private static final String PREFS_DUNNAGE_JYURYO = "prefs_dunnage_jyuryo";      // ダンネージ重量設定キー
    private static final String KEY_CONTAINER_NO1 = "container_no1";                // コンテナ番号1キー
    private static final String KEY_CONTAINER_NO2 = "container_no2";                // コンテナ番号2キー
    private static final String KEY_SEAL_NO = "seal_no";                            // シール番号キー
    private static final String KEY_CONTAINER_PHOTO_URI = "container_photo_uri";    // コンテナ写真URIキー
    private static final String KEY_SEAL_PHOTO_URI = "seal_photo_uri";              // シール写真URIキー

    private static final String MSG_CONTAINER_CONFIRMED = "コンテナ情報を確定しました"; // 確定メッセージ

    private Button btnPhotoContainerNo; // コンテナ番号写真ボタン
    private Button btnPhotoSealNo;      // シール番号写真ボタン

    private ImageView ivPhotoContainer; // コンテナ写真プレビュー
    private ImageView ivPhotoSeal;      // シール写真プレビュー

    private EditText etContainerNo1;    // コンテナNo（前半）
    private EditText etContainerNo2;    // コンテナNo（後半）
    private EditText etContainerKg;     // コンテナ重量
    private EditText etDunnageKg;       // ダンネージ重量
    private EditText etSealNo;          // シールNo
    private EditText etBookingNo;       // Booking No

    private TextView tvCheckDigit;      // チェックデジット
    private TextView tvBansenKg;        // 番線重量
    private TextView tvBundleCount;     // 束本数
    private TextView tvTotalKg;         // 総重量
    private TextView tvRemainKg;        // 残重量


    //写真撮影ターゲット（どちらの写真を撮るか）
    private enum PhotoTarget {CONTAINER, SEAL}

    private PhotoTarget currentTarget = PhotoTarget.CONTAINER; // 現在の撮影対象

    private ExecutorService io; // I/O処理スレッド
    private AppDatabase db;     // DBインスタンス

    // 前画面から受け取った束情報（重量など）を保持
    private final java.util.Map<String, String> bundleValues = new java.util.HashMap<>();
    // 当画面の入力値（戻る/再表示用）を保持
    private final java.util.Map<String, String> containerValues = new java.util.HashMap<>();

    private int bundleCount = 0;        // 束数（個数）
    private int sekisaiSokuJyuryo = 0;  // 積載束重量（kg）
    private int maxContainerJyuryo = 0; // 最大積載可能重量（kg）

    private Uri containerPhotoUri; // コンテナ写真URI
    private Uri sealPhotoUri;      // シール写真URI

    /**
     * 撮影画面からの戻りを受け取るランチャー。
     * 返却されたURIをターゲット別（コンテナ/シール）に保持し、プレビュー表示する。
     */
    @ExperimentalCamera2Interop
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                // 1) キャンセル/戻り判定
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    toast("撮影キャンセル");
                    Log.i(TAG, "Camera canceled");
                    return;
                }

                // 2) 撮影結果のURIとターゲット(コンテナ/シール)を取得
                String uriStr = result.getData().getStringExtra(PhotographingActivity.EXTRA_RESULT_URI);
                String target = result.getData().getStringExtra(PhotographingActivity.EXTRA_TARGET);

                // 3) URI取得失敗時のガード
                if (uriStr == null) {
                    toast("画像URIが取得できませんでした");
                    Log.e(TAG, "result uri is null");
                    return;
                }

                Uri uri = Uri.parse(uriStr);

                // 4) ターゲット別にURI保持＋プレビュー表示
                if ("CONTAINER".equals(target)) {
                    containerPhotoUri = uri;

                    // 同URI再描画対策（setImageURIが反映されないケースを回避）
                    ivPhotoContainer.setImageURI(null);
                    ivPhotoContainer.setImageURI(uri);

                    toast("コンテナNo写真を表示しました");
                } else {
                    sealPhotoUri = uri;

                    ivPhotoSeal.setImageURI(null);
                    ivPhotoSeal.setImageURI(uri);

                    toast("シールNo写真を表示しました");
                }
            });

    /**
     * カメラ権限要求の結果を受け取るランチャー。
     */
    @ExperimentalCamera2Interop
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    // 権限OK → カメラ起動（内部処理）
                    launchCameraInternal();
                } else {
                    // 権限NG → メッセージのみ
                    toast("カメラ権限が拒否されています");
                }
            });

    //============================================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... 画面再生成時の保存状態
    //　戻り値　:　[void] ..... なし
    //============================================================
    @ExperimentalCamera2Interop
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画面レイアウト設定
        setContentView(R.layout.activity_container_input);

        // DBアクセス用スレッド/DBインスタンスを準備
        io = Executors.newSingleThreadExecutor();
        db = AppDatabase.getInstance(getApplicationContext());

        // 画面部品の紐づけ
        bindViews();

        // 下部ボタンの表示/活性を設定
        setupBottomButtons();

        // 入力欄のイベント/フォーカス移動を設定
        setupInputHandlers();

        // 写真撮影ボタン/プレビューを設定
        setupPhotoHandlers();

        // 前画面からの引継値を読み込み
        loadPassedValues(getIntent());

        // 初期表示（DB/引継/設定値を反映）
        loadInitialData();
    }

    //============================================================
    //　機　能　:　画面終了時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ExecutorServiceの解放
        if (io != null) {
            io.shutdown();
        }
    }

    //============================================================
    //　機　能　:　画面部品を取得してメンバーに保持する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void bindViews() {
        // ボタン
        btnPhotoContainerNo = findViewById(R.id.btnPhotoContainerNo);
        btnPhotoSealNo = findViewById(R.id.btnPhotoSealNo);

        // 画像プレビュー
        ivPhotoContainer = findViewById(R.id.ivPhotoContainer);
        ivPhotoSeal = findViewById(R.id.ivPhotoSeal);

        // 入力欄
        etContainerNo1 = findViewById(R.id.etContainerNo1);
        etContainerNo2 = findViewById(R.id.etContainerNo2);
        etContainerKg = findViewById(R.id.etContainerKg);
        etDunnageKg = findViewById(R.id.etDunnageKg);
        etSealNo = findViewById(R.id.etSealNo);
        etBookingNo = findViewById(R.id.etYoyakuNo);

        // 表示欄
        tvCheckDigit = findViewById(R.id.tvCheckDigit);
        tvBansenKg = findViewById(R.id.tvBansenKg);
        tvBundleCount = findViewById(R.id.tvSekisaiBundleCount);
        tvTotalKg = findViewById(R.id.tvTotalKg);
        tvRemainKg = findViewById(R.id.tvRemainKg);
    }

    //============================================================
    //　機　能　:　下部ボタンの表示内容を設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupBottomButtons() {
        MaterialButton btnBlue = findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = findViewById(R.id.btnBottomYellow);

        // ボタン文言設定
        if (btnBlue != null) btnBlue.setText("確定");
        if (btnRed != null) btnRed.setText("");
        if (btnGreen != null) btnGreen.setText("");
        if (btnYellow != null) btnYellow.setText("終了");

        // 活性制御（BaseActivity側の共通処理想定）
        refreshBottomButtonsEnabled();
    }

    //============================================================
    //　機　能　:　入力イベントを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupInputHandlers() {
        // コンテナNo入力 → チェックデジット更新
        if (etContainerNo1 != null) etContainerNo1.addTextChangedListener(containerNoWatcher);
        if (etContainerNo2 != null) etContainerNo2.addTextChangedListener(containerNoWatcher);

        // 重量入力 → 総重量/残重量計算＋Preferences保存
        if (etContainerKg != null) etContainerKg.addTextChangedListener(weightWatcher);
        if (etDunnageKg != null) etDunnageKg.addTextChangedListener(weightWatcher);

        // Enter/IMEでのフォーカス移動設定
        setupEnterFocus(etContainerNo1, etContainerNo2);
        setupEnterFocus(etContainerNo2, etContainerKg);
        setupEnterFocus(etContainerKg, etDunnageKg);
        setupEnterFocus(etDunnageKg, etSealNo);
        setupEnterFocus(etSealNo, etContainerNo1);

        // BookingNoは編集不可（表示専用）
        if (etBookingNo != null) {
            etBookingNo.setFocusable(false);
            etBookingNo.setFocusableInTouchMode(false);
        }
    }

    //============================================================
    //　機　能　:　Enter/IME入力でフォーカスを移動する
    //　引　数　:　from ..... EditText（移動元）
    //　　　　　:　to ..... EditText（移動先）
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setupEnterFocus(@Nullable EditText from, @Nullable EditText to) {
        if (from == null) return;

        // 物理Enterキー対応
        from.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_ENTER) return false;

            // 押下時のみ処理（UPでは処理しない）
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (to != null) {
                    to.requestFocus();
                } else {
                    // 移動先がnullの場合は自身に戻す
                    from.requestFocus();
                }
            }
            return true;
        });

        // IMEアクション（Next/Done等）対応
        from.setOnEditorActionListener((v, actionId, event) -> {
            if (!isEnterAction(actionId, event)) return false;

            if (to != null) {
                to.requestFocus();
            } else {
                from.requestFocus();
            }
            return true;
        });
    }

    //============================================================
    //　機　能　:　Enter/IMEアクションかを判定する
    //　引　数　:　actionId ..... ID
    //　　　　　:　event ..... KeyEvent（キーイベント）
    //　戻り値　:　[boolean] ..... Enter相当ならtrue
    //============================================================
    private boolean isEnterAction(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }

    //============================================================
    //　機　能　:　写真関連のイベントを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @ExperimentalCamera2Interop
    private void setupPhotoHandlers() {
        // コンテナ写真ボタン
        if (btnPhotoContainerNo != null) {
            // 入力フォーカス対象から外す（現場端末の操作性向上）
            btnPhotoContainerNo.setFocusable(false);
            btnPhotoContainerNo.setFocusableInTouchMode(false);

            btnPhotoContainerNo.setOnClickListener(v -> {
                // 撮影ターゲット設定 → カメラ起動
                currentTarget = PhotoTarget.CONTAINER;
                launchCamera();
            });
        }

        // シール写真ボタン
        if (btnPhotoSealNo != null) {
            btnPhotoSealNo.setFocusable(false);
            btnPhotoSealNo.setFocusableInTouchMode(false);

            btnPhotoSealNo.setOnClickListener(v -> {
                currentTarget = PhotoTarget.SEAL;
                launchCamera();
            });
        }

        // プレビュータップで拡大表示
        if (ivPhotoContainer != null) {
            ivPhotoContainer.setOnClickListener(v -> showPreview(ivPhotoContainer));
        }
        if (ivPhotoSeal != null) {
            ivPhotoSeal.setOnClickListener(v -> showPreview(ivPhotoSeal));
        }
    }

    //============================================================
    //　機　能　:　初期データを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void loadInitialData() {
        // 画面項目の初期化
        initForm();

        // DB操作はバックグラウンドスレッドで実行
        io.execute(() -> {
            try {
                // 1) システム設定（既定重量/最大重量など）を取得
                SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);

                // 2) 作業中明細の集計（束数・重量・予約No.など）を取得
                SyukkaMeisaiWorkDao.WorkSummary summary = db.syukkaMeisaiWorkDao().getWorkSummary();

                // 3) 初期値を解決（DB設定値が無い場合のフォールバック含む）
                int defaultContainer = resolveDefaultContainerWeight(system);
                int defaultDunnage = resolveDefaultDunnageWeight(system);
                maxContainerJyuryo = resolveMaxContainerWeight(system);

                // 4) 集計値を画面用フィールドへ保持
                bundleCount = summary != null ? summary.sokusu : 0;
                sekisaiSokuJyuryo = summary != null ? safeInt(summary.jyuryo) : 0;
                String bookingNo = summary != null ? summary.bookingNo : "";

                // 5) 引継値（当画面の入力保持）を取得
                String savedContainer = containerValues.get(KEY_CONTAINER_JYURYO);
                String savedDunnage = containerValues.get(KEY_DUNNAGE_JYURYO);

                // 6) 前回入力（Preferences）を取得
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                String prefContainer = prefs.getString(PREFS_CONTAINER_JYURYO, "");
                String prefDunnage = prefs.getString(PREFS_DUNNAGE_JYURYO, "");

                // 7) その他引継値を取得
                String savedNo1 = containerValues.get(KEY_CONTAINER_NO1);
                String savedNo2 = containerValues.get(KEY_CONTAINER_NO2);
                String savedSeal = containerValues.get(KEY_SEAL_NO);
                String savedContainerPhoto = containerValues.get(KEY_CONTAINER_PHOTO_URI);
                String savedSealPhoto = containerValues.get(KEY_SEAL_PHOTO_URI);

                // 8) UI反映はメインスレッドで実行
                runOnUiThread(() -> {
                    // 束側の重量（前画面からの値）を取得
                    String bundleContainer = bundleValues.get(KEY_CONTAINER_JYURYO);
                    String bundleDunnage = bundleValues.get(KEY_DUNNAGE_JYURYO);

                    // コンテナ自重：優先順位＝当画面引継 → Preferences → 前画面引継 → DB既定
                    if (etContainerKg != null) {
                        if (!TextUtils.isEmpty(savedContainer)) {
                            etContainerKg.setText(savedContainer);
                        } else if (!TextUtils.isEmpty(prefContainer)) {
                            etContainerKg.setText(prefContainer);
                        } else if (!TextUtils.isEmpty(bundleContainer)) {
                            etContainerKg.setText(bundleContainer);
                        } else {
                            etContainerKg.setText(String.valueOf(defaultContainer));
                        }
                    }

                    // ダンネージ重量：優先順位＝当画面引継 → Preferences → 前画面引継 → DB既定
                    if (etDunnageKg != null) {
                        if (!TextUtils.isEmpty(savedDunnage)) {
                            etDunnageKg.setText(savedDunnage);
                        } else if (!TextUtils.isEmpty(prefDunnage)) {
                            etDunnageKg.setText(prefDunnage);
                        } else if (!TextUtils.isEmpty(bundleDunnage)) {
                            etDunnageKg.setText(bundleDunnage);
                        } else {
                            etDunnageKg.setText(String.valueOf(defaultDunnage));
                        }
                    }

                    // コンテナNo/シールNo復元
                    if (etContainerNo1 != null) etContainerNo1.setText(defaultString(savedNo1));
                    if (etContainerNo2 != null) etContainerNo2.setText(defaultString(savedNo2));
                    if (etSealNo != null) etSealNo.setText(defaultString(savedSeal));

                    // 写真URI復元（表示＋内部保持）
                    restorePhoto(ivPhotoContainer, savedContainerPhoto, true);
                    restorePhoto(ivPhotoSeal, savedSealPhoto, false);

                    // 束数表示（個数）
                    if (tvBundleCount != null) {
                        tvBundleCount.setText(formatNumber(bundleCount));
                    }

                    // ※元コード踏襲：tvBansenKgにもbundleCountをセットしている（表示仕様に依存）
                    if (tvBansenKg != null) {
                        tvBansenKg.setText(formatNumber(bundleCount));
                    }

                    // 予約No.表示
                    if (etBookingNo != null) {
                        etBookingNo.setText(bookingNo);
                    }

                    // チェックデジット更新
                    updateCheckDigit();

                    // 総重量/残重量計算
                    calcJyuryo();

                    // 初期フォーカス
                    if (etContainerNo1 != null) etContainerNo1.requestFocus();
                });
            } catch (Exception ex) {
                // 例外時は共通エラー処理へ
                runOnUiThread(() -> errorProcess("ContainerInput loadInitialData", ex));
            }
        });
    }

    //============================================================
    //　機　能　:　入力フォームを初期化する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void initForm() {
        // 入力欄初期化
        if (etContainerNo1 != null) etContainerNo1.setText("");
        if (etContainerNo2 != null) etContainerNo2.setText("");
        if (etSealNo != null) etSealNo.setText("");
        if (etBookingNo != null) etBookingNo.setText("");

        // 表示欄初期化
        if (tvCheckDigit != null) tvCheckDigit.setText("");
        if (tvBundleCount != null) tvBundleCount.setText("");
        if (tvBansenKg != null) tvBansenKg.setText("");
        if (tvTotalKg != null) tvTotalKg.setText("");
        if (tvRemainKg != null) tvRemainKg.setText("");

        // プレビュー初期化
        if (ivPhotoContainer != null) ivPhotoContainer.setImageDrawable(null);
        if (ivPhotoSeal != null) ivPhotoSeal.setImageDrawable(null);
    }

    //============================================================
    //　機　能　:　引き継ぎ値を読み込む
    //　引　数　:　intent ..... 画面遷移情報
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void loadPassedValues(@Nullable Intent intent) {
        if (intent == null) return;

        // 1) 束側の引継Mapを取得（Serializable想定）
        java.io.Serializable bundleExtra = intent.getSerializableExtra(EXTRA_BUNDLE_VALUES);
        if (bundleExtra instanceof java.util.Map) {
            bundleValues.clear();
            java.util.Map<?, ?> raw = (java.util.Map<?, ?>) bundleExtra;

            // 型が不定なのでStringへ変換しつつ格納
            for (java.util.Map.Entry<?, ?> entry : raw.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    bundleValues.put(key.toString(), value.toString());
                }
            }
        }

        // 2) 当画面（コンテナ側）の引継Mapを取得
        java.io.Serializable containerExtra = intent.getSerializableExtra(EXTRA_CONTAINER_VALUES);
        if (containerExtra instanceof java.util.Map) {
            containerValues.clear();
            java.util.Map<?, ?> raw = (java.util.Map<?, ?>) containerExtra;

            for (java.util.Map.Entry<?, ?> entry : raw.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    containerValues.put(key.toString(), value.toString());
                }
            }
        }
    }

    /**
     * コンテナNo入力監視（チェックデジット更新）。
     */
    private final TextWatcher containerNoWatcher = new TextWatcher() {
        //============================================================
        //　機　能　:　テキスト変更前の処理を行う
        //　引　数　:　s ..... 文字列
        //　　　　　:　start ..... 開始位置
        //　　　　　:　count ..... 件数
        //　　　　　:　after ..... 変更後文字数
        //　戻り値　:　[void] ..... なし
        //============================================================
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // 事前処理なし
        }

        //============================================================
        //　機　能　:　テキスト変更中の処理を行う
        //　引　数　:　s ..... 文字列
        //　　　　　:　start ..... 開始位置
        //　　　　　:　before ..... 変更前文字数
        //　　　　　:　count ..... 件数
        //　戻り値　:　[void] ..... なし
        //============================================================
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // 途中処理なし
        }

        //============================================================
        //　機　能　:　テキスト変更後の処理を行う
        //　引　数　:　s ..... 文字列
        //　戻り値　:　[void] ..... なし
        //============================================================
        @Override
        public void afterTextChanged(Editable s) {
            // 入力確定後にチェックデジット再計算
            updateCheckDigit();
        }
    };

    //============================================================
    //　機　能　:　チェックデジット表示を更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void updateCheckDigit() {
        if (etContainerNo1 == null || etContainerNo2 == null || tvCheckDigit == null) return;

        // 入力値を取得
        String no1 = safeText(etContainerNo1).trim();
        String no2 = safeText(etContainerNo2).trim();

        // 期待フォーマット：先頭3桁＋末尾6桁（間の"U"は固定）
        if (no1.length() == 3 && no2.length() == 6) {
            String containerNo = no1 + "U" + no2;

            // 共通関数でチェックデジットを算出し表示
            tvCheckDigit.setText(HandyUtil.calcCheckDigit(containerNo));
        } else {
            // 条件未達なら表示クリア
            tvCheckDigit.setText("");
        }
    }

    /**
     * 重量入力監視（総重量/残重量の再計算＋Preferences保存）。
     */
    private final TextWatcher weightWatcher = new TextWatcher() {
        //============================================================
        //　機　能　:　テキスト変更前の処理を行う
        //　引　数　:　s ..... 文字列
        //　　　　　:　start ..... 開始位置
        //　　　　　:　count ..... 件数
        //　　　　　:　after ..... 変更後文字数
        //　戻り値　:　[void] ..... なし
        //============================================================
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // 事前処理なし
        }

        //============================================================
        //　機　能　:　テキスト変更中の処理を行う
        //　引　数　:　s ..... 文字列
        //　　　　　:　start ..... 開始位置
        //　　　　　:　before ..... 変更前文字数
        //　　　　　:　count ..... 件数
        //　戻り値　:　[void] ..... なし
        //============================================================
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // 途中処理なし
        }

        //============================================================
        //　機　能　:　テキスト変更後の処理を行う
        //　引　数　:　s ..... 文字列
        //　戻り値　:　[void] ..... なし
        //============================================================
        @Override
        public void afterTextChanged(Editable s) {
            // 重量変更 → 合計/残量を即時計算
            calcJyuryo();

            // 入力した重量を端末に保存（次回の初期値として使用）
            persistContainerWeights();
        }
    };

    //============================================================
    //　機　能　:　コンテナ重量を保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void persistContainerWeights() {
        // Preferencesへ保存（次回起動時の初期値に利用）
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        String container = safeText(etContainerKg).trim();
        String dunnage = safeText(etDunnageKg).trim();

        prefs.edit()
                .putString(PREFS_CONTAINER_JYURYO, container)
                .putString(PREFS_DUNNAGE_JYURYO, dunnage)
                .apply();
    }

    //============================================================
    //　機　能　:　重量を計算する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void calcJyuryo() {
        // 入力重量を取得（未入力/不正は0）
        int container = getIntFromEdit(etContainerKg);
        int dunnage = getIntFromEdit(etDunnageKg);

        // 総重量＝コンテナ自重＋ダンネージ＋積載束重量＋束数（※仕様通り加算）
        int total = container + dunnage + sekisaiSokuJyuryo + bundleCount;

        // 総重量表示
        if (tvTotalKg != null) tvTotalKg.setText(formatNumber(total));

        // 残重量＝最大積載可能 - 総重量
        int remaining = maxContainerJyuryo - total;

        // 残重量表示
        if (tvRemainKg != null) tvRemainKg.setText(formatNumber(remaining));
    }

    //============================================================
    //　機　能　:　カメラを起動する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @ExperimentalCamera2Interop
    private void launchCamera() {
        // カメラ権限チェック
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 権限未許可 → 権限要求
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        // 権限OK → カメラ起動
        launchCameraInternal();
    }

    //============================================================
    //　機　能　:　内部処理でカメラを起動する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @ExperimentalCamera2Interop
    private void launchCameraInternal() {
        // 撮影画面へ遷移し、ターゲット（コンテナ/シール）を引き渡す
        Intent intent = new Intent(this, PhotographingActivity.class);
        intent.putExtra(PhotographingActivity.EXTRA_TARGET,
                (currentTarget == PhotoTarget.CONTAINER) ? "CONTAINER" : "SEAL");
        cameraLauncher.launch(intent);
    }

    //============================================================
    //　機　能　:　撮影画像のプレビューを表示する
    //　引　数　:　source ..... 呼び出し元
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void showPreview(ImageView source) {
        // 画像が無い場合は何もしない
        if (source == null || source.getDrawable() == null) {
            return;
        }

        // プレビューダイアログ用レイアウトを生成
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageView preview = dialogView.findViewById(R.id.ivPreviewImage);
        Button btnClose = dialogView.findViewById(R.id.btnPreviewClose);

        // 表示対象のDrawableをそのままセット
        preview.setImageDrawable(source.getDrawable());

        // ダイアログ生成（閉じるボタンでのみ閉じる）
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    //============================================================
    //　機　能　:　写真表示を復元する
    //　引　数　:　target ..... 対象ビュー
    //　　　　　:　uriString ..... URI文字列
    //　　　　　:　isContainer ..... 真偽値フラグ
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void restorePhoto(ImageView target, String uriString, boolean isContainer) {
        // 表示先が無い、またはURIが空なら何もしない
        if (target == null || TextUtils.isEmpty(uriString)) {
            return;
        }

        // ContentResolverでURIを開けるかチェックしつつ表示
        try (InputStream stream = getContentResolver().openInputStream(Uri.parse(uriString))) {
            if (stream == null) return;

            Uri uri = Uri.parse(uriString);

            // 同URI再描画対策
            target.setImageURI(null);
            target.setImageURI(uri);

            // 内部保持（確認ダイアログ等で使用）
            if (isContainer) {
                containerPhotoUri = uri;
            } else {
                sealPhotoUri = uri;
            }
        } catch (IOException | SecurityException | IllegalArgumentException ex) {
            // URIが無効/権限不可などの場合は復元できないためログのみ
            Log.w(TAG, "restorePhoto failed: " + uriString, ex);
        }
    }

    //============================================================
    //　機　能　:　青ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionBlue() {
        // 1) 入力チェック（必須/桁数/重量超過など）
        if (!validateRequiredFields()) return;

        // 2) コンテナ写真が必要条件に対して未撮影なら確認ダイアログ
        if (needsContainerPhotoConfirm()) {
            showQuestion("コンテナ写真が撮影されていません。写真撮影せずに登録してもよろしいですか？", yes -> {
                if (!yes) return;

                // コンテナ写真OK → 次にシール写真確認へ
                confirmSealThenRegister();
            });
            return;
        }

        // 3) コンテナ写真OK → 次にシール写真確認へ
        confirmSealThenRegister();
    }

    //============================================================
    //　機　能　:　シール写真確認後に登録する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void confirmSealThenRegister() {
        // シール写真が必要条件に対して未撮影なら確認ダイアログ
        if (needsSealPhotoConfirm()) {
            showQuestion("シール写真が撮影されていません。写真撮影せずに登録してもよろしいですか？", yes -> {
                if (!yes) return;

                // シール写真OK → 登録へ
                procRegister();
            });
            return;
        }

        // シール写真OK → 登録へ
        procRegister();
    }

    //============================================================
    //　機　能　:　黄ボタン押下時の処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    protected void onFunctionYellow() {
        // 終了（finish()がオーバーライドされており、引継値保存も行う）
        finish();
    }

    //============================================================
    //　機　能　:　必須項目をチェックする
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 入力OK:true、NG:false
    //============================================================
    private boolean validateRequiredFields() {
        // コンテナNoは入力がある場合のみ桁数チェックを行う
        String containerNo = safeText(etContainerNo1).trim() + safeText(etContainerNo2).trim();
        if (!TextUtils.isEmpty(containerNo)
                && (safeText(etContainerNo1).trim().length() != 3 || safeText(etContainerNo2).trim().length() != 6)) {
            showErrorMsg("コンテナNoの入力が正しくありません", MsgDispMode.MsgBox);
            return false;
        }

        // コンテナ自重必須
        if (getIntFromEdit(etContainerKg) <= 0) {
            showErrorMsg("コンテナ自重が未入力です", MsgDispMode.Label);
            if (etContainerKg != null) etContainerKg.requestFocus();
            return false;
        }

        // ダンネージ重量必須
        if (getIntFromEdit(etDunnageKg) <= 0) {
            showErrorMsg("ダンネージ重量が未入力です", MsgDispMode.Label);
            if (etDunnageKg != null) etDunnageKg.requestFocus();
            return false;
        }

        // ダンネージ上限（WEB DB側がtinyint想定のため255まで）
        if (getIntFromEdit(etDunnageKg) >= 256) {
            // WEBのDB（T_SYUKKA_CONTAINER.DUNNAGE_JYURYO）の型がtinyint型のため、0~255までしか登録不可
            showErrorMsg("ダンネージ重量は255Kg以下で入力してください", MsgDispMode.Label);
            if (etDunnageKg != null) etDunnageKg.requestFocus();
            return false;
        }

        // 最大積載重量超過チェック
        if (getRemainingWeight() < 0) {
            showErrorMsg("積載重量が超過しています", MsgDispMode.MsgBox);
            return false;
        }

        return true;
    }

    //============================================================
    //　機　能　:　残重量を取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 残重量
    //============================================================
    private int getRemainingWeight() {
        // 現在の入力値から総重量を算出し、最大積載との差分を返す
        int container = getIntFromEdit(etContainerKg);
        int dunnage = getIntFromEdit(etDunnageKg);
        int total = container + dunnage + sekisaiSokuJyuryo + bundleCount;
        return maxContainerJyuryo - total;
    }

    //============================================================
    //　機　能　:　コンテナ写真確認が必要か判定する
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 確認が必要ならtrue
    //============================================================
    private boolean needsContainerPhotoConfirm() {
        // コンテナNo未入力かつ写真も無い場合は確認対象
        String containerNo = safeText(etContainerNo1).trim() + safeText(etContainerNo2).trim();
        return TextUtils.isEmpty(containerNo) && !hasImage(ivPhotoContainer, containerPhotoUri);
    }

    //============================================================
    //　機　能　:　シール写真確認が必要か判定する
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 確認が必要ならtrue
    //============================================================
    private boolean needsSealPhotoConfirm() {
        // シールNo未入力かつ写真も無い場合は確認対象
        String sealNo = safeText(etSealNo).trim();
        return TextUtils.isEmpty(sealNo) && !hasImage(ivPhotoSeal, sealPhotoUri);
    }

    //============================================================
    //　機　能　:　画像の有無を判定する
    //　引　数　:　view ..... ビュー
    //　　　　　:　uri ..... URI情報
    //　戻り値　:　[boolean] ..... 画像があればtrue
    //============================================================
    private boolean hasImage(ImageView view, Uri uri) {
        // URIが保持されている、またはImageViewにDrawableがあれば画像ありとみなす
        return uri != null || (view != null && view.getDrawable() != null);
    }

    //============================================================
    //　機　能　:　登録処理を行う
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void procRegister() {
        // ローディング表示
        showLoadingShort();

        // DB登録/画像保存/送信はバックグラウンドで実行
        io.execute(() -> {
            try {
                // 1) DB登録（コンテナID採番含む）
                int containerId = registerDb();

                // 2) 画像ファイル保存（撮影済みの場合のみ）
                saveImageFile(containerId, DataSync.ImageType.CONTAINER, containerPhotoUri);
                saveImageFile(containerId, DataSync.ImageType.SEAL, sealPhotoUri);

                // 3) 送信処理（出荷のみ送信）
                DataSync sync = new DataSync(getApplicationContext());
                boolean sent = sync.sendSyukkaOnly();
                String errorMessage = sync.getLastErrorMessage();

                // 4) UIへ結果反映
                runOnUiThread(() -> {
                    hideLoadingShort();

                    if (!sent) {
                        // 送信失敗：詳細メッセージを組み立てて表示
                        String msg = buildSendFailedMessage(errorMessage);
                        showCompleteFlow(msg);
                        return;
                    }

                    // 送信成功：完了メッセージ→終了
                    showCompleteFlow(null);
                });
            } catch (Exception ex) {
                // 想定外例外は共通エラー処理へ
                runOnUiThread(() -> errorProcess("ContainerInput procRegister", ex));
            }
        });
    }

    /**
     * 完了フロー表示（送信失敗時はエラーダイアログ→OK後に完了表示へ誘導）。
     */
    private void showCompleteFlow(@Nullable String sendErrorMessage) {
        if (TextUtils.isEmpty(sendErrorMessage)) {
            // エラーなし：完了表示して終了
            showCompleteInfoAndFinish();
            return;
        }

        // エラーあり：ブザー/バイブ→エラー表示
        HandyUtil.playErrorBuzzer(this);
        HandyUtil.playVibrater(this);

        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage(sendErrorMessage)
                .setCancelable(false)
                .setPositiveButton("OK", (d1, w1) -> {
                    d1.dismiss();

                    // ダイアログを閉じた後、完了表示へ（UIスレッドで実行）
                    getWindow().getDecorView().post(this::showCompleteInfoAndFinish);
                })
                .show();
    }

    /**
     * 完了メッセージ表示→OKで画面終了。
     */
    private void showCompleteInfoAndFinish() {
        HandyUtil.playSuccessBuzzer(this);
        HandyUtil.playVibrater(this);

        new AlertDialog.Builder(this)
                .setTitle("情報")
                .setMessage(MSG_CONTAINER_CONFIRMED)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    // 呼び出し元へOKを返す
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    /**
     * 送信失敗時の表示メッセージを整形する。
     * detailが空/同一文の場合はベース文のみ返す。
     */
    private String buildSendFailedMessage(@Nullable String detail) {
        final String base = "出荷データの更新に失敗しました";

        if (TextUtils.isEmpty(detail)) {
            return base;
        }

        String trimmed = detail.trim();
        if (base.equals(trimmed)) {
            return base;
        }

        return base + "\n" + trimmed;
    }

    //============================================================
    //　機　能　:　DBへ登録する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 採番したcontainerId
    //============================================================
    private int registerDb() {
        AtomicInteger newId = new AtomicInteger(0);

        // 1トランザクションでコンテナ登録～関連テーブル更新まで実施
        db.runInTransaction(() -> {
            // 1) 新規containerIdを採番（最大+1）
            Integer maxId = db.syukkaContainerDao().getMaxContainerId();
            int containerId = (maxId == null) ? 1 : maxId + 1;
            newId.set(containerId);

            // 2) 登録用データを組み立て
            String containerNo = buildContainerNo();
            String bookingNo = safeText(etBookingNo).trim();
            String now = DateTimeFormatUtil.nowDbYmdHms();

            SyukkaContainerEntity entity = new SyukkaContainerEntity();
            entity.containerId = containerId;
            entity.bookingNo = bookingNo;
            entity.containerNo = containerNo;
            entity.containerJyuryo = getIntFromEdit(etContainerKg);
            entity.dunnageJyuryo = getIntFromEdit(etDunnageKg);
            entity.sealNo = safeText(etSealNo).trim();
            entity.containerSize = resolveContainerSize();
            entity.insertProcName = "ContainerInput";
            entity.insertYmd = now;
            entity.updateProcName = "ContainerInput";
            entity.updateYmd = now;

            // 3) コンテナ情報をUpsert
            db.syukkaContainerDao().upsert(entity);

            // 4) 作業中明細へcontainerIdを設定（紐付け）
            db.syukkaMeisaiDao().updateContainerIdForWork(containerId);

            // 5) 予約No.がある場合は完了数/重量を加算
            if (!TextUtils.isEmpty(bookingNo)) {
                db.yoteiDao().incrementKanryo(bookingNo, bundleCount, sekisaiSokuJyuryo);
            }

            // 6) 作業中明細をクリア（登録済みとして扱う）
            db.syukkaMeisaiWorkDao().deleteAll();
        });

        return newId.get();
    }

    //============================================================
    //　機　能　:　コンテナ番号を生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... コンテナNo（チェックデジット付、未入力なら空）
    //============================================================
    private String buildContainerNo() {
        // 先頭3桁/末尾6桁を取得
        String no1 = safeText(etContainerNo1).trim();
        String no2 = safeText(etContainerNo2).trim();

        // 先頭が未入力ならコンテナNoなし扱い
        if (TextUtils.isEmpty(no1)) {
            return "";
        }

        // ベース（XXXUYYYYYY）を生成しチェックデジット付与
        String base = no1 + "U" + no2;
        String checkDigit = HandyUtil.calcCheckDigit(base);
        return base + checkDigit;
    }

    //============================================================
    //　機　能　:　コンテナサイズを決定する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 20 or 40
    //============================================================
    private int resolveContainerSize() {
        // Preferencesからサイズを取得（既定は20ft）
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 40 : 20;
    }

    //============================================================
    //　機　能　:　画像ファイルを保存する
    //　引　数　:　containerId ..... ID
    //　　　　　:　type ..... 種別
    //　　　　　:　uri ..... URI情報
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void saveImageFile(int containerId, DataSync.ImageType type, Uri uri) throws IOException {
        // 未撮影なら保存不要
        if (uri == null) {
            return;
        }

        // 保存先ファイルを取得
        File file = getImageFile(containerId, type);
        if (file == null) {
            return;
        }

        // URI→ファイルへコピー
        ContentResolver resolver = getContentResolver();
        try (InputStream input = resolver.openInputStream(uri);
             FileOutputStream output = new FileOutputStream(file)) {

            if (input == null) return;

            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    //============================================================
    //　機　能　:　画像ファイルを取得する
    //　引　数　:　containerId ..... ID
    //　　　　　:　type ..... 種別
    //　戻り値　:　[File] ..... 保存先ファイル
    //============================================================
    private File getImageFile(int containerId, DataSync.ImageType type) {
        // 外部領域（Pictures）を優先し、取れなければ内部領域へ
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            dir = getFilesDir();
        }

        // ファイル名：container_{id}_{suffix}.jpg
        String name = "container_" + containerId + "_" + type.getSuffix() + ".jpg";
        return new File(dir, name);
    }


    //============================================================
    //　機　能　:　既定のコンテナ重量を決定する
    //　引　数　:　system ..... システム設定情報
    //　戻り値　:　[int] ..... 既定コンテナ自重（未設定は0）
    //============================================================
    private int resolveDefaultContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultContainerJyuryo != null) {
            return system.defaultContainerJyuryo;
        }
        return 0;
    }

    //============================================================
    //　機　能　:　既定のダンネージ重量を決定する
    //　引　数　:　system ..... システム設定情報
    //　戻り値　:　[int] ..... 既定ダンネージ重量（未設定は0）
    //============================================================
    private int resolveDefaultDunnageWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultDunnageJyuryo != null) {
            return system.defaultDunnageJyuryo;
        }
        return 0;
    }

    //============================================================
    //　機　能　:　コンテナ最大重量を決定する
    //　引　数　:　system ..... システム設定情報
    //　戻り値　:　[int] ..... 最大積載重量
    //============================================================
    private int resolveMaxContainerWeight(@Nullable SystemEntity system) {
        // DB設定があり、かつ正の値ならそれを採用
        if (system != null && system.maxContainerJyuryo != null && system.maxContainerJyuryo > 0) {
            return system.maxContainerJyuryo;
        }

        // 無ければPreferencesのサイズから推定（20ft:24000, 40ft:30000）
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 30000 : 24000;
    }

    //============================================================
    //　機　能　:　null安全な整数へ変換する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[int] ..... nullなら0
    //============================================================
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    //============================================================
    //　機　能　:　入力欄から整数値を取得する
    //　引　数　:　editText ..... テキスト
    //　戻り値　:　[int] ..... 数値（不正/未入力は0）
    //============================================================
    private int getIntFromEdit(EditText editText) {
        if (editText == null) return 0;

        // 文字列取得
        String raw = safeText(editText);
        if (TextUtils.isEmpty(raw)) return 0;

        // カンマ除去＋トリム
        String cleaned = raw.replace(",", "").trim();
        if (TextUtils.isEmpty(cleaned)) return 0;

        // 数値変換（失敗時は0）
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    //============================================================
    //　機　能　:　null安全な文字列を取得する
    //　引　数　:　editText ..... テキスト
    //　戻り値　:　[String] ..... null安全な文字列
    //============================================================
    private String safeText(EditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString();
    }

    //============================================================
    //　機　能　:　既定値付き文字列を取得する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... nullなら空文字
    //============================================================
    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    //============================================================
    //　機　能　:　数値文字列を整形する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... カンマ区切り文字列
    //============================================================
    private String formatNumber(int value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    //============================================================
    //　機　能　:　短時間メッセージを表示
    //　引　数　:　msg ..... メッセージ
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //============================================================
    //　機　能　:　画面を終了する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    public void finish() {
        // 終了時に入力値をMapへ保存（次画面/再表示の引継用）
        saveContainerValues();

        // 呼び出し元へ引継Mapを返却
        Intent result = new Intent();
        result.putExtra(EXTRA_CONTAINER_VALUES, new java.util.HashMap<>(containerValues));
        setResult(RESULT_OK, result);

        super.finish();
    }

    //============================================================
    //　機　能　:　コンテナ入力値を保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void saveContainerValues() {
        // 入力値を引継用Mapへ保存
        containerValues.put(KEY_CONTAINER_JYURYO, safeText(etContainerKg).trim());
        containerValues.put(KEY_DUNNAGE_JYURYO, safeText(etDunnageKg).trim());
        containerValues.put(KEY_CONTAINER_NO1, safeText(etContainerNo1).trim());
        containerValues.put(KEY_CONTAINER_NO2, safeText(etContainerNo2).trim());
        containerValues.put(KEY_SEAL_NO, safeText(etSealNo).trim());

        // 写真URI（未撮影は空文字）
        containerValues.put(KEY_CONTAINER_PHOTO_URI,
                containerPhotoUri != null ? containerPhotoUri.toString() : "");
        containerValues.put(KEY_SEAL_PHOTO_URI,
                sealPhotoUri != null ? sealPhotoUri.toString() : "");
    }
}
