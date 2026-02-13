package com.example.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.connector.DataSync;

public class DataSyncWorker extends Worker {
    public static final String KEY_ERROR_MESSAGE = "key_error_message";

    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            DataSync dataSync = new DataSync(getApplicationContext());
            boolean success = dataSync.runSync();
            if (success) {
                return Result.success();
            }
            return Result.failure(new Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, "データ送受信に失敗しました")
                    .build());
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = ex.getClass().getSimpleName();
            }
            return Result.failure(new Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, message)
                    .build());
        }
    }
}
