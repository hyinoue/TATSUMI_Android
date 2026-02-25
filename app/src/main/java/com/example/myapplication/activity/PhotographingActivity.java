package com.example.myapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
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

//============================================================
//　処理概要　:　コンテナ/シール撮影用カメラ画面Activity
//　　　　　　:　CameraXでプレビュー・撮影を行い、撮影後に確認/保存/破棄を行う。
//　　　　　　:　権限未許可の場合はカメラ権限を要求し、許可後にカメラを開始する。
//　関　　数　:　onCreate ............... 画面生成/初期化(権限確認/イベント設定/カメラ開始)
//　　　　　　:　onDestroy .............. リソース解放(シャッター音)
//　　　　　　:　onResume ............... 設定変更検知/カメラ再起動
//　　　　　　:　startCamera ............ CameraX初期化/プレビュー開始/撮影設定反映（Interop使用）
//　　　　　　:　restartCamera .......... カメラ再起動
//　　　　　　:　hasSettingChanges ...... カメラ設定変更有無判定
//　　　　　　:　mapTargetResolution .... 設定値→解像度(Size)変換
//　　　　　　:　mapFlashMode ........... 設定値→フラッシュモード変換
//　　　　　　:　mapAeMode .............. 設定値→AEモード変換
//　　　　　　:　mapAwbMode ............. 設定値→AWBモード変換
//　　　　　　:　takePhoto .............. 撮影処理(一時ファイル作成/保存/プレビュー表示)
//　　　　　　:　onPreviewTouched ....... タッチAF(フォーカス/測光)
//　　　　　　:　showFocusIndicator ..... フォーカス位置インジケータ表示
//　　　　　　:　playShutterSound ....... シャッター音再生
//　　　　　　:　saveCapture ............ 撮影結果を返却して終了
//　　　　　　:　discardCapture .......... 撮影破棄(一時ファイル削除/表示戻し)
//　　　　　　:　showCaptureReview ...... プレビュー確認UI表示切替
//　　　　　　:　triggerAfAeAtCenter .... 中央でAF/AEをトリガー
//　　　　　　:　getOutputFile .......... 出力ファイル生成
//　　　　　　:　setStatus .............. ステータス表示設定
//============================================================

@ExperimentalCamera2Interop
public class PhotographingActivity extends BaseActivity {

    public static final String EXTRA_TARGET = "extra_target";      // "CONTAINER" / "SEAL"
    public static final String EXTRA_RESULT_URI = "extra_result_uri"; // 撮影結果URI返却キー

    private static final String TAG = "Photographing"; // ログタグ

    private static final int CAM_UXGA = 0;    // 画像サイズ: UXGA
    private static final int CAM_QUADVGA = 1; // 画像サイズ: QUADVGA
    private static final int CAM_XGA = 2;     // 画像サイズ: XGA
    private static final int CAM_SVGA = 3;    // 画像サイズ: SVGA
    private static final int CAM_VGA = 4;     // 画像サイズ: VGA
    private static final int CAM_QVGA = 5;    // 画像サイズ: QVGA

    private static final int CAM_FLASH_AUTO = 0;    // フラッシュ: AUTO
    private static final int CAM_FLASH_ENABLE = 1;  // フラッシュ: ON
    private static final int CAM_FLASH_DISABLE = 2; // フラッシュ: OFF

    private static final int CAM_LIGHT_AUTO = 0;   // 露出補正: AUTO
    private static final int CAM_OUTDOOR = 1;      // 露出補正: 屋外
    private static final int CAM_FLUORESCENT = 2;  // 露出補正: 蛍光灯
    private static final int CAM_INCANDESCE = 3;   // 露出補正: 白熱灯
    private static final int CAM_DIMLIGHT = 4;     // 露出補正: 暗所

    private static final int AE_MODE_AUTO = CaptureRequest.CONTROL_AE_MODE_ON;                        // AE: 自動露出
    private static final int AE_MODE_ALWAYS_FLASH = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;   // AE: 常時発光
    private static final int AE_MODE_AUTO_FLASH = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;       // AE: 自動発光

    private PreviewView previewView;     // カメラプレビュー
    private TextView statusBar;          // ステータスメッセージ
    private ImageView capturedPreview;   // 撮影画像プレビュー
    private View focusIndicator;         // フォーカス表示
    private View confirmButtons;         // 保存/破棄ボタン領域
    private ImageButton btnSettings;     // 設定ボタン
    private ImageButton btnShutter;      // シャッターボタン
    private ImageButton btnSave;         // 保存ボタン
    private ImageButton btnDiscard;      // 破棄ボタン

