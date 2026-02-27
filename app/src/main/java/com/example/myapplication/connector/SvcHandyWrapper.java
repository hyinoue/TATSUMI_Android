package com.example.myapplication.connector;

import com.example.myapplication.db.dao.CommHistoryDao;
import com.example.myapplication.db.entity.CommHistoryEntity;
import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;
import com.example.myapplication.time.DateTimeFormatUtil;

import java.io.Closeable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//============================================================
//　処理概要　:　リポジトリの結果を業務向け戻り値へ整形するサービスラッパークラス
//　関　　数　:　getSagyouYmd ............................... 作業日取得（リトライ＋履歴）
//　　　　　　:　getUpdateYmdHms ............................ 更新日時取得（リトライ＋履歴）
//　　　　　　:　getSyukkaData .............................. 出荷データ取得（リトライ＋履歴）
//　　　　　　:　sendSyukkaData ............................. 出荷データ送信（リトライ＋履歴）
//　　　　　　:　getSyougoData .............................. 照合データ取得（リトライ＋履歴）
//　　　　　　:　sendSyougoData ............................. 照合データ送信（リトライ＋履歴）
//　　　　　　:　uploadBinaryFile ........................... バイナリ送信（リトライ＋履歴）
//　　　　　　:　getDownloadHandyExecuteFileNames ........... 実行ファイル名一覧取得（リトライ＋履歴）
//　　　　　　:　getDownloadHandyExecuteFile ................ 実行ファイル取得（リトライ＋履歴）
//　　　　　　:　close ...................................... クローズ（将来拡張用）
//　　　　　　:　getHistoryRow ............................... 履歴行（作業領域）生成
//　　　　　　:　saveHistoryRow .............................. 履歴行をDBへ保存
//　　　　　　:　setErrorInfo ................................ エラー情報の設定
//　　　　　　:　formatDbDate ................................ DB用日時文字列へ整形
//　　　　　　:　nullSafe .................................... null安全文字列化
//　　　　　　:　trimToLength ................................ 文字列の最大長制限
//　　　　　　:　createLogId ................................. ログID採番（yyyyMMdd + 連番）
//============================================================
public class SvcHandyWrapper implements Closeable {

    private static final int RETRY_COUNT = 3;      // リトライ回数
    private static final int MAX_TEXT_LENGTH = 1000; // ログ文字列最大長

    private final SvcHandyRepository repository; // サービスリポジトリ
    private final CommHistoryDao commHistoryDao; // 通信履歴DAO

    // ログID採番の排他用
    private static final Object LOG_ID_LOCK = new Object();

    // ログID用日付（yyyyMMdd）
    private final SimpleDateFormat logIdDateFormat =
            new SimpleDateFormat("yyyyMMdd", Locale.JAPAN);

    //============================================================
    //　機　能　:　SvcHandyWrapperを初期化する（デフォルト）
    //　引　数　:　なし
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //============================================================
    public SvcHandyWrapper() {
        this(new SvcHandyRepository(), null);
    }

    //============================================================
    //　機　能　:　SvcHandyWrapperを初期化する（リポジトリ指定）
    //　引　数　:　repository ..... 通信リポジトリ
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //============================================================
    public SvcHandyWrapper(SvcHandyRepository repository) {
        this(repository, null);
    }

    //============================================================
    //　機　能　:　SvcHandyWrapperを初期化する（リポジトリ＋履歴DAO指定）
    //　引　数　:　repository ..... 通信リポジトリ
    //　　　　　:　commHistoryDao ..... データアクセスオブジェクト
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //============================================================
    public SvcHandyWrapper(SvcHandyRepository repository, CommHistoryDao commHistoryDao) {
        this.repository = repository;
        this.commHistoryDao = commHistoryDao;
    }

