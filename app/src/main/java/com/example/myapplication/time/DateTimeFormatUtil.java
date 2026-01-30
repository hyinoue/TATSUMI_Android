package com.example.myapplication.time;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//============================================================
//　処理概要　:　DateTimeFormatUtilクラス
//============================================================

public final class DateTimeFormatUtil {
    private static final String OUTPUT_PATTERN = "MM-dd HH:mm";
    private static final String[] INPUT_PATTERNS = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};

    private DateTimeFormatUtil() {
    }

    public static String formatSagyouYmdForDisplay(@Nullable String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        for (String pattern : INPUT_PATTERNS) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.JAPAN);
                parser.setLenient(false);
                Date parsed = parser.parse(trimmed);
                if (parsed != null) {
                    return new SimpleDateFormat(OUTPUT_PATTERN, Locale.JAPAN).format(parsed);
                }
            } catch (ParseException ignored) {
            }
        }

        if (trimmed.length() >= 16) {
            return trimmed.substring(5, 16);
        }

        return trimmed;
    }
}
