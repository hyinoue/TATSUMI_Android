package com.example.myapplication.connector;

import com.example.myapplication.db.dao.CommHistoryDao;
import com.example.myapplication.db.entity.CommHistoryEntity;
import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.io.Closeable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//================================
//　処理概要　:　SvcHandyWrapperクラス
//================================

public class SvcHandyWrapper implements Closeable {
    private static final int RETRY_COUNT = 3;
    private static final int MAX_TEXT_LENGTH = 1000;

    private final SvcHandyRepository repository;
    private final CommHistoryDao commHistoryDao;
    private final SimpleDateFormat dbDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);

    //=======================================
    //　機　能　:　SvcHandyWrapperの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //=======================================

    public SvcHandyWrapper() {
        this(new SvcHandyRepository(), null);
    }
    //================================================
    //　機　能　:　SvcHandyWrapperの初期化処理
    //　引　数　:　repository ..... SvcHandyRepository
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //================================================

    public SvcHandyWrapper(SvcHandyRepository repository) {
        this(repository, null);
    }

    public SvcHandyWrapper(SvcHandyRepository repository, CommHistoryDao commHistoryDao) {
        this.repository = repository;
        this.commHistoryDao = commHistoryDao;
    }
    //============================
    //　機　能　:　sagyou Ymdを取得する
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... なし
    //============================

    public Date getSagyouYmd() throws Exception {
        CommHistoryRow history = getHistoryRow("getSagyouYmd");
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    Date result = repository.getSagyouYmd();
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("作業日の取得に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }
    //=================================
    //　機　能　:　update Ymd Hmsを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[Date] ..... なし
    //=================================

    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        CommHistoryRow history = getHistoryRow("getUpdateYmdHms");
        history.argument = String.valueOf(sagyouYmd);
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    Date result = repository.getUpdateYmdHms(sagyouYmd);
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("出荷データ更新日時の取得に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }
    //==================================
    //　機　能　:　syukka Dataを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[SyukkaData] ..... なし
    //==================================

    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        CommHistoryRow history = getHistoryRow("getSyukkaData");
        history.argument = String.valueOf(sagyouYmd);
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    SyukkaData result = repository.getSyukkaData(sagyouYmd);
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("出荷データの取得に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    //===================================
    //　機　能　:　syukka Dataを送信する
    //　引　数　:　data ..... BunningData
    //　戻り値　:　[boolean] ..... なし
    //===================================
    public boolean sendSyukkaData(BunningData data) throws Exception {
        CommHistoryRow history = getHistoryRow("sendSyukkaData");
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    boolean result = repository.sendSyukkaData(data);
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("出荷データの更新に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    //==================================
    //　機　能　:　syougo Dataを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyougoData] ..... なし
    //==================================
    public SyougoData getSyougoData() throws Exception {
        CommHistoryRow history = getHistoryRow("getSyougoData");
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    SyougoData result = repository.getSyougoData();
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("照合データの取得に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    //===================================
    //　機　能　:　syougo Dataを送信する
    //　引　数　:　data ..... CollateData
    //　戻り値　:　[boolean] ..... なし
    //===================================
    public boolean sendSyougoData(CollateData data) throws Exception {
        CommHistoryRow history = getHistoryRow("sendSyougoData");
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    boolean result = repository.sendSyougoData(data);
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("照合データの更新に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    //====================================
    //　機　能　:　upload Binary Fileを送信する
    //　引　数　:　fileName ..... String
    //　　　　　:　buffer ..... byte[]
    //　戻り値　:　[boolean] ..... なし
    //====================================
    public boolean uploadBinaryFile(String fileName, byte[] buffer) throws Exception {
        CommHistoryRow history = getHistoryRow("uploadBinaryFile");
        history.argument = fileName;
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    boolean result = repository.uploadBinaryFile(fileName, buffer);
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("ファイルのアップロードに失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    //==================================================
    //　機　能　:　download Handy Execute File Namesを取得する
    //　引　数　:　なし
    //　戻り値　:　[String[]] ..... なし
    //==================================================
    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        CommHistoryRow history = getHistoryRow("getDownloadHandyExecuteFileNames");
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    String[] result = repository.getDownloadHandyExecuteFileNames();
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("更新対象ファイルの取得に失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    //===============================================
    //　機　能　:　download Handy Execute Fileを取得する
    //　引　数　:　fileName ..... String
    //　戻り値　:　[byte[]] ..... なし
    //===============================================
    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        CommHistoryRow history = getHistoryRow("getDownloadHandyExecuteFile");
        history.argument = fileName;
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    byte[] result = repository.getDownloadHandyExecuteFile(fileName);
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception("ファイルのダウンロードに失敗しました", lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    private CommHistoryRow getHistoryRow(String procName) {
        CommHistoryRow row = new CommHistoryRow();
        row.startYmdhms = new Date();
        row.procName = procName;
        return row;
    }

    private void saveHistoryRow(CommHistoryRow history) {
        if (commHistoryDao == null || history == null) {
            return;
        }

        if (history.endYmdhms == null) {
            history.endYmdhms = new Date();
        }

        CommHistoryEntity entity = new CommHistoryEntity();
        entity.startYmdhms = nullSafe(formatDbDate(history.startYmdhms));
        entity.endYmdhms = nullSafe(formatDbDate(history.endYmdhms));
        entity.procName = trimToLength(nullSafe(history.procName), 100);
        entity.argument = trimToLength(nullSafe(history.argument), 255);
        entity.returnValue = trimToLength(nullSafe(history.returnValue), MAX_TEXT_LENGTH);
        entity.errNumber = trimToLength(nullSafe(history.errorNumber), 20);
        entity.errDescription = trimToLength(history.errorDescription, MAX_TEXT_LENGTH);
        commHistoryDao.upsert(entity);
    }

    private void setErrorInfo(CommHistoryRow history, Exception exception) {
        if (history == null || exception == null) {
            return;
        }
        history.errorNumber = exception.getClass().getSimpleName();
        history.errorDescription = exception.getMessage();
    }

    private String formatDbDate(Date value) {
        if (value == null) {
            return null;
        }
        return dbDateFormat.format(value);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }

    private static class CommHistoryRow {
        private Date startYmdhms;
        private Date endYmdhms;
        private String procName;
        private String argument;
        private String returnValue;
        private String errorNumber;
        private String errorDescription;
    }

    //============================
    //　機　能　:　closeの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    public void close() {
        // no-op (placeholder for future resources)
    }
}
