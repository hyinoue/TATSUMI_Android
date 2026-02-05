package com.example.myapplication.log;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FileLogger {
    private static final String LOG_FILE_NAME = "ErrorLog.txt";
    private static final Object LOCK = new Object();

    private FileLogger() {
    }

    public static void info(Context context, String source, String message) {
        write(context, "INFO", source, message, null);
    }

    public static void error(Context context, String source, String message, Throwable t) {
        write(context, "ERROR", source, message, t);
    }

    private static void write(Context context, String level, String source, String message, Throwable t) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS0", Locale.JAPAN).format(new Date());
        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);

        StringBuilder sb = new StringBuilder();
        sb.append(ts).append("|").append(level).append("|file|").append(source).append("\n");

        if (message != null && !message.isEmpty()) {
            sb.append(message).append("\n");
        }
        if (t != null) {
            sb.append(t.toString()).append("\n");
            for (StackTraceElement e : t.getStackTrace()) {
                sb.append("   at ").append(e.toString()).append("\n");
            }
        }
        sb.append("\n");

        synchronized (LOCK) {
            try (FileOutputStream fos = new FileOutputStream(logFile, true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(sb.toString());
                bw.flush();
            } catch (Exception ignored) {
                // ログ失敗時の再帰防止
            }
        }
    }
}
