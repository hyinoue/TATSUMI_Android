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
import androidx.core.content.ContextCompat;

import com.densowave.bhtsdk.hardkeyboardsettings.INPUT_MODE;
import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.SyukkaContainerEntity;
import com.example.myapplication.settings.HandyUtil;
import com.example.myapplication.settings.InputConstraintUtil;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


//=======================================
//　処理概要　:　ContainerInputActivityクラス
//=======================================

/**
 * コンテナ情報入力画面のActivity。
 *
 * <p>コンテナ番号/シール番号/重量などの入力に加えて、
 * コンテナ・シールの写真撮影を行い、結果をDB/画面に反映する。</p>
 *
 * <p>主な処理フロー:</p>
 * <ul>
 *     <li>前画面から渡された束情報/重量情報を受け取り、残量を表示。</li>
 *     <li>カメラ起動 → 撮影結果URIを保持し、ImageViewにプレビュー表示。</li>
 *     <li>確定時に入力値を保存し、結果を呼び出し元へ返却。</li>
 * </ul>
 */
public class ContainerInputActivity extends BaseActivity {

    private static final String TAG = "ContainerInput";
    private static final int SYSTEM_RENBAN = 1;

    public static final String EXTRA_BUNDLE_VALUES = "container_input_bundle_values";
    public static final String EXTRA_CONTAINER_VALUES = "container_input_values";

    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo";
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";
    private static final String PREFS_CONTAINER_JYURYO = "prefs_container_jyuryo";
    private static final String PREFS_DUNNAGE_JYURYO = "prefs_dunnage_jyuryo";
    private static final String KEY_CONTAINER_NO1 = "container_no1";
    private static final String KEY_CONTAINER_NO2 = "container_no2";
    private static final String KEY_SEAL_NO = "seal_no";
    private static final String KEY_CONTAINER_PHOTO_URI = "container_photo_uri";
    private static final String KEY_SEAL_PHOTO_URI = "seal_photo_uri";

    private Button btnPhotoContainerNo;
    private Button btnPhotoSealNo;

    private ImageView ivPhotoContainer;
    private ImageView ivPhotoSeal;

    private EditText etContainerNo1;
    private EditText etContainerNo2;
    private EditText etContainerKg;
    private EditText etDunnageKg;
    private EditText etSealNo;
    private EditText etBookingNo;

    private TextView tvCheckDigit;
    private TextView tvBansenKg;
    private TextView tvBundleCount;
    private TextView tvTotalKg;
    private TextView tvRemainKg;

    private enum PhotoTarget {CONTAINER, SEAL}

    private PhotoTarget currentTarget = PhotoTarget.CONTAINER;

    private ExecutorService io;
    private AppDatabase db;

    private final java.util.Map<String, String> bundleValues = new java.util.HashMap<>();
    private final java.util.Map<String, String> containerValues = new java.util.HashMap<>();

    private int bundleCount = 0;        // 束数（個数）
    private int sekisaiSokuJyuryo = 0;  // 積載束重量（kg）
    private int maxContainerJyuryo = 0; // 最大積載可能重量（kg）