    //============================================================
    //　機　能　:　作業日を取得する（リトライし、履歴を保存する）
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... 作業日
    //============================================================
    public Date getSagyouYmd() throws Exception {
        CommHistoryRow history = getHistoryRow("getSagyouYmd");
        Exception lastException = null;

        try {
            // 一時障害を想定してリトライする
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    Date result = repository.getSagyouYmd();

                    // 成功時：戻り値と終了時刻を履歴へ設定
                    history.returnValue = String.valueOf(result);
                    history.endYmdhms = new Date();
                    return result;

                } catch (Exception ex) {
                    // 失敗時：最後の例外を保持して次のリトライへ
                    lastException = ex;
                }
            }

            // リトライ全失敗：履歴へエラー情報を設定し、呼び出し側へ例外を投げる
            setErrorInfo(history, lastException);
            throw new Exception("作業日の取得に失敗しました", lastException);

        } finally {
            // 成否に関係なく履歴保存（DAOが無い場合は何もしない）
            saveHistoryRow(history);
        }
    }

    //============================================================
    //　機　能　:　出荷データ更新日時を取得する（リトライし、履歴を保存する）
    //　引　数　:　sagyouYmd ..... 日時
    //　戻り値　:　[Date] ..... 更新日時
    //============================================================
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

    //============================================================
    //　機　能　:　出荷データを取得する（リトライし、履歴を保存する）
    //　引　数　:　sagyouYmd ..... 日時
    //　戻り値　:　[SyukkaData] ..... 出荷データ
    //============================================================
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

    //============================================================
    //　機　能　:　出荷データを送信する（リトライし、履歴を保存する）
    //　引　数　:　data ..... データ
    //　戻り値　:　[boolean] ..... 送信結果（成功:true / 失敗:false）
    //============================================================
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

    //============================================================
    //　機　能　:　照合データを取得する（リトライし、履歴を保存する）
    //　引　数　:　なし
    //　戻り値　:　[SyougoData] ..... 照合データ
    //============================================================
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

    //============================================================
    //　機　能　:　照合データを送信する（リトライし、履歴を保存する）
    //　引　数　:　data ..... データ
    //　戻り値　:　[boolean] ..... 送信結果（成功:true / 失敗:false）
    //============================================================
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

    //============================================================
    //　機　能　:　バイナリファイルを送信する（リトライし、履歴を保存する）
    //　引　数　:　fileName ..... ファイル関連情報
    //　　　　　:　buffer ..... 文字列バッファ
    //　戻り値　:　[boolean] ..... 送信結果（成功:true / 失敗:false）
    //============================================================
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

    //============================================================
    //　機　能　:　実行ファイル名一覧を取得する（リトライし、履歴を保存する）
    //　引　数　:　なし
    //　戻り値　:　[String[]] ..... 実行ファイル名一覧
    //============================================================
    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        CommHistoryRow history = getHistoryRow("getDownloadHandyExecuteFileNames");
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    String[] result = repository.getDownloadHandyExecuteFileNames();

                    // String.valueOf(array) は参照値になってしまうため、件数だけ残す
                    history.returnValue = (result == null) ? "null" : ("count=" + result.length);

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

    //============================================================
    //　機　能　:　実行ファイルを取得する（リトライし、履歴を保存する）
    //　引　数　:　fileName ..... ファイル関連情報
    //　戻り値　:　[byte[]] ..... ファイルデータ
    //============================================================
    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        CommHistoryRow history = getHistoryRow("getDownloadHandyExecuteFile");
        history.argument = fileName;
        Exception lastException = null;

        try {
            for (int count = 0; count < RETRY_COUNT; count++) {
                try {
                    byte[] result = repository.getDownloadHandyExecuteFile(fileName);

                    // byte[] も String.valueOf は参照値になるため、サイズだけ残す
                    history.returnValue = (result == null) ? "null" : ("bytes=" + result.length);

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

    //============================================================
    //　機　能　:　履歴行（作業領域）を生成する
    //　引　数　:　procName ..... 名称
    //　戻り値　:　[CommHistoryRow] ..... 履歴行
    //============================================================
    private CommHistoryRow getHistoryRow(String procName) {
        CommHistoryRow row = new CommHistoryRow();
        row.startYmdhms = new Date();
        row.procName = procName;
        return row;
    }

    //============================================================
    //　機　能　:　履歴行をDBへ保存する
    //　引　数　:　history ..... 通信履歴情報
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void saveHistoryRow(CommHistoryRow history) {
        // DAO未指定（履歴不要運用）の場合は何もしない
        if (commHistoryDao == null || history == null) {
            return;
        }

        // 終了時刻が未設定の場合はここで補完
        if (history.endYmdhms == null) {
            history.endYmdhms = new Date();
        }

        // DB格納用エンティティへ詰め替え
        CommHistoryEntity entity = new CommHistoryEntity();
        entity.logId = createLogId(history.startYmdhms);
        entity.startYmdhms = nullSafe(formatDbDate(history.startYmdhms));
        entity.endYmdhms = nullSafe(formatDbDate(history.endYmdhms));
        entity.procName = trimToLength(nullSafe(history.procName), 100);
        entity.argument = trimToLength(nullSafe(history.argument), 255);
        entity.returnValue = trimToLength(nullSafe(history.returnValue), MAX_TEXT_LENGTH);

        // エラー情報（無い場合はnullのままでも良いが、長さ制限だけ適用）
        entity.errDescription = trimToLength(history.errorDescription, MAX_TEXT_LENGTH);

        // 保存（Upsert）
        commHistoryDao.upsert(entity);
    }

    //============================================================
    //　機　能　:　履歴行へエラー情報を設定する
    //　引　数　:　history ..... 通信履歴情報
    //　　　　　:　exception ..... 例外情報
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setErrorInfo(CommHistoryRow history, Exception exception) {
        if (history == null || exception == null) {
            return;
        }
        history.errorDescription = exception.getMessage();
    }

    //============================================================
    //　機　能　:　DateをDB用日時文字列へ整形する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... DB用日時文字列
    //============================================================
    private String formatDbDate(Date value) {
        if (value == null) {
            return null;
        }
        return DateTimeFormatUtil.formatDbYmdHms(value);
    }

    //============================================================
    //　機　能　:　nullを空文字へ変換する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... nullの場合は空文字
    //============================================================
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    //============================================================
    //　機　能　:　文字列を最大長で切り詰める
    //　引　数　:　value ..... 設定値
    //　　　　　:　maxLength ..... 件数
    //　戻り値　:　[String] ..... 切り詰め後文字列（valueがnullならnull）
    //============================================================
    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }

    //============================================================
    //　機　能　:　ログIDを採番する（yyyyMMdd + 3桁連番）
    //　引　数　:　startDate ..... 日時
    //　戻り値　:　[String] ..... ログID
    //============================================================
    private String createLogId(Date startDate) {
        synchronized (LOG_ID_LOCK) {
            // 基準日はstartDate優先（nullなら現在時刻）
            Date baseDate = startDate == null ? new Date() : startDate;

            // 日付プレフィックス（yyyyMMdd）
            String ymdPrefix = logIdDateFormat.format(baseDate);

            // その日の最大ログIDを取得し、連番部分を+1する
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

            // yyyyMMdd + 3桁ゼロ埋め
            return ymdPrefix + String.format(Locale.JAPAN, "%03d", nextSequence);
        }
    }

    private static class CommHistoryRow {
        private Date startYmdhms;
        private Date endYmdhms;
        private String procName;
        private String argument;
        private String returnValue;
        private String errorDescription;
    }

    //============================================================
    //　機　能　:　closeの処理（将来拡張用：現状はクローズ対象なし）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    @Override
    public void close() {
        // no-op（現状クローズ対象のリソースなし）
    }
}
