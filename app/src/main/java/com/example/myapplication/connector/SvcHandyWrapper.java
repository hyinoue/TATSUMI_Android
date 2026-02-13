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

public class SvcHandyWrapper implements Closeable {
    private static final int RETRY_COUNT = 3;
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final Object LOG_ID_LOCK = new Object();

    private final SvcHandyRepository repository;
    private final CommHistoryDao commHistoryDao;
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
    private final SimpleDateFormat logIdDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.JAPAN);

    public SvcHandyWrapper() {
        this(new SvcHandyRepository(), null);
    }

    public SvcHandyWrapper(SvcHandyRepository repository) {
        this(repository, null);
    }

    public SvcHandyWrapper(SvcHandyRepository repository, CommHistoryDao commHistoryDao) {
        this.repository = repository;
        this.commHistoryDao = commHistoryDao;
    }

    public Date getSagyouYmd() throws Exception {
        return executeWithHistory("getSagyouYmd", null,
                repository::getSagyouYmd,
                "作業日の取得に失敗しました");
    }

    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        return executeWithHistory("getUpdateYmdHms", String.valueOf(sagyouYmd),
                () -> repository.getUpdateYmdHms(sagyouYmd),
                "出荷データ更新日時の取得に失敗しました");
    }

    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        return executeWithHistory("getSyukkaData", String.valueOf(sagyouYmd),
                () -> repository.getSyukkaData(sagyouYmd),
                "出荷データの取得に失敗しました");
    }

    public boolean sendSyukkaData(BunningData data) throws Exception {
        return executeWithHistory("sendSyukkaData", null,
                () -> repository.sendSyukkaData(data),
                "出荷データの更新に失敗しました");
    }

    public SyougoData getSyougoData() throws Exception {
        return executeWithHistory("getSyougoData", null,
                repository::getSyougoData,
                "照合データの取得に失敗しました");
    }

    public boolean sendSyougoData(CollateData data) throws Exception {
        return executeWithHistory("sendSyougoData", null,
                () -> repository.sendSyougoData(data),
                "照合データの更新に失敗しました");
    }

    public boolean uploadBinaryFile(String fileName, byte[] buffer) throws Exception {
        return executeWithHistory("uploadBinaryFile", fileName,
                () -> repository.uploadBinaryFile(fileName, buffer),
                "ファイルのアップロードに失敗しました");
    }

    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        return executeWithHistory("getDownloadHandyExecuteFileNames", null,
                repository::getDownloadHandyExecuteFileNames,
                "更新対象ファイルの取得に失敗しました");
    }

    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        return executeWithHistory("getDownloadHandyExecuteFile", fileName,
                () -> repository.getDownloadHandyExecuteFile(fileName),
                "ファイルのダウンロードに失敗しました");
    }

    private <T> T executeWithHistory(
            String procName,
            String argument,
            ThrowingSupplier<T> supplier,
            String failureMessage
    ) throws Exception {
        CommHistoryRow history = new CommHistoryRow(procName, argument);
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    T result = supplier.get();
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            setErrorInfo(history, lastException);
            throw new Exception(failureMessage, lastException);
        } finally {
            saveHistoryRow(history);
        }
    }

    private void saveHistoryRow(CommHistoryRow history) {
        if (commHistoryDao == null || history == null) return;
        if (history.endYmdhms == null) history.endYmdhms = new Date();

        CommHistoryEntity entity = new CommHistoryEntity();
        entity.logId = createLogId(history.startYmdhms);
        entity.startYmdhms = nullSafe(formatDbDate(history.startYmdhms));
        entity.endYmdhms = nullSafe(formatDbDate(history.endYmdhms));
        entity.procName = trimToLength(nullSafe(history.procName), 100);
        entity.argument = trimToLength(nullSafe(history.argument), 255);
        entity.returnValue = trimToLength(nullSafe(history.returnValue), MAX_TEXT_LENGTH);
        entity.errDescription = trimToLength(history.errorDescription, MAX_TEXT_LENGTH);
        commHistoryDao.upsert(entity);
    }

    private void setErrorInfo(CommHistoryRow history, Exception exception) {
        if (history == null || exception == null) return;
        history.errorDescription = exception.getMessage();
    }

    private String formatDbDate(Date value) {
        return value == null ? null : dbDateFormat.format(value);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String createLogId(Date startDate) {
        synchronized (LOG_ID_LOCK) {
            Date baseDate = startDate == null ? new Date() : startDate;
            String ymdPrefix = logIdDateFormat.format(baseDate);
            String maxLogId = commHistoryDao.findMaxLogIdByDatePrefix(ymdPrefix);

            int nextSequence = 1;
            if (maxLogId != null && maxLogId.length() >= 11) {
                String sequencePart = maxLogId.substring(maxLogId.length() - 3);
                try {
                    nextSequence = Integer.parseInt(sequencePart) + 1;
                } catch (NumberFormatException ignored) {
                    nextSequence = 1;
                }
            }
            return ymdPrefix + String.format(Locale.JAPAN, "%03d", nextSequence);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static class CommHistoryRow {
        private final Date startYmdhms = new Date();
        private Date endYmdhms;
        private final String procName;
        private final String argument;
        private String returnValue;
        private String errorDescription;

        private CommHistoryRow(String procName, String argument) {
            this.procName = procName;
            this.argument = argument;
        }
    }
}
