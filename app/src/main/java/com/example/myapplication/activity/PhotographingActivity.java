package com.example.myapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.R;
import com.example.myapplication.settings.AppSettings;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.TimeUnit;


//================================
//　処理概要　:　PhotographingActivityクラス
//================================

/**
 * コンテナ/シール撮影用のカメラ画面Activity。
 *
 * <p>CameraXでプレビュー・撮影を行い、撮影後に確認/保存/破棄を選択する。</p>
 *
 * <p>主な処理フロー:</p>
 * <ul>
 *     <li>カメラ権限確認 → プレビュー開始。</li>
 *     <li>シャッターで撮影 → プレビュー確認画面へ切替。</li>
 *     <li>保存時はURIを返却し、破棄時は一時ファイルを削除。</li>
 * </ul>
 */
@ExperimentalCamera2Interop
public class PhotographingActivity extends BaseActivity {

    public static final String EXTRA_TARGET = "extra_target";      // "CONTAINER" / "SEAL"
    public static final String EXTRA_RESULT_URI = "extra_result_uri";

    private static final String TAG = "Photographing";

    private static final int CAM_UXGA = 0;
    private static final int CAM_QUADVGA = 1;
    private static final int CAM_XGA = 2;
    private static final int CAM_SVGA = 3;
    private static final int CAM_VGA = 4;
    private static final int CAM_QVGA = 5;

    private static final int CAM_FLASH_AUTO = 0;
    private static final int CAM_FLASH_ENABLE = 1;
    private static final int CAM_FLASH_DISABLE = 2;

    private static final int CAM_LIGHT_AUTO = 0;
    private static final int CAM_OUTDOOR = 1;
    private static final int CAM_FLUORESCENT = 2;
    private static final int CAM_INCANDESCE = 3;
    private static final int CAM_DIMLIGHT = 4;

    private static final int AE_MODE_AUTO = CaptureRequest.CONTROL_AE_MODE_ON;
    private static final int AE_MODE_ALWAYS_FLASH = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
    private static final int AE_MODE_AUTO_FLASH = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;

    private PreviewView previewView;
    private TextView statusBar;
    private ImageView capturedPreview;
    private View confirmButtons;
    private ImageButton btnSettings;
    private ImageButton btnShutter;
    private ImageButton btnSave;
    private ImageButton btnDiscard;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;

