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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.connector.DataSync;
import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.SyukkaContainerEntity;
import com.example.myapplication.settings.HandyUtil;
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

public class ContainerInputActivity extends BaseActivity {

    private static final String TAG = "ContainerInput";
    private static final int SYSTEM_RENBAN = 1;

    private static final String PREFS_NAME = "container_input";
    private static final String KEY_CONTAINER_JYURYO = "container_jyuryo";
    private static final String KEY_DUNNAGE_JYURYO = "dunnage_jyuryo";
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

    private int bundleCount = 0;        // 束数（個数）
    private int sekisaiSokuJyuryo = 0;  // 積載束重量（kg）
    private int maxContainerJyuryo = 0; // 最大積載可能重量（kg）

    private Uri containerPhotoUri;
    private Uri sealPhotoUri;

    //============================================================
    //　機　能　:　カメラ起動結果（プレビューから戻る）
    //============================================================
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    toast("撮影キャンセル");
                    Log.i(TAG, "Camera canceled");
                    return;
                }

                String uriStr = result.getData().getStringExtra(CameraPreviewActivity.EXTRA_RESULT_URI);
                String target = result.getData().getStringExtra(CameraPreviewActivity.EXTRA_TARGET);

                if (uriStr == null) {
                    toast("画像URIが取得できませんでした");
                    Log.e(TAG, "result uri is null");
                    return;
                }

                Uri uri = Uri.parse(uriStr);

                if ("CONTAINER".equals(target)) {
                    containerPhotoUri = uri;
                    ivPhotoContainer.setImageURI(null); // 同URI再描画対策
                    ivPhotoContainer.setImageURI(uri);
                    toast("コンテナNo写真を表示しました");
                } else {
                    sealPhotoUri = uri;
                    ivPhotoSeal.setImageURI(null);
                    ivPhotoSeal.setImageURI(uri);
                    toast("シールNo写真を表示しした");
                }
            });

    //============================================================
    //　機　能　:　CAMERA権限要求
    //============================================================
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCameraInternal();
                } else {
                    toast("カメラ権限が拒否されています");
                }
            });

    //============================================================
    //　機　能　:　画面生成
    //============================================================
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

        loadInitialData();
    }

    //============================================================
    //　機　能　:　破棄
    //============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (io != null) {
            io.shutdown();
        }
    }

    //============================================================
    //　機　能　:　View紐付け
    //============================================================
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

    //============================================================
    //　機　能　:　4色ボタンの表示
    //============================================================
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

    //============================================================
    //　機　能　:　入力イベント設定
    //============================================================
    private void setupInputHandlers() {
        if (etContainerNo1 != null) etContainerNo1.addTextChangedListener(containerNoWatcher);
        if (etContainerNo2 != null) etContainerNo2.addTextChangedListener(containerNoWatcher);
        if (etContainerKg != null) etContainerKg.addTextChangedListener(weightWatcher);
        if (etDunnageKg != null) etDunnageKg.addTextChangedListener(weightWatcher);

        if (etBookingNo != null) {
            etBookingNo.setFocusable(false);
            etBookingNo.setFocusableInTouchMode(false);
        }
    }

    //============================================================
    //　機　能　:　写真ボタン設定
    //============================================================
    private void setupPhotoHandlers() {
        if (btnPhotoContainerNo != null) {
            btnPhotoContainerNo.setOnClickListener(v -> {
                currentTarget = PhotoTarget.CONTAINER;
                launchCamera();
            });
        }

        if (btnPhotoSealNo != null) {
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

    //============================================================
    //　機　能　:　初期データ読み込み
    //============================================================
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

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String savedContainer = prefs.getString(KEY_CONTAINER_JYURYO, null);
                String savedDunnage = prefs.getString(KEY_DUNNAGE_JYURYO, null);
                String savedNo1 = prefs.getString(KEY_CONTAINER_NO1, "");
                String savedNo2 = prefs.getString(KEY_CONTAINER_NO2, "");
                String savedSeal = prefs.getString(KEY_SEAL_NO, "");
                String savedContainerPhoto = prefs.getString(KEY_CONTAINER_PHOTO_URI, "");
                String savedSealPhoto = prefs.getString(KEY_SEAL_PHOTO_URI, "");

                runOnUiThread(() -> {
                    if (etContainerKg != null) {
                        etContainerKg.setText(!TextUtils.isEmpty(savedContainer)
                                ? savedContainer
                                : String.valueOf(defaultContainer));
                    }
                    if (etDunnageKg != null) {
                        etDunnageKg.setText(!TextUtils.isEmpty(savedDunnage)
                                ? savedDunnage
                                : String.valueOf(defaultDunnage));
                    }

                    if (etContainerNo1 != null) etContainerNo1.setText(savedNo1);
                    if (etContainerNo2 != null) etContainerNo2.setText(savedNo2);
                    if (etSealNo != null) etSealNo.setText(savedSeal);
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

    //============================================================
    //　機　能　:　画面初期化
    //============================================================
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

    //============================================================
    //　機　能　:　コンテナNo入力監視
    //============================================================
    private final TextWatcher containerNoWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateCheckDigit();
        }
    };

    //============================================================
    //　機　能　:　チェックデジット表示更新
    //============================================================
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

    //============================================================
    //　機　能　:　重量入力監視
    //============================================================
    private final TextWatcher weightWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            calcJyuryo();
        }
    };

    //============================================================
    //　機　能　:　総重量＆残重量計算
    //　注　意　:　束数(bundleCount)は“個数”なのでkg計算に加算しない
    //============================================================
    private void calcJyuryo() {
        int container = getIntFromEdit(etContainerKg);
        int dunnage = getIntFromEdit(etDunnageKg);

        int total = container + dunnage + sekisaiSokuJyuryo + bundleCount;

        if (tvTotalKg != null) tvTotalKg.setText(formatNumber(total));

        int remaining = maxContainerJyuryo - total;
        if (tvRemainKg != null) tvRemainKg.setText(formatNumber(remaining));
    }

    //============================================================
    //　機　能　:　カメラ起動（権限チェック）
    //============================================================
    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCameraInternal();
    }

    //============================================================
    //　機　能　:　カメラ起動（内部）
    //============================================================
    private void launchCameraInternal() {
        Intent intent = new Intent(this, CameraPreviewActivity.class);
        intent.putExtra(CameraPreviewActivity.EXTRA_TARGET,
                (currentTarget == PhotoTarget.CONTAINER) ? "CONTAINER" : "SEAL");
        cameraLauncher.launch(intent);
    }

    //============================================================
    //　機　能　:　画像プレビュー表示
    //============================================================
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

    //============================================================
    //　機　能　:　画像復元（URI→ImageView）
    //============================================================
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
        } catch (IOException ex) {
            Log.w(TAG, "restorePhoto failed: " + uriString, ex);
        }
    }

    //============================================================
    //　機　能　:　4色ボタン（青＝確定）
    //============================================================
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

    @Override
    protected void onFunctionYellow() {
        finish();
    }

    //============================================================
    //　機　能　:　必須チェック
    //============================================================
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

    //============================================================
    //　機　能　:　残重量取得
    //============================================================
    private int getRemainingWeight() {
        int container = getIntFromEdit(etContainerKg);
        int dunnage = getIntFromEdit(etDunnageKg);
        int total = container + dunnage + sekisaiSokuJyuryo + bundleCount;
        return maxContainerJyuryo - total;
    }

    private boolean needsContainerPhotoConfirm() {
        String containerNo = safeText(etContainerNo1).trim() + safeText(etContainerNo2).trim();
        return TextUtils.isEmpty(containerNo) && !hasImage(ivPhotoContainer, containerPhotoUri);
    }

    private boolean needsSealPhotoConfirm() {
        String sealNo = safeText(etSealNo).trim();
        return TextUtils.isEmpty(sealNo) && !hasImage(ivPhotoSeal, sealPhotoUri);
    }

    private boolean hasImage(ImageView view, Uri uri) {
        return uri != null || (view != null && view.getDrawable() != null);
    }

    //============================================================
    //　機　能　:　登録処理
    //============================================================
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

    //============================================================
    //　機　能　:　DB登録（コンテナ＋明細更新）
    //============================================================
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

    //============================================================
    //　機　能　:　コンテナNo生成（チェックデジット含む）
    //============================================================
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

    //============================================================
    //　機　能　:　コンテナサイズ取得（20/40）
    //============================================================
    private int resolveContainerSize() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 40 : 20;
    }

    //============================================================
    //　機　能　:　画像保存
    //============================================================
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

    private File getImageFile(int containerId, DataSync.ImageType type) {
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            dir = getFilesDir();
        }
        String name = "container_" + containerId + "_" + type.getSuffix() + ".jpg";
        return new File(dir, name);
    }

    private String nowAsText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(new Date());
    }

    private int resolveDefaultContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultContainerJyuryo != null) {
            return system.defaultContainerJyuryo;
        }
        return 0;
    }

    private int resolveDefaultDunnageWeight(@Nullable SystemEntity system) {
        if (system != null && system.defaultDunnageJyuryo != null) {
            return system.defaultDunnageJyuryo;
        }
        return 0;
    }

    private int resolveMaxContainerWeight(@Nullable SystemEntity system) {
        if (system != null && system.maxContainerJyuryo != null && system.maxContainerJyuryo > 0) {
            return system.maxContainerJyuryo;
        }
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String size = prefs.getString("container_size", "20ft");
        return "40ft".equals(size) ? 30000 : 24000;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    //============================================================
    //　機　能　:　EditText→int（カンマ除去対応）
    //============================================================
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

    private String safeText(EditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString();
    }

    //============================================================
    //　機　能　:　数値表示（DecimalFormat禁止 → String.formatで統一）
    //============================================================
    private String formatNumber(int value) {
        return String.format(Locale.JAPAN, "%,d", value);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //============================================================
    //　機　能　:　一時保存
    //============================================================
    @Override
    protected void onPause() {
        super.onPause();
        saveFormState();
    }

    private void saveFormState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CONTAINER_JYURYO, safeText(etContainerKg))
                .putString(KEY_DUNNAGE_JYURYO, safeText(etDunnageKg))
                .putString(KEY_CONTAINER_NO1, safeText(etContainerNo1))
                .putString(KEY_CONTAINER_NO2, safeText(etContainerNo2))
                .putString(KEY_SEAL_NO, safeText(etSealNo))
                .putString(KEY_CONTAINER_PHOTO_URI, containerPhotoUri != null ? containerPhotoUri.toString() : "")
                .putString(KEY_SEAL_PHOTO_URI, sealPhotoUri != null ? sealPhotoUri.toString() : "")
                .apply();
    }
}
