package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class VanningCollationActivity extends BaseActivity {

    public static final String EXTRA_CONTAINER_ID = "extra_container_id";
    public static final String EXTRA_CONTAINER_NO = "extra_container_no";
    public static final String EXTRA_BUNDLE_CNT = "extra_bundle_cnt";
    public static final String EXTRA_SAGYOU_YMD = "extra_sagyou_ymd";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vanning_collation);

        // ▼ 下ボタン（include）を取得
        View bottom = findViewById(R.id.includeBottomButtons);

        // ▼ 各ボタンを取得
        MaterialButton btnBlue = bottom.findViewById(R.id.btnBottomBlue);
        MaterialButton btnRed = bottom.findViewById(R.id.btnBottomRed);
        MaterialButton btnGreen = bottom.findViewById(R.id.btnBottomGreen);
        MaterialButton btnYellow = bottom.findViewById(R.id.btnBottomYellow);

        // ▼ 文字設定（画面ごとにここだけ変える）
        btnBlue.setText("確定");
        btnRed.setText("");
        btnGreen.setText("");
        btnYellow.setText("終了");

        btnYellow.setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

    }

    // ==============================
    // 4色ボタンの実処理（タップも物理キーもここに集約）
    // ==============================
    @Override
    protected void onFunctionBlue() {
        // 確定
        // TODO: 確定処理をここに実装
        showInfoMsg("確定（未実装）", MsgDispMode.Label);
    }

    @Override
    protected void onFunctionRed() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    @Override
    protected void onFunctionGreen() {
        // 今は空（ボタンTextが空なので実行されない想定）
    }

    @Override
    protected void onFunctionYellow() {
        // 終了
        finish();
    }
}