    private Uri containerPhotoUri;
    private Uri sealPhotoUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    // 撮影キャンセル/戻り時はそのまま終了
                    toast("撮影キャンセル");
                    Log.i(TAG, "Camera canceled");
                    return;
                }

                // 撮影結果のURIとターゲット(コンテナ/シール)を取得
                String uriStr = result.getData().getStringExtra(PhotographingActivity.EXTRA_RESULT_URI);
                String target = result.getData().getStringExtra(PhotographingActivity.EXTRA_TARGET);

                if (uriStr == null) {
                    // 取得失敗時はエラーメッセージのみ表示
                    toast("画像URIが取得できませんでした");
                    Log.e(TAG, "result uri is null");
                    return;
                }

                Uri uri = Uri.parse(uriStr);

                if ("CONTAINER".equals(target)) {
                    // コンテナ写真をプレビューに表示
                    containerPhotoUri = uri;
                    ivPhotoContainer.setImageURI(null); // 同URI再描画対策
                    ivPhotoContainer.setImageURI(uri);
                    toast("コンテナNo写真を表示しました");
                } else {
                    // シール写真をプレビューに表示
                    sealPhotoUri = uri;
                    ivPhotoSeal.setImageURI(null);
                    ivPhotoSeal.setImageURI(uri);
                    toast("シールNo写真を表示しした");
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCameraInternal();
                } else {
                    toast("カメラ権限が拒否されています");
                }
            });

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_container_input);

        io = Executors.newSingleThreadExecutor();
        db = AppDatabase.getInstance(getApplicationContext());

        bindViews();
        setupBottomButtons();
        setupInputHandlers();
        setupPhotoHandlers();
        loadPassedValues(getIntent());

        loadInitialData();
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
    //============================
    //　機　能　:　bind Viewsの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void bindViews() {
        btnPhotoContainerNo = findViewById(R.id.btnPhotoContainerNo);
        btnPhotoSealNo = findViewById(R.id.btnPhotoSealNo);
        ivPhotoContainer = findViewById(R.id.ivPhotoContainer);
        ivPhotoSeal = findViewById(R.id.ivPhotoSeal);
        etContainerNo1 = findViewById(R.id.etContainerNo1);
        etContainerNo2 = findViewById(R.id.etContainerNo2);
        etContainerKg = findViewById(R.id.etContainerKg);
        etDunnageKg = findViewById(R.id.etDunnageKg);
        etSealNo = findViewById(R.id.etSealNo);
        etBookingNo = findViewById(R.id.etYoyakuNo);
        tvCheckDigit = findViewById(R.id.tvCheckDigit);
        tvBansenKg = findViewById(R.id.tvBansenKg);
        tvBundleCount = findViewById(R.id.tvSekisaiBundleCount);
        tvTotalKg = findViewById(R.id.tvTotalKg);
        tvRemainKg = findViewById(R.id.tvRemainKg);
    }
    //================================
    //　機　能　:　bottom Buttonsを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupBottomButtons() {
        MaterialButton btnBlue = findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = findViewById(R.id.btnBottomYellow);

        if (btnBlue != null) btnBlue.setText("確定");
        if (btnRed != null) btnRed.setText("");
        if (btnGreen != null) btnGreen.setText("");
        if (btnYellow != null) btnYellow.setText("終了");
        refreshBottomButtonsEnabled();
    }
    //================================
    //　機　能　:　input Handlersを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupInputHandlers() {
        if (etContainerNo1 != null) etContainerNo1.addTextChangedListener(containerNoWatcher);
        if (etContainerNo2 != null) etContainerNo2.addTextChangedListener(containerNoWatcher);
        if (etContainerKg != null) etContainerKg.addTextChangedListener(weightWatcher);
        if (etDunnageKg != null) etDunnageKg.addTextChangedListener(weightWatcher);

        setupEnterFocus(etContainerNo1, etContainerNo2);
        setupEnterFocus(etContainerNo2, etContainerKg);
        setupEnterFocus(etContainerKg, etDunnageKg);
        setupEnterFocus(etDunnageKg, etSealNo);
        setupEnterFocus(etSealNo, etContainerNo1);

        if (etBookingNo != null) {
            etBookingNo.setFocusable(false);
            etBookingNo.setFocusableInTouchMode(false);
        }

        applyContainerNoInputMode();
    }

    //入力モード
    private void applyContainerNoInputMode() {
        // No1: 英字のみ（小文字入力は大文字に変換）
        InputConstraintUtil.applyAlphabetUpper(etContainerNo1);
        InputConstraintUtil.applyHardKeyboardModeOnFocus(this, etContainerNo1, INPUT_MODE.MODE_ALPHA);

        // No2: 数字のみ
        InputConstraintUtil.applyDigitsOnly(etContainerNo2);
        InputConstraintUtil.applyHardKeyboardModeOnFocus(this, etContainerNo2, INPUT_MODE.MODE_NUMERIC);
    }

    //from→toにフォーカスを移動
    private void setupEnterFocus(@Nullable EditText from, @Nullable EditText to) {
        if (from == null) return;

        from.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (to != null) {
                    to.requestFocus();
                } else {
                    from.requestFocus();
                }
            }
            return true;
        });

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

    private boolean isEnterAction(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }
    //================================
    //　機　能　:　photo Handlersを設定する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================

    private void setupPhotoHandlers() {
        if (btnPhotoContainerNo != null) {
            btnPhotoContainerNo.setFocusable(false);
            btnPhotoContainerNo.setFocusableInTouchMode(false);
            btnPhotoContainerNo.setOnClickListener(v -> {
                currentTarget = PhotoTarget.CONTAINER;
                launchCamera();
            });
        }

        if (btnPhotoSealNo != null) {
            btnPhotoSealNo.setFocusable(false);
            btnPhotoSealNo.setFocusableInTouchMode(false);
            btnPhotoSealNo.setOnClickListener(v -> {
                currentTarget = PhotoTarget.SEAL;
                launchCamera();
            });
        }

        if (ivPhotoContainer != null) {
            ivPhotoContainer.setOnClickListener(v -> showPreview(ivPhotoContainer));
        }
        if (ivPhotoSeal != null) {
            ivPhotoSeal.setOnClickListener(v -> showPreview(ivPhotoSeal));
        }
    }
    //==============================
    //　機　能　:　initial Dataを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================

    private void loadInitialData() {
        initForm();

        io.execute(() -> {
            try {
                SystemEntity system = db.systemDao().findById(SYSTEM_RENBAN);
                SyukkaMeisaiWorkDao.WorkSummary summary = db.syukkaMeisaiWorkDao().getWorkSummary();

                int defaultContainer = resolveDefaultContainerWeight(system);
                int defaultDunnage = resolveDefaultDunnageWeight(system);
                maxContainerJyuryo = resolveMaxContainerWeight(system);

                bundleCount = summary != null ? summary.sokusu : 0;
                sekisaiSokuJyuryo = summary != null ? safeInt(summary.jyuryo) : 0;
                String bookingNo = summary != null ? summary.bookingNo : "";

                String savedContainer = containerValues.get(KEY_CONTAINER_JYURYO);
                String savedDunnage = containerValues.get(KEY_DUNNAGE_JYURYO);
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                String prefContainer = prefs.getString(PREFS_CONTAINER_JYURYO, "");
                String prefDunnage = prefs.getString(PREFS_DUNNAGE_JYURYO, "");
                String savedNo1 = containerValues.get(KEY_CONTAINER_NO1);
                String savedNo2 = containerValues.get(KEY_CONTAINER_NO2);
                String savedSeal = containerValues.get(KEY_SEAL_NO);
                String savedContainerPhoto = containerValues.get(KEY_CONTAINER_PHOTO_URI);
                String savedSealPhoto = containerValues.get(KEY_SEAL_PHOTO_URI);

                runOnUiThread(() -> {
                    String bundleContainer = bundleValues.get(KEY_CONTAINER_JYURYO);
                    String bundleDunnage = bundleValues.get(KEY_DUNNAGE_JYURYO);
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

                    if (etContainerNo1 != null) etContainerNo1.setText(defaultString(savedNo1));
                    if (etContainerNo2 != null) etContainerNo2.setText(defaultString(savedNo2));
                    if (etSealNo != null) etSealNo.setText(defaultString(savedSeal));
                    restorePhoto(ivPhotoContainer, savedContainerPhoto, true);
                    restorePhoto(ivPhotoSeal, savedSealPhoto, false);

                    if (tvBundleCount != null) {
                        tvBundleCount.setText(formatNumber(bundleCount));         // 個数
                    }
                    if (tvBansenKg != null) {
                        tvBansenKg.setText(formatNumber(bundleCount));     // kg
                    }
                    if (etBookingNo != null) {
                        etBookingNo.setText(bookingNo);
                    }

                    updateCheckDigit();
                    calcJyuryo();

                    if (etContainerNo1 != null) etContainerNo1.requestFocus();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> errorProcess("ContainerInput loadInitialData", ex));
            }
        });
    }
    //============================
    //　機　能　:　formを初期化する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void initForm() {
        if (etContainerNo1 != null) etContainerNo1.setText("");
        if (etContainerNo2 != null) etContainerNo2.setText("");
        if (etSealNo != null) etSealNo.setText("");
        if (etBookingNo != null) etBookingNo.setText("");
        if (tvCheckDigit != null) tvCheckDigit.setText("");
        if (tvBundleCount != null) tvBundleCount.setText("");
        if (tvBansenKg != null) tvBansenKg.setText("");
        if (tvTotalKg != null) tvTotalKg.setText("");
        if (tvRemainKg != null) tvRemainKg.setText("");
        if (ivPhotoContainer != null) ivPhotoContainer.setImageDrawable(null);
        if (ivPhotoSeal != null) ivPhotoSeal.setImageDrawable(null);
    }
    //================================
    //　機　能　:　passed Valuesを読み込む
    //　引　数　:　intent ..... Intent
    //　戻り値　:　[void] ..... なし
    //================================

    private void loadPassedValues(@Nullable Intent intent) {
        if (intent == null) return;
        java.io.Serializable bundleExtra = intent.getSerializableExtra(EXTRA_BUNDLE_VALUES);
        if (bundleExtra instanceof java.util.Map) {
            bundleValues.clear();
            java.util.Map<?, ?> raw = (java.util.Map<?, ?>) bundleExtra;
            for (java.util.Map.Entry<?, ?> entry : raw.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    bundleValues.put(key.toString(), value.toString());
                }
            }
        }
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

    private final TextWatcher containerNoWatcher = new TextWatcher() {
        //===================================
        //　機　能　:　before Text Changedの処理
        //　引　数　:　s ..... CharSequence
        //　　　　　:　start ..... int
        //　　　　　:　count ..... int
        //　　　　　:　after ..... int
        //　戻り値　:　[void] ..... なし
        //===================================
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        //=================================
        //　機　能　:　on Text Changedの処理
        //　引　数　:　s ..... CharSequence
        //　　　　　:　start ..... int
        //　　　　　:　before ..... int
        //　　　　　:　count ..... int
        //　戻り値　:　[void] ..... なし
        //=================================
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        //==================================
        //　機　能　:　after Text Changedの処理
        //　引　数　:　s ..... Editable
        //　戻り値　:　[void] ..... なし
        //==================================
        @Override
        public void afterTextChanged(Editable s) {
            updateCheckDigit();
        }
    };
    //=============================
    //　機　能　:　check Digitを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================

    private void updateCheckDigit() {
        if (etContainerNo1 == null || etContainerNo2 == null || tvCheckDigit == null) return;

        String no1 = safeText(etContainerNo1).trim();
        String no2 = safeText(etContainerNo2).trim();
        if (no1.length() == 3 && no2.length() == 6) {
            String containerNo = no1 + "U" + no2;
            tvCheckDigit.setText(HandyUtil.calcCheckDigit(containerNo));
        } else {
            tvCheckDigit.setText("");
        }
    }

    private final TextWatcher weightWatcher = new TextWatcher() {
        //===================================
        //　機　能　:　before Text Changedの処理
        //　引　数　:　s ..... CharSequence
        //　　　　　:　start ..... int
        //　　　　　:　count ..... int
        //　　　　　:　after ..... int
        //　戻り値　:　[void] ..... なし
        //===================================
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        //=================================
        //　機　能　:　on Text Changedの処理
        //　引　数　:　s ..... CharSequence
        //　　　　　:　start ..... int
        //　　　　　:　before ..... int
        //　　　　　:　count ..... int
        //　戻り値　:　[void] ..... なし
        //=================================
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        //==================================
        //　機　能　:　after Text Changedの処理
        //　引　数　:　s ..... Editable
        //　戻り値　:　[void] ..... なし
        //==================================
        @Override
        public void afterTextChanged(Editable s) {
            calcJyuryo();
            persistContainerWeights();
        }
    };
    //=========================================
    //　機　能　:　persist Container Weightsの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================================

    private void persistContainerWeights() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String container = safeText(etContainerKg).trim();
        String dunnage = safeText(etDunnageKg).trim();
        prefs.edit()
                .putString(PREFS_CONTAINER_JYURYO, container)
                .putString(PREFS_DUNNAGE_JYURYO, dunnage)
                .apply();
    }
    //============================
    //　機　能　:　calc Jyuryoの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    private void calcJyuryo() {
        int container = getIntFromEdit(etContainerKg);
        int dunnage = getIntFromEdit(etDunnageKg);

        int total = container + dunnage + sekisaiSokuJyuryo + bundleCount;

        if (tvTotalKg != null) tvTotalKg.setText(formatNumber(total));

        int remaining = maxContainerJyuryo - total;
        if (tvRemainKg != null) tvRemainKg.setText(formatNumber(remaining));
    }
    //=============================
    //　機　能　:　launch Cameraの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCameraInternal();
    }
    //======================================
    //　機　能　:　launch Camera Internalの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================================

    private void launchCameraInternal() {
        Intent intent = new Intent(this, PhotographingActivity.class);
        intent.putExtra(PhotographingActivity.EXTRA_TARGET,
                (currentTarget == PhotoTarget.CONTAINER) ? "CONTAINER" : "SEAL");
        cameraLauncher.launch(intent);
    }
    //===================================
    //　機　能　:　show Previewの処理
    //　引　数　:　source ..... ImageView
    //　戻り値　:　[void] ..... なし
    //===================================

    private void showPreview(ImageView source) {
        if (source == null || source.getDrawable() == null) {
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageView preview = dialogView.findViewById(R.id.ivPreviewImage);
        Button btnClose = dialogView.findViewById(R.id.btnPreviewClose);

        preview.setImageDrawable(source.getDrawable());

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    //======================================
    //　機　能　:　restore Photoの処理
    //　引　数　:　target ..... ImageView
    //　　　　　:　uriString ..... String
    //　　　　　:　isContainer ..... boolean
    //　戻り値　:　[void] ..... なし
    //======================================

    private void restorePhoto(ImageView target, String uriString, boolean isContainer) {
        if (target == null || TextUtils.isEmpty(uriString)) {
            return;
        }
        try (InputStream stream = getContentResolver().openInputStream(Uri.parse(uriString))) {
            if (stream == null) return;
            Uri uri = Uri.parse(uriString);
            target.setImageURI(null);
            target.setImageURI(uri);
            if (isContainer) {
                containerPhotoUri = uri;
            } else {
                sealPhotoUri = uri;
            }
        } catch (IOException | SecurityException | IllegalArgumentException ex) {
            Log.w(TAG, "restorePhoto failed: " + uriString, ex);
        }
    }

    //================================
    //　機　能　:　on Function Blueの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================
    @Override
    protected void onFunctionBlue() {
        if (!validateRequiredFields()) return;

        if (needsContainerPhotoConfirm()) {
            showQuestion("コンテナ写真が撮影されていません。写真撮影せずに登録してもよろしいですか？", yes -> {
                if (!yes) return;
                confirmSealThenRegister();
            });
            return;
        }
        confirmSealThenRegister();
    }
    //==========================================
    //　機　能　:　confirm Seal Then Registerの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==========================================

    private void confirmSealThenRegister() {
        if (needsSealPhotoConfirm()) {
            showQuestion("シール写真が撮影されていません。写真撮影せずに登録してもよろしいですか？", yes -> {
                if (!yes) return;
                procRegister();
            });
            return;
        }
        procRegister();
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
    //========================================
    //　機　能　:　validate Required Fieldsの処理
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //========================================

    private boolean validateRequiredFields() {
        String containerNo = safeText(etContainerNo1).trim() + safeText(etContainerNo2).trim();
        if (!TextUtils.isEmpty(containerNo)
                && (safeText(etContainerNo1).trim().length() != 3 || safeText(etContainerNo2).trim().length() != 6)) {
            showErrorMsg("コンテナNoの入力が正しくありません", MsgDispMode.MsgBox);
            return false;
        }

        if (getIntFromEdit(etContainerKg) <= 0) {
            showErrorMsg("コンテナ自重が未入力です", MsgDispMode.Label);
            if (etContainerKg != null) etContainerKg.requestFocus();
            return false;
        }

        if (getIntFromEdit(etDunnageKg) <= 0) {
            showErrorMsg("ダンネージ重量が未入力です", MsgDispMode.Label);
            if (etDunnageKg != null) etDunnageKg.requestFocus();
            return false;
        }

        if (getRemainingWeight() < 0) {
            showErrorMsg("積載重量が超過しています", MsgDispMode.MsgBox);
            return false;
        }

        return true;
    }
    //==================================
    //　機　能　:　remaining Weightを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... なし
    //==================================

    private int getRemainingWeight() {
        int container = getIntFromEdit(etContainerKg);
        int dunnage = getIntFromEdit(etDunnageKg);
        int total = container + dunnage + sekisaiSokuJyuryo + bundleCount;
        return maxContainerJyuryo - total;
    }
    //=============================================
    //　機　能　:　needs Container Photo Confirmの処理
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //=============================================

    private boolean needsContainerPhotoConfirm() {
        String containerNo = safeText(etContainerNo1).trim() + safeText(etContainerNo2).trim();
        return TextUtils.isEmpty(containerNo) && !hasImage(ivPhotoContainer, containerPhotoUri);
    }
    //========================================
    //　機　能　:　needs Seal Photo Confirmの処理
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //========================================

    private boolean needsSealPhotoConfirm() {
        String sealNo = safeText(etSealNo).trim();
        return TextUtils.isEmpty(sealNo) && !hasImage(ivPhotoSeal, sealPhotoUri);
    }
    //=================================
    //　機　能　:　imageを判定する
    //　引　数　:　view ..... ImageView
    //　　　　　:　uri ..... Uri
    //　戻り値　:　[boolean] ..... なし
    //=================================

    private boolean hasImage(ImageView view, Uri uri) {
        return uri != null || (view != null && view.getDrawable() != null);
    }
    //=============================
    //　機　能　:　proc Registerの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================

    private void procRegister() {
        showLoadingShort();
        io.execute(() -> {
            try {
                int containerId = registerDb();
                saveImageFile(containerId, DataSync.ImageType.CONTAINER, containerPhotoUri);
                saveImageFile(containerId, DataSync.ImageType.SEAL, sealPhotoUri);

                DataSync sync = new DataSync(getApplicationContext());
                sync.sendSyukkaOnly();

                runOnUiThread(() -> {
                    hideLoadingShort();
                    showInfoMsg("コンテナ情報を確定しました", MsgDispMode.MsgBox);
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> errorProcess("ContainerInput procRegister", ex));
            }
        });
    }
    //===========================
    //　機　能　:　register Dbの処理
    //　引　数　:　なし
    //　戻り値　:　[int] ..... なし
    //===========================

    private int registerDb() {
        AtomicInteger newId = new AtomicInteger(0);

        db.runInTransaction(() -> {
            Integer maxId = db.syukkaContainerDao().getMaxContainerId();
            int containerId = (maxId == null) ? 1 : maxId + 1;
            newId.set(containerId);

            String containerNo = buildContainerNo();
            String bookingNo = safeText(etBookingNo).trim();
            String now = nowAsText();

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

            db.syukkaContainerDao().upsert(entity);

            db.syukkaMeisaiDao().updateContainerIdForWork(containerId);

            if (!TextUtils.isEmpty(bookingNo)) {
                db.yoteiDao().incrementKanryo(bookingNo, bundleCount, sekisaiSokuJyuryo);
            }

            db.syukkaMeisaiWorkDao().deleteAll();
        });

        return newId.get();
    }
    //==============================
    //　機　能　:　container Noを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //==============================

    private String buildContainerNo() {
        String no1 = safeText(etContainerNo1).trim();
        String no2 = safeText(etContainerNo2).trim();
        if (TextUtils.isEmpty(no1)) {
            return "";
        }

        String base = no1 + "U" + no2;
        String checkDigit = HandyUtil.calcCheckDigit(base);
        return base + checkDigit;
    }
    //======================================
    //　機　能　:　resolve Container Sizeの処理
    //　引　数　:　なし
    //　戻り値　:　[int] ..... なし
    //======================================

    private int resolveContainerSize() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 40 : 20;
    }
    //==========================================
    //　機　能　:　image Fileを保存する
    //　引　数　:　containerId ..... int
    //　　　　　:　type ..... DataSync.ImageType
    //　　　　　:　uri ..... Uri
    //　戻り値　:　[void] ..... なし
    //==========================================

    private void saveImageFile(int containerId, DataSync.ImageType type, Uri uri) throws IOException {
        if (uri == null) {
            return;
        }

        File file = getImageFile(containerId, type);
        if (file == null) {
            return;
        }

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
    //==========================================
    //　機　能　:　image Fileを取得する
    //　引　数　:　containerId ..... int
    //　　　　　:　type ..... DataSync.ImageType
    //　戻り値　:　[File] ..... なし
    //==========================================

    private File getImageFile(int containerId, DataSync.ImageType type) {
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            dir = getFilesDir();
        }
        String name = "container_" + containerId + "_" + type.getSuffix() + ".jpg";
        return new File(dir, name);
    }
    //==============================
    //　機　能　:　now As Textの処理
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //==============================

    private String nowAsText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(new Date());
    }
    //================================================
    //　機　能　:　resolve Default Container Weightの処理
    //　引　数　:　system ..... SystemEntity
    //　戻り値　:　[int] ..... なし
    //================================================

    private int resolveDefaultContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultContainerJyuryo != null) {
            return system.defaultContainerJyuryo;
        }
        return 0;
    }
    //==============================================
    //　機　能　:　resolve Default Dunnage Weightの処理
    //　引　数　:　system ..... SystemEntity
    //　戻り値　:　[int] ..... なし
    //==============================================

    private int resolveDefaultDunnageWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultDunnageJyuryo != null) {
            return system.defaultDunnageJyuryo;
        }
        return 0;
    }
    //============================================
    //　機　能　:　resolve Max Container Weightの処理
    //　引　数　:　system ..... SystemEntity
    //　戻り値　:　[int] ..... なし
    //============================================

    private int resolveMaxContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.maxContainerJyuryo != null && system.maxContainerJyuryo > 0) {
            return system.maxContainerJyuryo;
        }
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 30000 : 24000;
    }
    //================================
    //　機　能　:　safe Intの処理
    //　引　数　:　value ..... Integer
    //　戻り値　:　[int] ..... なし
    //================================

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
    //====================================
    //　機　能　:　int From Editを取得する
    //　引　数　:　editText ..... EditText
    //　戻り値　:　[int] ..... なし
    //====================================

    private int getIntFromEdit(EditText editText) {
        if (editText == null) return 0;
        String raw = safeText(editText);
        if (TextUtils.isEmpty(raw)) return 0;
        String cleaned = raw.replace(",", "").trim();
        if (TextUtils.isEmpty(cleaned)) return 0;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    //====================================
    //　機　能　:　safe Textの処理
    //　引　数　:　editText ..... EditText
    //　戻り値　:　[String] ..... なし
    //====================================

    private String safeText(EditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString();
    }
    //===============================
    //　機　能　:　default Stringの処理
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... なし
    //===============================

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
    //==============================
    //　機　能　:　numberを整形する
    //　引　数　:　value ..... int
    //　戻り値　:　[String] ..... なし
    //==============================

    private String formatNumber(int value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }
    //=============================
    //　機　能　:　toastの処理
    //　引　数　:　msg ..... String
    //　戻り値　:　[void] ..... なし
    //=============================

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //============================
    //　機　能　:　finishの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    public void finish() {
        saveContainerValues();
        Intent result = new Intent();
        result.putExtra(EXTRA_CONTAINER_VALUES, new java.util.HashMap<>(containerValues));
        setResult(RESULT_OK, result);
        super.finish();
    }
    //==================================
    //　機　能　:　container Valuesを保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================

    private void saveContainerValues() {
        containerValues.put(KEY_CONTAINER_JYURYO, safeText(etContainerKg).trim());
        containerValues.put(KEY_DUNNAGE_JYURYO, safeText(etDunnageKg).trim());
        containerValues.put(KEY_CONTAINER_NO1, safeText(etContainerNo1).trim());
        containerValues.put(KEY_CONTAINER_NO2, safeText(etContainerNo2).trim());
        containerValues.put(KEY_SEAL_NO, safeText(etSealNo).trim());
        containerValues.put(KEY_CONTAINER_PHOTO_URI,
                containerPhotoUri != null ? containerPhotoUri.toString() : "");
        containerValues.put(KEY_SEAL_PHOTO_URI,
                sealPhotoUri != null ? sealPhotoUri.toString() : "");
    }
}