    private String target;
    private Uri pendingPhotoUri;
    private File pendingPhotoFile;
    private int lastImageSize;
    private int lastFlashMode;
    private int lastLightMode;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    setStatus("CAM_PERMISSION_DENIED");
                }
            });

    //======================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //======================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photographing);

        AppSettings.init(this);
        AppSettings.load();
        lastImageSize = AppSettings.CameraImageSize;
        lastFlashMode = AppSettings.CameraFlash;
        lastLightMode = AppSettings.CameraLightMode;

        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraSettingActivity.class);
            startActivity(intent);
        });

        target = getIntent().getStringExtra(EXTRA_TARGET);
        if (target == null) target = "CONTAINER";

        previewView = findViewById(R.id.previewView);
        statusBar = findViewById(R.id.statusBar);
        capturedPreview = findViewById(R.id.capturedPreview);
        confirmButtons = findViewById(R.id.confirmButtons);

        Button btnExit = findViewById(R.id.btnExit);
        btnShutter = findViewById(R.id.btnShutter);
        btnSave = findViewById(R.id.btnSave);
        btnDiscard = findViewById(R.id.btnDiscard);

        btnExit.setOnClickListener(v -> finish());
        btnShutter.setOnClickListener(v -> takePhoto());
        btnSave.setOnClickListener(v -> saveCapture());
        btnDiscard.setOnClickListener(v -> discardCapture());

        showCaptureReview(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    //======================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    @Override
    protected void onResume() {
        super.onResume();
        AppSettings.load();
        if (hasSettingChanges()) {
            lastImageSize = AppSettings.CameraImageSize;
            lastFlashMode = AppSettings.CameraFlash;
            lastLightMode = AppSettings.CameraLightMode;
            restartCamera();
        }
    }

    //======================
    //　機　能　:　cameraを開始する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    @ExperimentalCamera2Interop
    private void startCamera() {
        setStatus("CAM_STARTING");

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                cameraProvider.unbindAll();

                androidx.camera.core.Preview.Builder previewBuilder =
                        new androidx.camera.core.Preview.Builder();
                Size targetResolution = mapTargetResolution(AppSettings.CameraImageSize);
                if (targetResolution != null) {
                    previewBuilder.setTargetResolution(targetResolution);
                }
                Camera2Interop.Extender<androidx.camera.core.Preview> previewExtender =
                        new Camera2Interop.Extender<>(previewBuilder);
                previewExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                );
                previewExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_MODE,
                        mapAwbMode(AppSettings.CameraLightMode)
                );
                Preview preview = previewBuilder.build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                androidx.camera.core.ImageCapture.Builder captureBuilder =
                        new androidx.camera.core.ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(mapFlashMode(AppSettings.CameraFlash));
                if (targetResolution != null) {
                    captureBuilder.setTargetResolution(targetResolution);
                }
                Camera2Interop.Extender<androidx.camera.core.ImageCapture> captureExtender =
                        new Camera2Interop.Extender<>(captureBuilder);
                captureExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                );
                captureExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_MODE,
                        mapAwbMode(AppSettings.CameraLightMode)
                );
                captureExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        mapAeMode(AppSettings.CameraFlash)
                );
                imageCapture = captureBuilder.build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                camera = cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
                setStatus("CAM_RUNNING");
                triggerAfAeAtCenter();

            } catch (Exception e) {
                Log.e(TAG, "startCamera failed", e);
                setStatus("CAM_OPENERROR");
            }
        }, ContextCompat.getMainExecutor(this));
    }
    //========================
    //　機　能　:　restart Cameraの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //========================

    private void restartCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        startCamera();
    }
    //===========================
    //　機　能　:　setting Changesを判定する
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //===========================

    private boolean hasSettingChanges() {
        return lastImageSize != AppSettings.CameraImageSize
                || lastFlashMode != AppSettings.CameraFlash
                || lastLightMode != AppSettings.CameraLightMode;
    }
    //===============================
    //　機　能　:　map Target Resolutionの処理
    //　引　数　:　setting ..... int
    //　戻り値　:　[Size] ..... なし
    //===============================

    private Size mapTargetResolution(int setting) {
        switch (setting) {
            case CAM_UXGA:
                return new Size(1200, 1600);
            case CAM_QUADVGA:
                return new Size(960, 1280);
            case CAM_XGA:
                return new Size(768, 1024);
            case CAM_SVGA:
                return new Size(600, 800);
            case CAM_VGA:
                return new Size(480, 640);
            case CAM_QVGA:
                return new Size(240, 320);
            default:
                return null;
        }
    }
    //========================
    //　機　能　:　map Flash Modeの処理
    //　引　数　:　setting ..... int
    //　戻り値　:　[int] ..... なし
    //========================

    private int mapFlashMode(int setting) {
        switch (setting) {
            case CAM_FLASH_AUTO:
                return ImageCapture.FLASH_MODE_AUTO;
            case CAM_FLASH_ENABLE:
                return ImageCapture.FLASH_MODE_ON;
            case CAM_FLASH_DISABLE:
                return ImageCapture.FLASH_MODE_OFF;
            default:
                return ImageCapture.FLASH_MODE_AUTO;
        }
    }
    //=============================
    //　機　能　:　map Ae Modeの処理
    //　引　数　:　flashSetting ..... int
    //　戻り値　:　[int] ..... なし
    //=============================

    private int mapAeMode(int flashSetting) {
        switch (flashSetting) {
            case CAM_FLASH_ENABLE:
                return AE_MODE_ALWAYS_FLASH;
            case CAM_FLASH_DISABLE:
                return AE_MODE_AUTO;
            case CAM_FLASH_AUTO:
            default:
                return AE_MODE_AUTO_FLASH;
        }
    }
    //=============================
    //　機　能　:　map Awb Modeの処理
    //　引　数　:　lightSetting ..... int
    //　戻り値　:　[int] ..... なし
    //=============================

    private int mapAwbMode(int lightSetting) {
        switch (lightSetting) {
            case CAM_OUTDOOR:
                return CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT;
            case CAM_FLUORESCENT:
                return CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT;
            case CAM_INCANDESCE:
                return CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT;
            case CAM_DIMLIGHT:
                return CaptureRequest.CONTROL_AWB_MODE_WARM_FLUORESCENT;
            case CAM_LIGHT_AUTO:
            default:
                return CaptureRequest.CONTROL_AWB_MODE_AUTO;
        }
    }
    //======================
    //　機　能　:　take Photoの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }
        File file = getOutputFile();
        if (file == null) {
            setStatus("FILE_ERROR");
            return;
        }
        pendingPhotoFile = file;

        ImageCapture.OutputFileOptions output =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(
                output,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    //=============================================================
                    //　機　能　:　on Image Savedの処理
                    //　引　数　:　outputFileResults ..... ImageCapture.OutputFileResults
                    //　戻り値　:　[void] ..... なし
                    //=============================================================
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        pendingPhotoUri = FileProvider.getUriForFile(
                                PhotographingActivity.this,
                                getPackageName() + ".fileprovider",
                                file
                        );
                        if (capturedPreview != null) {
                            capturedPreview.setImageURI(pendingPhotoUri);
                        }
                        showCaptureReview(true);
                    }

                    //============================================
                    //　機　能　:　on Errorの処理
                    //　引　数　:　exception ..... ImageCaptureException
                    //　戻り値　:　[void] ..... なし
                    //============================================
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "photo capture failed", exception);
                        setStatus("CAPTURE_ERROR");
                    }
                }
        );
    }
    //======================
    //　機　能　:　captureを保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================

    private void saveCapture() {
        if (pendingPhotoUri == null) {
            setStatus("SAVE_ERROR");
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_URI, pendingPhotoUri.toString());
        result.putExtra(EXTRA_TARGET, target);
        setResult(RESULT_OK, result);
        finish();
    }
    //=========================
    //　機　能　:　discard Captureの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=========================

    private void discardCapture() {
        if (pendingPhotoFile != null && pendingPhotoFile.exists()) {
            boolean deleted = pendingPhotoFile.delete();
            Log.d(TAG, "discard photo deleted=" + deleted);
        }
        pendingPhotoFile = null;
        pendingPhotoUri = null;
        if (capturedPreview != null) {
            capturedPreview.setImageDrawable(null);
        }
        showCaptureReview(false);
    }
    //=============================
    //　機　能　:　show Capture Reviewの処理
    //　引　数　:　show ..... boolean
    //　戻り値　:　[void] ..... なし
    //=============================

    private void showCaptureReview(boolean show) {
        if (capturedPreview != null) {
            capturedPreview.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (confirmButtons != null) {
            confirmButtons.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnShutter != null) {
            btnShutter.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (btnSettings != null) {
            btnSettings.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnDiscard != null) {
            btnDiscard.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    //=================================
    //　機　能　:　trigger Af Ae At Centerの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=================================

    private void triggerAfAeAtCenter() {
        if (camera == null) return;

        MeteringPoint point =
                //=================================================
                //　機　能　:　Surface Oriented Metering Point Factoryの処理
                //　引　数　:　1.0f .....
                //　　　　　:　.createPoint(0.5f ..... 1.0f)
                //　　　　　:　0.5f .....
                //　戻り値　:　[new] ..... なし
                //=================================================
                new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
                        .createPoint(0.5f, 0.5f);

        FocusMeteringAction action = new FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();

        camera.getCameraControl().startFocusAndMetering(action);
    }
    //=======================
    //　機　能　:　output Fileを取得する
    //　引　数　:　なし
    //　戻り値　:　[File] ..... なし
    //=======================

    private File getOutputFile() {
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            dir = getFilesDir();
        }
        String name = "capture_" + System.currentTimeMillis() + ".jpg";
        return new File(dir, name);
    }
    //===========================
    //　機　能　:　statusを設定する
    //　引　数　:　message ..... String
    //　戻り値　:　[void] ..... なし
    //===========================

    private void setStatus(String message) {
        if (statusBar != null) {
            statusBar.setText(message);
        }
    }
}
