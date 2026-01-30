package com.example.myapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.TimeUnit;


//============================================================
//　処理概要　:　CameraPreviewActivityクラス
//============================================================

public class CameraPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET = "extra_target";      // "CONTAINER" / "SEAL"
    public static final String EXTRA_RESULT_URI = "extra_result_uri";

    private static final String TAG = "CameraPreview";

    private PreviewView previewView;
    private TextView statusBar;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;

    private String target;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    setStatus("CAM_PERMISSION_DENIED");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // タイトルバー非表示
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // ★ 先にレイアウトを読み込む
        setContentView(R.layout.activity_photographing);

        // ★ ここから findViewById
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraSettingActivity.class);
            startActivity(intent);
        });

        target = getIntent().getStringExtra(EXTRA_TARGET);
        if (target == null) target = "CONTAINER";

        previewView = findViewById(R.id.previewView);
        statusBar = findViewById(R.id.statusBar);

        Button btnExit = findViewById(R.id.btnExit);
        Button btnAF = findViewById(R.id.btnAF);
        ImageButton btnShutter = findViewById(R.id.btnShutter);

        btnExit.setOnClickListener(v -> finish());
        btnAF.setOnClickListener(v -> triggerAfAeAtCenter());
        btnShutter.setOnClickListener(v -> takePhoto());

        // 権限→開始
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }


    private void startCamera() {
        setStatus("CAM_STARTING");

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                cameraProvider.unbindAll();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageCapture（シャッター用）
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                camera = cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
                setStatus("CAM_RUNNING");

            } catch (Exception e) {
                Log.e(TAG, "startCamera failed", e);
                setStatus("CAM_OPENERROR");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            setStatus("CAM_NOT_READY");
            return;
        }

        try {
            String prefix = "CONTAINER".equals(target) ? "container_" : "seal_";
            File photoFile = File.createTempFile(prefix, ".jpg", getCacheDir());

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );

            ImageCapture.OutputFileOptions options =
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            setStatus("CAPTURING");

            imageCapture.takePicture(
                    options,
                    ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                            setStatus("CAPTURE_OK");
                            Intent data = new Intent();
                            data.putExtra(EXTRA_RESULT_URI, uri.toString());
                            data.putExtra(EXTRA_TARGET, target);
                            setResult(RESULT_OK, data);
                            finish();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exc) {
                            Log.e(TAG, "takePicture error", exc);
                            setStatus("CAPTURE_ERROR");
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "takePhoto failed", e);
            setStatus("CAPTURE_ERROR");
        }
    }

    private void triggerAfAeAtCenter() {
        if (camera == null || previewView == null) {
            setStatus("CAM_NOT_READY");
            return;
        }

        float x = previewView.getWidth() / 2f;
        float y = previewView.getHeight() / 2f;

        SurfaceOrientedMeteringPointFactory factory =
                new SurfaceOrientedMeteringPointFactory(previewView.getWidth(), previewView.getHeight());
        MeteringPoint point = factory.createPoint(x, y);

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(point,
                        FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                        .build();

        camera.getCameraControl().startFocusAndMetering(action);
        setStatus("AF_TRIGGERED");
    }

    private void setStatus(@NonNull String msg) {
        if (statusBar != null) statusBar.setText(msg);
    }
}
