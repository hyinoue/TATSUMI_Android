package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.log.FileLogger;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FileLogger.info(this, "Application#onCreate", "app start");

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                FileLogger.error(this, "UncaughtException", "thread=" + thread.getName(), throwable));
    }
}
