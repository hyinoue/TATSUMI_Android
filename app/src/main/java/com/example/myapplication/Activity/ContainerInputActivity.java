package com.example.myapplication.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class ContainerInputActivity extends BaseActivity {

    private static final String TAG = "ContainerInput";

    private Button btnPhotoContainerNo;
    private Button btnPhotoSealNo;

    private ImageView ivPhotoContainer;
    private ImageView ivPhotoSeal;

    private enum PhotoTarget {CONTAINER, SEAL}

    private PhotoTarget currentTarget = PhotoTarget.CONTAINER;

    // ① CameraPreviewActivity 起動結果
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
                    ivPhotoContainer.setImageURI(null); // 同URI再描画対策
                    ivPhotoContainer.setImageURI(uri);
                    toast("コンテナNo写真を表示しました");
                } else {
                    ivPhotoSeal.setImageURI(null);
                    ivPhotoSeal.setImageURI(uri);
                    toast("シールNo写真を表示しました");
                }
            });

    // ② CAMERA権限要求
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCameraInternal();
                } else {
                    toast("カメラ権限が拒否されています");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_container_input);

        // ===== View取得 =====
        btnPhotoContainerNo = findViewById(R.id.btnPhotoContainerNo);
        btnPhotoSealNo = findViewById(R.id.btnPhotoSealNo);
        ivPhotoContainer = findViewById(R.id.ivPhotoContainer);
        ivPhotoSeal = findViewById(R.id.ivPhotoSeal);

        // ===== 取得確認 =====
        if (btnPhotoContainerNo == null || btnPhotoSealNo == null) {
            toast("撮影ボタンIDがレイアウトに存在しません");
            Log.e(TAG, "Photo buttons are null. Check layout IDs / setContentView.");
            return;
        }
        if (ivPhotoContainer == null || ivPhotoSeal == null) {
            toast("ImageView IDがレイアウトに存在しません");
            Log.e(TAG, "ImageViews are null. Check layout IDs / XML.");
            return;
        }

        // ===== クリック設定 =====
        btnPhotoContainerNo.setOnClickListener(v -> {
            currentTarget = PhotoTarget.CONTAINER;
            launchCamera();
        });

        btnPhotoSealNo.setOnClickListener(v -> {
            currentTarget = PhotoTarget.SEAL;
            launchCamera();
        });

        // ===== 下ボタン（include） =====
        setupBottomButtons();
    }

    private void launchCamera() {
        // 権限チェック→必要なら要求
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCameraInternal();
    }

    private void launchCameraInternal() {
        Intent intent = new Intent(this, CameraPreviewActivity.class);
        intent.putExtra(CameraPreviewActivity.EXTRA_TARGET,
                (currentTarget == PhotoTarget.CONTAINER) ? "CONTAINER" : "SEAL");
        cameraLauncher.launch(intent);
    }

    private void setupBottomButtons() {
        View bottom = findViewById(R.id.includeBottomButtons);
        if (bottom == null) {
            Log.w(TAG, "includeBottomButtons not found");
            return;
        }

        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        if (btnBlue == null || btnRed == null || btnGreen == null || btnYellow == null) {
            Log.w(TAG, "bottom buttons not found in include");
            return;
        }

        btnBlue.setText("確定");
        btnRed.setText("");
        btnGreen.setText("");
        btnYellow.setText("終了");

        btnBlue.setOnClickListener(v -> {
            toast("確定（未実装）");
            Log.i(TAG, "確定押下");
        });

        btnYellow.setOnClickListener(v -> finish());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
