package com.example.myapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.TimeUnit;


//============================================================
//　処理概要　:　PhotographingActivityクラス
//============================================================

@ExperimentalCamera2Interop
public class PhotographingActivity extends BaseActivity {

    public static final String EXTRA_TARGET = "extra_target";      // "CONTAINER" / "SEAL"
    public static final String EXTRA_RESULT_URI = "extra_result_uri";

    private static final String TAG = "Photographing";

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

        setContentView(R.layout.activity_photographing);

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

    private void startCamera() {
        setStatus("CAM_STARTING");

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                cameraProvider.unbindAll();

                androidx.camera.core.Preview.Builder previewBuilder =
                        new androidx.camera.core.Preview.Builder();
                Camera2Interop.Extender<androidx.camera.core.Preview> previewExtender =
                        new Camera2Interop.Extender<>(previewBuilder);
                previewExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                );
                Preview preview = previewBuilder.build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                androidx.camera.core.ImageCapture.Builder captureBuilder =
                        new androidx.camera.core.ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);
                Camera2Interop.Extender<androidx.camera.core.ImageCapture> captureExtender =
                        new Camera2Interop.Extender<>(captureBuilder);
                captureExtender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
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

    private void takePhoto() {
        if (imageCapture == null) {
            setStatus("CAM_NOT_READY");
            return;
        }

        try {
            triggerAfAeAtCenter();
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
                            pendingPhotoUri = uri;
                            pendingPhotoFile = photoFile;
                            capturedPreview.setImageURI(uri);
                            showCaptureReview(true);
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

    private void saveCapture() {
        if (pendingPhotoUri == null) {
            setStatus("CAPTURE_EMPTY");
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_URI, pendingPhotoUri.toString());
        data.putExtra(EXTRA_TARGET, target);
        setResult(RESULT_OK, data);
        finish();
    }

    private void discardCapture() {
        if (pendingPhotoFile != null && pendingPhotoFile.exists()) {
            if (!pendingPhotoFile.delete()) {
                Log.w(TAG, "Failed to delete captured photo: " + pendingPhotoFile.getAbsolutePath());
            }
        }
        pendingPhotoUri = null;
        pendingPhotoFile = null;
        capturedPreview.setImageDrawable(null);
        showCaptureReview(false);
        setStatus("CAPTURE_DISCARD");
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

    private void showCaptureReview(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        capturedPreview.setVisibility(visibility);
        confirmButtons.setVisibility(visibility);
        btnShutter.setVisibility(show ? View.GONE : View.VISIBLE);
        btnSettings.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void setStatus(@NonNull String msg) {
        if (statusBar != null) statusBar.setText(msg);
    }
}
