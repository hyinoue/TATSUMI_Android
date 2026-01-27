package com.example.myapplication.settings;

import androidx.annotation.NonNull;

public class HandyUtil {

    private HandyUtil() {
    }

    /**
     * ISO 6346 コンテナ番号のチェックデジット計算
     */
    @NonNull
    public static String calcCheckDigit(String containerNo) {
        if (containerNo == null || containerNo.trim().isEmpty()) {
            return "";
        }

        String value = containerNo.trim().toUpperCase();
        int sum = 0;
        for (int i = 0; i < value.length(); i++) {
            int code = charToCode(value.charAt(i));
            if (code < 0) {
                return "";
            }
            sum += code * (1 << i);
        }

        int check = sum % 11;
        if (check == 10) check = 0;
        return String.valueOf(check);
    }

    private static int charToCode(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }

        switch (c) {
            case 'A':
                return 10;
            case 'B':
                return 12;
            case 'C':
                return 13;
            case 'D':
                return 14;
            case 'E':
                return 15;
            case 'F':
                return 16;
            case 'G':
                return 17;
            case 'H':
                return 18;
            case 'I':
                return 19;
            case 'J':
                return 20;
            case 'K':
                return 21;
            case 'L':
                return 23;
            case 'M':
                return 24;
            case 'N':
                return 25;
            case 'O':
                return 26;
            case 'P':
                return 27;
            case 'Q':
                return 28;
            case 'R':
                return 29;
            case 'S':
                return 30;
            case 'T':
                return 31;
            case 'U':
                return 32;
            case 'V':
                return 34;
            case 'W':
                return 35;
            case 'X':
                return 36;
            case 'Y':
                return 37;
            case 'Z':
                return 38;
            default:
                return -1;
        }
    }
}