    private ProcessCameraProvider cameraProvider; // CameraXプロバイダ
    private Camera camera;                        // カメラインスタンス
    private ImageCapture imageCapture;            // 撮影ユースケース

    private String target;               // 撮影対象種別
    private Uri pendingPhotoUri;         // 仮保存画像URI
    private File pendingPhotoFile;       // 仮保存画像ファイル
    private int lastImageSize;           // 最終画像サイズ設定
    private int lastFlashMode;           // 最終フラッシュ設定
    private int lastLightMode;           // 最終露出補正設定
    private MediaActionSound shutterSound; // シャッター音

    // カメラの実行時パーミッション（android.permission.CAMERA）をリクエストするためのLauncher。
    // Activity Result API を使用して、非同期で許可結果を受け取る。
    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(
                    // 1つのパーミッションをリクエストするContract
                    new ActivityResultContracts.RequestPermission(),
                    // パーミッション許可結果のコールバック（true: 許可 / false: 拒否）
                    granted -> {
                        if (granted) {
                            // ユーザーがカメラ権限を許可した場合
                            // → カメラ処理を開始する
                            startCamera();
                        } else {
                            // ユーザーがカメラ権限を拒否した場合
                            // → カメラは起動せず、ステータスのみ更新する
                            setStatus("CAM_PERMISSION_DENIED");
                        }
                    }
            );

    //============================================
    //　機　能　:　画面生成時の初期化処理
    //　引　数　:　savedInstanceState ..... Bundle
    //　戻り値　:　[void] ..... なし
    //============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photographing);

        // 設定初期化/読み込み
        AppSettings.init(this);
        AppSettings.load();
        lastImageSize = AppSettings.CameraImageSize;
        lastFlashMode = AppSettings.CameraFlash;
        lastLightMode = AppSettings.CameraLightMode;

        // 設定ボタン
        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraSettingActivity.class);
            startActivity(intent);
        });

        // 呼び出し元から撮影対象を受け取る（未指定はCONTAINER）
        target = getIntent().getStringExtra(EXTRA_TARGET);
        if (target == null) target = "CONTAINER";

        // 画面部品取得
        previewView = findViewById(R.id.previewView);
        statusBar = findViewById(R.id.statusBar);
        capturedPreview = findViewById(R.id.capturedPreview);
        focusIndicator = findViewById(R.id.focusIndicator);
        confirmButtons = findViewById(R.id.confirmButtons);

        Button btnExit = findViewById(R.id.btnExit);
        btnShutter = findViewById(R.id.btnShutter);
        btnSave = findViewById(R.id.btnSave);
        btnDiscard = findViewById(R.id.btnDiscard);

        // イベント設定
        btnExit.setOnClickListener(v -> finish());
        btnShutter.setOnClickListener(v -> takePhoto());
        btnSave.setOnClickListener(v -> saveCapture());
        btnDiscard.setOnClickListener(v -> discardCapture());
        previewView.setOnTouchListener(this::onPreviewTouched);

        // シャッター音
        shutterSound = new MediaActionSound();
        shutterSound.load(MediaActionSound.SHUTTER_CLICK);

        // 初期はレビュー非表示
        showCaptureReview(false);

        // 権限確認→許可済なら開始、未許可なら要求
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    //================================================================
    //　機　能　:　リソース解放(シャッター音)
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shutterSound != null) {
            shutterSound.release();
            shutterSound = null;
        }
    }

    //============================
    //　機　能　:　画面再表示時の処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    protected void onResume() {
        super.onResume();

        // 設定再読み込み
        AppSettings.load();

        // 設定変更があればカメラ再起動
        if (hasSettingChanges()) {
            lastImageSize = AppSettings.CameraImageSize;
            lastFlashMode = AppSettings.CameraFlash;
            lastLightMode = AppSettings.CameraLightMode;
            restartCamera();
        }
    }

    //============================
    //　機　能　:　cameraを開始する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    // ※クラスに @ExperimentalCamera2Interop を付けているため、メソッド側のOpt-inは不要。
    //   （もしクラスに付けない方針なら、このメソッドに @ExperimentalCamera2Interop を付与する。）
    private void startCamera() {
        setStatus("CAM_STARTING");

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                // プロバイダ取得/既存バインド解除
                cameraProvider = future.get();
                cameraProvider.unbindAll();

                // --- Preview設定 ---
                androidx.camera.core.Preview.Builder previewBuilder =
                        new androidx.camera.core.Preview.Builder();

                // 解像度設定
                Size targetResolution = mapTargetResolution(AppSettings.CameraImageSize);
                if (targetResolution != null) {
                    previewBuilder.setTargetResolution(targetResolution);
                }

                // Camera2パラメータ設定（AF/AWB）
                // ※Camera2InteropはExperimental APIのためOpt-inが必要（本クラスで実施済み）
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

                // --- Capture設定 ---
                androidx.camera.core.ImageCapture.Builder captureBuilder =
                        new androidx.camera.core.ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(mapFlashMode(AppSettings.CameraFlash));
                if (targetResolution != null) {
                    captureBuilder.setTargetResolution(targetResolution);
                }

                // Camera2パラメータ設定（AF/AWB/AE）
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

                // 背面カメラを選択してライフサイクルにバインド
                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                camera = cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);

                setStatus("CAM_RUNNING");

                // 開始直後に中央でAF/AEを一度トリガー
                triggerAfAeAtCenter();

            } catch (Exception e) {
                Log.e(TAG, "startCamera failed", e);
                setStatus("CAM_OPENERROR");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //==============================
    //　機　能　:　restart Cameraの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================
    private void restartCamera() {
        // 既存バインド解除して再開始
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        startCamera();
    }

    //=================================
    //　機　能　:　setting Changesを判定する
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... なし
    //=================================
    private boolean hasSettingChanges() {
        return lastImageSize != AppSettings.CameraImageSize
                || lastFlashMode != AppSettings.CameraFlash
                || lastLightMode != AppSettings.CameraLightMode;
    }

    //=====================================
    //　機　能　:　map Target Resolutionの処理
    //　引　数　:　setting ..... int
    //　戻り値　:　[Size] ..... 対応解像度（未対応はnull）
    //=====================================
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

    //==============================
    //　機　能　:　map Flash Modeの処理
    //　引　数　:　setting ..... int
    //　戻り値　:　[int] ..... ImageCapture.FLASH_MODE_*
    //==============================
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

    //===================================
    //　機　能　:　map Ae Modeの処理
    //　引　数　:　flashSetting ..... int
    //　戻り値　:　[int] ..... CaptureRequest.CONTROL_AE_MODE_*
    //===================================
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

    //===================================
    //　機　能　:　map Awb Modeの処理
    //　引　数　:　lightSetting ..... int
    //　戻り値　:　[int] ..... CaptureRequest.CONTROL_AWB_MODE_*
    //===================================
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

    //============================
    //　機　能　:　take Photoの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // シャッター音
        playShutterSound();

        // 出力ファイル生成
        File file = getOutputFile();
        if (file == null) {
            setStatus("FILE_ERROR");
            return;
        }
        pendingPhotoFile = file;

        ImageCapture.OutputFileOptions output =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        // 撮影実行
        imageCapture.takePicture(
                output,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    //===================================================================
                    //　機　能　:　撮影成功時の処理
                    //　引　数　:　outputFileResults ..... ImageCapture.OutputFileResults
                    //　戻り値　:　[void] ..... なし
                    //===================================================================
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // File→URIへ変換（FileProvider経由）
                        pendingPhotoUri = FileProvider.getUriForFile(
                                PhotographingActivity.this,
                                getPackageName() + ".fileprovider",
                                file
                        );

                        // プレビュー表示へ反映
                        if (capturedPreview != null) {
                            capturedPreview.setImageURI(pendingPhotoUri);
                        }

                        // レビュー表示に切替
                        showCaptureReview(true);
                    }

                    //==================================================
                    //　機　能　:　撮影失敗時の処理
                    //　引　数　:　exception ..... ImageCaptureException
                    //　戻り値　:　[void] ..... なし
                    //==================================================
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "photo capture failed", exception);
                        setStatus("CAPTURE_ERROR");
                    }
                }
        );
    }

    //================================================================
    //　機　能　:　タッチAF(フォーカス/測光)
    //　引　数　:　v ..... View
    //　　　　　:　event ..... MotionEvent
    //　戻り値　:　[boolean] ..... True:消費
    //================================================================
    private boolean onPreviewTouched(View v, MotionEvent event) {
        // タッチ離し時のみ処理
        if (event.getAction() != MotionEvent.ACTION_UP || camera == null) {
            return true;
        }

        // タッチ位置を取得しインジケータ表示
        float touchX = event.getX();
        float touchY = event.getY();
        showFocusIndicator(touchX, touchY);

        // タッチ位置にAF/AEを設定
        MeteringPoint point = previewView.getMeteringPointFactory().createPoint(touchX, touchY);
        FocusMeteringAction action = new FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();
        camera.getCameraControl().startFocusAndMetering(action);
        return true;
    }

    //================================================================
    //　機　能　:　フォーカス位置インジケータ表示
    //　引　数　:　centerX ..... 表示中心X
    //　　　　　:　centerY ..... 表示中心Y
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void showFocusIndicator(float centerX, float centerY) {
        if (focusIndicator == null) {
            return;
        }

        // インジケータをタッチ位置へ移動（中心合わせ）
        float halfWidth = focusIndicator.getWidth() / 2f;
        float halfHeight = focusIndicator.getHeight() / 2f;
        focusIndicator.setX(centerX - halfWidth);
        focusIndicator.setY(centerY - halfHeight);

        // アニメーション表示
        focusIndicator.setScaleX(1.3f);
        focusIndicator.setScaleY(1.3f);
        focusIndicator.setAlpha(1f);
        focusIndicator.setVisibility(View.VISIBLE);
        focusIndicator.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .withEndAction(() -> focusIndicator.postDelayed(
                        () -> focusIndicator.setVisibility(View.GONE),
                        5000
                ))
                .start();
    }

    //================================================================
    //　機　能　:　シャッター音再生
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void playShutterSound() {
        if (shutterSound == null) {
            return;
        }
        shutterSound.play(MediaActionSound.SHUTTER_CLICK);
    }

    //============================
    //　機　能　:　captureを保存する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void saveCapture() {
        // 撮影結果が無ければエラー
        if (pendingPhotoUri == null) {
            setStatus("SAVE_ERROR");
            return;
        }

        // 呼び出し元へURIを返却
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_URI, pendingPhotoUri.toString());
        result.putExtra(EXTRA_TARGET, target);
        setResult(RESULT_OK, result);
        finish();
    }

    //===============================
    //　機　能　:　discard Captureの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===============================
    private void discardCapture() {
        // 一時ファイルがあれば削除
        if (pendingPhotoFile != null && pendingPhotoFile.exists()) {
            boolean deleted = pendingPhotoFile.delete();
            Log.d(TAG, "discard photo deleted=" + deleted);
        }

        // 状態をクリア
        pendingPhotoFile = null;
        pendingPhotoUri = null;

        // 画像表示をクリア
        if (capturedPreview != null) {
            capturedPreview.setImageDrawable(null);
        }

        // 撮影画面へ戻す
        showCaptureReview(false);
    }

    //===================================
    //　機　能　:　show Capture Reviewの処理
    //　引　数　:　show ..... boolean
    //　戻り値　:　[void] ..... なし
    //===================================
    private void showCaptureReview(boolean show) {
        // 撮影後レビューの表示/非表示切替
        if (capturedPreview != null) {
            capturedPreview.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (confirmButtons != null) {
            confirmButtons.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // 撮影中UIの表示/非表示切替
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

        // レビュー中はフォーカスインジケータを消す
        if (focusIndicator != null && show) {
            focusIndicator.setVisibility(View.GONE);
        }
    }

    //=======================================
    //　機　能　:　trigger Af Ae At Centerの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=======================================
    private void triggerAfAeAtCenter() {
        if (camera == null) return;

        // プレビュー中央を測光/フォーカス点として作成
        MeteringPoint point =
                new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
                        .createPoint(0.5f, 0.5f);

        // 中央でAF/AE開始（一定時間で自動キャンセル）
        FocusMeteringAction action = new FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();

        camera.getCameraControl().startFocusAndMetering(action);
    }

    //=============================
    //　機　能　:　output Fileを取得する
    //　引　数　:　なし
    //　戻り値　:　[File] ..... 出力先ファイル
    //=============================
    private File getOutputFile() {
        // まずはPictures配下、無ければ内部領域へ
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            dir = getFilesDir();
        }

        // 一意なファイル名で作成
        String name = "capture_" + System.currentTimeMillis() + ".jpg";
        return new File(dir, name);
    }

    //=================================
    //　機　能　:　statusを設定する
    //　引　数　:　message ..... String
    //　戻り値　:　[void] ..... なし
    //=================================
    private void setStatus(String message) {
        if (statusBar != null) {
            statusBar.setText(message);
        }
    }
}
