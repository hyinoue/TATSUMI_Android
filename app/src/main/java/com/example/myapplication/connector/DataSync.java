package com.example.myapplication.connector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.example.myapplication.db.AppDatabase;
import com.example.myapplication.db.dao.CommHistoryDao;
import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.dao.SystemDao;
import com.example.myapplication.db.dao.SyukkaContainerDao;
import com.example.myapplication.db.dao.SyukkaMeisaiDao;
import com.example.myapplication.db.dao.YoteiDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.db.entity.KakuninMeisaiEntity;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.SyukkaContainerEntity;
import com.example.myapplication.db.entity.SyukkaMeisaiEntity;
import com.example.myapplication.db.entity.YoteiEntity;
import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.CollateDtl;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyougoDtl;
import com.example.myapplication.model.SyougoHeader;
import com.example.myapplication.model.SyukkaData;
import com.example.myapplication.model.SyukkaHeader;
import com.example.myapplication.model.SyukkaMeisai;
import com.example.myapplication.time.XsdDateTime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


//============================================================
//　処理概要　:　DataSyncクラス
//　関　　数　:　sendSyukkaOnly ................. 出荷データのみ送信
//　　　　　　:　sendSyougoOnly ................. 照合データのみ送信
//　　　　　　:　runSync ........................ 送受信を含む同期処理
//　　　　　　:　getLastErrorMessage ............ 最終エラーメッセージ取得
//　　　　　　:　sagyouYotei .................... 作業予定日の取得
//　　　　　　:　historyDel ..................... 通信履歴の削除
//　　　　　　:　dataSousinAll .................. 未送信出荷データの一括送信
//　　　　　　:　dataSousinOnce ................. 出荷データ（コンテナ単位）の送信
//　　　　　　:　dataSousinSyougo ............... 未送信照合データの一括送信
//　　　　　　:　dataSousinSyougoOnce ........... 照合データ（コンテナ単位）の送信
//　　　　　　:　dataUpdate ..................... データ確認日時の更新
//　　　　　　:　receiveSyukkaData .............. 出荷データの受信・DB反映
//　　　　　　:　receiveSyougoData .............. 照合データの受信・DB反映
//　　　　　　:　getPicture ..................... 画像ファイル取得（送信用）
//　　　　　　:　downscaleJpegIfNeeded .......... 画像の縮小・回転補正・圧縮
//　　　　　　:　readExifRotation ............... Exifから回転角度取得
//　　　　　　:　rotateBitmapIfNeeded ........... 必要に応じてBitmap回転
//　　　　　　:　compressBitmap ................. JPEG圧縮（容量制限対応）
//　　　　　　:　deletePicture .................. 画像ファイル削除
//　　　　　　:　getImageFile ................... 画像ファイルパス生成
//　　　　　　:　resolveImageDir ................ 画像保存ディレクトリ解決
//　　　　　　:　formatDbDate ................... DB用日時文字列へ整形
//　　　　　　:　parseDbDate .................... DB用日時文字列の解析
//　　　　　　:　parseDbDateOrMin ............... 日時解析（失敗時は最小値）
//　　　　　　:　intOrZero ...................... null安全なint変換
//　　　　　　:　normalizeSendKey ............... 送信キー用の正規化（空白/制御文字除去）
//　　　　　　:　buildSendFailedMessage ......... 送信失敗メッセージ組み立て
//　　　　　　:　safeMessage .................... 例外メッセージ安全取得
//　　　　　　:　reportError .................... エラー通知（ログ/コールバック）
//============================================================
public class DataSync {
    private static final String TAG = "DataSync"; // ログタグ
    private static final int SYSTEM_RENBAN = 1;    // システム連番

    public interface ErrorHandler {
        void onError(String message);
    }

    private final AppDatabase db;                        // DBインスタンス
    private final SvcHandyWrapper svcWrapper;            // Webサービスラッパー
    private final CommHistoryDao commHistoryDao;         // 通信履歴DAO
    private final SyukkaContainerDao syukkaContainerDao; // 出荷コンテナDAO
    private final SyukkaMeisaiDao syukkaMeisaiDao;       // 出荷明細DAO
    private final YoteiDao yoteiDao;                     // 予定DAO
    private final KakuninContainerDao kakuninContainerDao; // 確認コンテナDAO
    private final KakuninMeisaiDao kakuninMeisaiDao;     // 確認明細DAO
    private final SystemDao systemDao;                   // システムDAO
    private final File imageDir;                         // 画像格納ディレクトリ

    private final ErrorHandler errorHandler; // エラーハンドラ

    private String lastErrorMessage; // 最終エラーメッセージ

    //================================================================
    //　機　能　:　DB用日時フォーマット（yyyy-MM-dd HH:mm:ss）を生成する
    //　引　数　:　なし
    //　戻り値　:　[SimpleDateFormat] ..... DB用日時フォーマッタ
    //================================================================
    private final SimpleDateFormat dbDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);

    public enum ImageType {
        CONTAINER("container"),
        SEAL("seal");

        private final String suffix;

        ImageType(String suffix) {
            this.suffix = suffix;
        }

        //================================================================
        //　機　能　:　画像種別のサフィックスを取得する
        //　引　数　:　なし
        //　戻り値　:　[String] ..... サフィックス
        //================================================================
        public String getSuffix() {
            return suffix;
        }
    }

    //================================================================
    //　機　能　:　DataSyncを初期化する（DBはデフォルト、コールバックなし）
    //　引　数　:　context ..... Context
    //　戻り値　:　[DataSync] ..... なし
    //================================================================
    public DataSync(Context context) {
        this(context, AppDatabase.getInstance(context), null, null);
    }

    //================================================================
    //　機　能　:　DataSyncを初期化する（DBはデフォルト、エラー通知あり）
    //　引　数　:　context ..... Context
    //　　　　　:　errorHandler ..... ErrorHandler
    //　戻り値　:　[DataSync] ..... なし
    //================================================================
    public DataSync(Context context, ErrorHandler errorHandler) {
        this(context, AppDatabase.getInstance(context), null, errorHandler);
    }

    //================================================================
    //　機　能　:　DataSyncを初期化する（フル指定）
    //　引　数　:　context ..... Context
    //　　　　　:　db ..... AppDatabase
    //　　　　　:　svcWrapper ..... SvcHandyWrapper
    //　　　　　:　errorHandler ..... ErrorHandler
    //　戻り値　:　[DataSync] ..... なし
    //================================================================
    public DataSync(Context context, AppDatabase db, SvcHandyWrapper svcWrapper, ErrorHandler errorHandler) {
        // DB/DAOの準備
        this.db = db;
        this.commHistoryDao = db.commHistoryDao();
        this.syukkaContainerDao = db.syukkaContainerDao();
        this.syukkaMeisaiDao = db.syukkaMeisaiDao();
        this.yoteiDao = db.yoteiDao();
        this.kakuninContainerDao = db.kakuninContainerDao();
        this.kakuninMeisaiDao = db.kakuninMeisaiDao();
        this.systemDao = db.systemDao();

        // 通信ラッパー（未指定の場合はデフォルト生成）
        this.svcWrapper = svcWrapper != null
                ? svcWrapper
                : new SvcHandyWrapper(new SvcHandyRepository(), this.commHistoryDao);

        // 画像保存先ディレクトリ
        this.imageDir = resolveImageDir(context);

        // エラー通知用コールバック
        this.errorHandler = errorHandler;
    }

    //================================================================
    //　機　能　:　出荷データのみ送信する（未送信分を全件送信）
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 送信成否
    //================================================================
    public boolean sendSyukkaOnly() throws Exception {
        // 前回エラーを初期化
        lastErrorMessage = null;

        // 作業予定日を取得（DB優先、無ければ通信で取得）
        Date sagyouYmd = sagyouYotei();

        // 未送信の出荷データをコンテナ単位で送信
        boolean sent = dataSousinAll(sagyouYmd);
        return sent;
    }

    //================================================================
    //　機　能　:　最終エラーメッセージを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... 最終エラーメッセージ
    //================================================================
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    //================================================================
    //　機　能　:　照合データのみ送信する（未送信分を全件送信）
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 送信成否
    //================================================================
    public boolean sendSyougoOnly() {
        return dataSousinSyougo();
    }

    //================================================================
    //　機　能　:　同期処理を実行する（送信→受信→DB反映）
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 同期処理にエラーがなければtrue
    //================================================================
    public boolean runSync() {
        // 通信履歴を一定期間より前の分だけ削除
        historyDel();

        boolean hasError = false;
        Date sagyouYmd = null;

        // 作業予定日の取得（ここで取れない場合は以降の出荷系処理をスキップ）
        try {
            sagyouYmd = sagyouYotei();
        } catch (Exception ex) {
            hasError = true;
            reportError(ex);
            reportError("作業予定が登録されていません");
        }

        // 出荷データ送信（作業予定日が取れた場合のみ）
        if (sagyouYmd != null) {
            try {
                boolean syukkaSent = dataSousinAll(sagyouYmd);
                if (!syukkaSent) {
                    hasError = true;
                    reportError(buildSendFailedMessage("出荷データの更新に失敗しました", lastErrorMessage));
                }
            } catch (Exception ex) {
                hasError = true;
                reportError(ex);
            }
        }

        // 照合データ送信（作業予定日に依存しない）
        boolean syougoSent = dataSousinSyougo();
        if (!syougoSent) {
            hasError = true;
            reportError(buildSendFailedMessage("照合データの更新に失敗しました", lastErrorMessage));
        }

        // 出荷データ受信＆更新（作業予定日が取れた場合のみ）
        if (sagyouYmd != null) {
            try {
                receiveSyukkaData(sagyouYmd);
                dataUpdate(sagyouYmd);
            } catch (Exception ex) {
                hasError = true;
                reportError(ex);
            }
        }

        // 照合データ受信
        try {
            receiveSyougoData();
        } catch (Exception ex) {
            hasError = true;
            reportError(ex);
        }

        // どこかでエラーがあればfalse
        return !hasError;
    }

    //================================================================
    //　機　能　:　例外をログ出力し、エラーメッセージを通知する
    //　引　数　:　ex ..... Exception
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void reportError(Exception ex) {
        // ログへ詳細出力
        Log.e(TAG, "DataSync failed", ex);

        // 表示用メッセージを整形（null/空を避ける）
        String msg = ex.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = ex.getClass().getSimpleName();
        }

        // コールバックへ通知
        reportError(msg);
    }

    //================================================================
    //　機　能　:　エラーメッセージを通知する（コールバックがある場合のみ）
    //　引　数　:　msg ..... String
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void reportError(String msg) {
        // 空メッセージは通知しない
        if (errorHandler != null && msg != null && !msg.trim().isEmpty()) {
            errorHandler.onError(msg);
        }
    }

    //================================================================
    //　機　能　:　作業予定日を取得する（DB優先／なければサービスから取得）
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... 作業予定日
    //================================================================
    private Date sagyouYotei() throws Exception {
        // まずはローカルDBの作業予定を参照
        YoteiEntity existing = yoteiDao.findFirst();
        if (existing != null && existing.sagyouYoteiYmd != null) {
            Date parsed = parseDbDate(existing.sagyouYoteiYmd);
            if (parsed != null) {
                return parsed;
            }
        }

        // DBに無い／解析できない場合はサービスから取得
        Date sagyouYmd = svcWrapper.getSagyouYmd();
        if (sagyouYmd == null) {
            throw new IllegalStateException("作業予定が登録されていません");
        }
        return sagyouYmd;
    }

    //================================================================
    //　機　能　:　通信履歴を削除する（直近1か月より前を削除）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void historyDel() {
        // 1か月前の日付を算出
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);

        // 1か月前より前の履歴を削除
        commHistoryDao.deleteBefore(formatDbDate(cal.getTime()));
    }

    //================================================================
    //　機　能　:　未送信の出荷データを全件送信する（コンテナ単位）
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[boolean] ..... 送信成否
    //================================================================
    private boolean dataSousinAll(Date sagyouYmd) {
        // 前回エラーを初期化
        lastErrorMessage = null;

        // 未送信コンテナを取得
        List<SyukkaContainerEntity> containers = syukkaContainerDao.findUnsent();

        // 1件でも送信に失敗したらfalseで中断
        for (SyukkaContainerEntity container : containers) {
            if (!dataSousinOnce(container, sagyouYmd)) {
                return false;
            }
        }
        return true;
    }

    //================================================================
    //　機　能　:　出荷データを1コンテナ分送信する
    //　引　数　:　container ..... SyukkaContainerEntity
    //　　　　　:　sagyouYmd ..... Date
    //　戻り値　:　[boolean] ..... 送信成否
    //================================================================
    private boolean dataSousinOnce(SyukkaContainerEntity container, Date sagyouYmd) {
        try {
            // containerIdは送信や画像取得のキーとして必須
            if (container.containerId == null) {
                Log.w(TAG, "Container ID is null; skip send. containerNo="
                        + formatContainerNo(container.containerNo));
                return false;
            }

            // 送信用データを組み立て
            BunningData data = new BunningData();
            data.syukkaYmd = sagyouYmd;
            data.containerNo = normalizeSendKey(container.containerNo);
            data.containerJyuryo = intOrZero(container.containerJyuryo);
            data.dunnageJyuryo = intOrZero(container.dunnageJyuryo);
            data.sealNo = normalizeSendKey(container.sealNo);

            // 画像を読み込み（必要なら縮小・回転補正・圧縮）
            data.containerPhoto = getPicture(container.containerId, ImageType.CONTAINER);
            data.sealPhoto = getPicture(container.containerId, ImageType.SEAL);

            // コンテナ紐づきの明細を取得
            List<SyukkaMeisaiEntity> detailRows =
                    syukkaMeisaiDao.findByContainerId(container.containerId);

            // bookingNoは明細で欠ける可能性があるため、コンテナ側の値をフォールバックに使う
            String fallbackBookingNo = normalizeSendKey(container.bookingNo);
            int missingBookingCount = 0;
            String missingBookingSample = "";

            // 明細を送信用の束（bundle）へ詰め替え
            for (SyukkaMeisaiEntity row : detailRows) {
                SyukkaMeisai detail = new SyukkaMeisai();
                detail.heatNo = normalizeSendKey(row.heatNo);
                detail.sokuban = normalizeSendKey(row.sokuban);

                // bookingNoは明細→無ければコンテナのbookingNo→それでも無ければエラー扱い
                String bookingNo = normalizeSendKey(row.bookingNo);
                if (bookingNo.isEmpty()) {
                    bookingNo = fallbackBookingNo;
                }
                if (bookingNo.isEmpty()) {
                    // 送信必須項目の欠落としてカウントし、ログ用にサンプルも保持
                    missingBookingCount++;
                    if (missingBookingSample.isEmpty()) {
                        missingBookingSample = detail.heatNo + "/" + detail.sokuban;
                    }
                }

                detail.bookingNo = normalizeSendKey(bookingNo);
                detail.bundleNo = normalizeSendKey(row.bundleNo);
                detail.syukkaSashizuNo = normalizeSendKey(row.syukkaSashizuNo);
                detail.jyuryo = intOrZero(row.jyuryo);

                // 送信データへ追加
                data.bundles.add(detail);
            }

            // 明細が無ければ送信しない（仕様上成立しない）
            if (data.bundles.isEmpty()) {
                Log.w(TAG, "No bundle details; skip SendSyukkaData. containerId="
                        + container.containerId + " containerNo="
                        + formatContainerNo(container.containerNo));
                return false;
            }

            // bookingNo欠落があれば送信しない（送信先で整合性が取れない可能性）
            if (missingBookingCount > 0) {
                Log.w(TAG, "BookingNo missing in bundle details; skip SendSyukkaData. containerId="
                        + container.containerId + " containerNo="
                        + formatContainerNo(container.containerNo)
                        + " missingCount=" + missingBookingCount
                        + " sampleHeatSokuban=" + missingBookingSample);
                return false;
            }

            // サービスへ送信
            if (svcWrapper.sendSyukkaData(data)) {
                // 送信済みマークを更新
                String now = formatDbDate(new Date());
                syukkaContainerDao.markSent(container.containerId, now);

                // 送信後は画像を削除（端末容量対策）
                deletePicture(container.containerId, ImageType.CONTAINER);
                deletePicture(container.containerId, ImageType.SEAL);

                return true;
            }

            // サービス戻り値がfalseの場合は失敗扱い
            Log.w(TAG, "SendSyukkaDataResult=false. containerId=" + container.containerId
                    + " containerNo=" + formatContainerNo(container.containerNo)
                    + " bundleCount=" + data.bundles.size());
            lastErrorMessage = "出荷データの更新に失敗しました";
            return false;

        } catch (Exception ex) {
            // 例外はログ出力して失敗扱い
            Log.e(TAG, "DataSousinOnce failed", ex);
            lastErrorMessage = safeMessage(ex);
            return false;
        }
    }

    //================================================================
    //　機　能　:　空文字判定を行う
    //　引　数　:　value ..... String
    //　戻り値　:　[boolean] ..... 空(null/空白のみ)ならtrue
    //================================================================
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    //================================================================
    //　機　能　:　例外メッセージを安全に取得する（null/空を避ける）
    //　引　数　:　ex ..... Exception
    //　戻り値　:　[String] ..... メッセージ
    //================================================================
    private String safeMessage(Exception ex) {
        if (ex == null) {
            return "不明なエラー";
        }
        String msg = ex.getMessage();
        if (isBlank(msg)) {
            return ex.getClass().getSimpleName();
        }
        return msg;
    }

    //================================================================
    //　機　能　:　送信失敗メッセージを組み立てる（詳細があれば追記）
    //　引　数　:　baseMessage ..... String
    //　　　　　:　detail ..... String
    //　戻り値　:　[String] ..... 組み立て後メッセージ
    //================================================================
    private String buildSendFailedMessage(String baseMessage, String detail) {
        // 詳細が無ければベースのみ
        if (isBlank(detail)) {
            return baseMessage;
        }

        // 余計な空白を除去
        String trimmed = detail.trim();

        // 既に同じ文言なら二重にしない
        if (baseMessage.equals(trimmed)) {
            return baseMessage;
        }

        // 改行で詳細を追記
        return baseMessage + "\n" + trimmed;
    }

    //================================================================
    //　機　能　:　送信キー用に文字列を正規化する（全角空白→半角、制御文字除去）
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... 正規化済み文字列（nullは空文字）
    //================================================================
    private String normalizeSendKey(String value) {
        if (value == null) {
            return "";
        }

        // 全角空白を半角へ寄せ、前後空白を除去
        String normalized = value.replace('\u3000', ' ').trim();

        // 制御文字（DEL等）を除去して送信事故を防ぐ
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch >= 0x20 && ch != 0x7F) || ch == '\t') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    //================================================================
    //　機　能　:　ログ出力用にcontainerNoを整形する（null/空を見やすく）
    //　引　数　:　containerNo ..... String
    //　戻り値　:　[String] ..... 整形後文字列
    //================================================================
    private String formatContainerNo(String containerNo) {
        if (containerNo == null || containerNo.trim().isEmpty()) {
            return "<empty>";
        }
        return containerNo.trim();
    }

    //================================================================
    //　機　能　:　未送信の照合データを全件送信する（完了分のみ）
    //　引　数　:　なし
    //　戻り値　:　[boolean] ..... 送信成否
    //================================================================
    private boolean dataSousinSyougo() {
        // 前回エラーを初期化
        lastErrorMessage = null;

        // 未送信かつ照合完了のコンテナを取得
        List<KakuninContainerEntity> containers = kakuninContainerDao.findUnsentCompleted();

        // 1件でも送信に失敗したらfalseで中断
        for (KakuninContainerEntity container : containers) {
            if (!dataSousinSyougoOnce(container)) {
                return false;
            }
        }
        return true;
    }

    //================================================================
    //　機　能　:　照合データを1コンテナ分送信する
    //　引　数　:　container ..... KakuninContainerEntity
    //　戻り値　:　[boolean] ..... 送信成否
    //================================================================
    private boolean dataSousinSyougoOnce(KakuninContainerEntity container) {
        try {
            // 送信用の照合データを組み立て
            CollateData collateData = new CollateData();
            collateData.containerID = normalizeSendKey(container.containerId);
            collateData.syogoKanryo = Boolean.TRUE.equals(container.containerSyougoKanryo);

            // コンテナに紐づく照合明細を詰め替え
            List<KakuninMeisaiEntity> detailRows =
                    kakuninMeisaiDao.findByContainerId(container.containerId);
            for (KakuninMeisaiEntity row : detailRows) {
                CollateDtl detail = new CollateDtl();
                detail.collateDtlheatNo = normalizeSendKey(row.heatNo);
                detail.collateDtlsokuban = normalizeSendKey(row.sokuban);
                detail.collateDtlsyougoKakunin = Boolean.TRUE.equals(row.containerSyougoKakunin);

                // 送信データへ追加
                collateData.collateDtls.add(detail);
            }

            // サービスへ送信
            if (svcWrapper.sendSyougoData(collateData)) {
                // 送信済みマークを更新
                String now = formatDbDate(new Date());
                kakuninContainerDao.markSent(container.containerId, now);
                return true;
            }

            // サービス戻り値がfalseの場合は失敗扱い
            Log.w(TAG, "SendSyougoDataResult=false");
            lastErrorMessage = "照合データの更新に失敗しました";
            return false;

        } catch (Exception ex) {
            // 例外はログ出力して失敗扱い
            Log.e(TAG, "DataSousinSyougoOnce failed", ex);
            lastErrorMessage = safeMessage(ex);
            return false;
        }
    }

    //================================================================
    //　機　能　:　データ確認日時を更新する（更新要否を判定して反映）
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void dataUpdate(Date sagyouYmd) {
        // 最終更新日時が未設定の予定を抽出（対象が無い場合は何もしない）
        List<YoteiEntity> candidates = yoteiDao.findWithNullLastUpd();
        if (candidates.isEmpty()) {
            return;
        }

        // ※呼び出し側の想定で副作用がある可能性があるため呼んでいる（失敗しても継続）
        try {
            svcWrapper.getSagyouYmd();
        } catch (Exception ex) {
            Log.e(TAG, "GetSagyouYmd failed", ex);
        }

        // ローカル側の最終更新日時
        Date lastUpd = parseDbDateOrMin(candidates.get(0).lastUpdYmdhms);

        // サーバ側の更新日時を取得
        Date getUpdate;
        try {
            getUpdate = svcWrapper.getUpdateYmdHms(sagyouYmd);
        } catch (Exception ex) {
            Log.e(TAG, "GetUpdateYmdHms failed", ex);
            return;
        }

        // サーバ更新がローカルより新しい場合は、ここでは更新しない（受信を優先したい意図）
        if (getUpdate != null && lastUpd != null && getUpdate.after(lastUpd)) {
            return;
        }

        // システムテーブルへデータ確認日時を反映
        String now = formatDbDate(new Date());
        if (systemDao.updateDataConf(SYSTEM_RENBAN, now, "DataSync#dataUpdate", now) == 0) {
            // 更新対象が無い場合は新規作成（upsert）
            SystemEntity system = new SystemEntity();
            system.renban = SYSTEM_RENBAN;
            system.dataConfYmdhms = now;
            system.updateProcName = "DataSync#dataUpdate";
            system.updateYmd = now;
            systemDao.upsert(system);
        }
    }

    //================================================================
    //　機　能　:　出荷データを受信してDBへ反映する（トランザクション）
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void receiveSyukkaData(Date sagyouYmd) throws Exception {
        // ※呼び出し側の想定で副作用がある可能性があるため呼んでいる（失敗しても継続）
        try {
            svcWrapper.getSagyouYmd();
        } catch (Exception ex) {
            Log.e(TAG, "GetSagyouYmd failed", ex);
        }

        // サービスから出荷データを取得
        SyukkaData data = svcWrapper.getSyukkaData(sagyouYmd);
        if (data == null) {
            throw new IllegalStateException("出荷データの取得に失敗しました");
        }

        // DB更新は一括トランザクションで整合性を保つ
        db.runInTransaction(() -> {
            // 古いデータをクリア（送信済み・紐づき済みのものを削除）
            yoteiDao.deleteAll();
            syukkaMeisaiDao.deleteSentLinked();
            syukkaContainerDao.deleteSent();

            // ヘッダ（予定）を反映
            for (SyukkaHeader header : data.header) {
                YoteiEntity entity = new YoteiEntity();
                entity.bookingNo = header.bookingNo;
                entity.sagyouYoteiYmd = formatDbDate(header.syukkaYmd);
                entity.containerCount = header.containerCount;
                entity.goukeiBundole = header.totalBundole;
                entity.goukeiJyuryo = header.totalJyuryo;
                entity.kanryoContainer = header.kanryoContainerCnt;
                entity.kanryoBundole = header.kanryoBundleSum;
                entity.kanryoJyuryo = header.knaryoJyuryoSum;
                entity.lastUpdYmdhms = formatDbDate(header.lastUpdYmdHms);
                yoteiDao.upsert(entity);
            }

            // 明細（束）を反映（既存があれば更新、無ければ追加）
            for (SyukkaMeisai bundle : data.meisai) {
                SyukkaMeisaiEntity existing = syukkaMeisaiDao.findOne(bundle.heatNo, bundle.sokuban);
                if (existing != null) {
                    syukkaMeisaiDao.updateFromReceive(
                            bundle.heatNo,
                            bundle.sokuban,
                            bundle.syukkaSashizuNo,
                            bundle.bundleNo,
                            bundle.jyuryo,
                            bundle.bookingNo
                    );
                } else {
                    SyukkaMeisaiEntity entity = new SyukkaMeisaiEntity();
                    entity.heatNo = bundle.heatNo;
                    entity.sokuban = bundle.sokuban;
                    entity.syukkaSashizuNo = bundle.syukkaSashizuNo;
                    entity.bundleNo = bundle.bundleNo;
                    entity.jyuryo = bundle.jyuryo;
                    entity.bookingNo = bundle.bookingNo;
                    syukkaMeisaiDao.insert(entity);
                }
            }

            // システムテーブルへ受信日時を反映
            String now = formatDbDate(new Date());
            if (systemDao.updateDataSync(SYSTEM_RENBAN, now, now, "DataSync#receiveSyukkaData", now) == 0) {
                // 更新対象が無い場合は新規作成（upsert）
                SystemEntity system = new SystemEntity();
                system.renban = SYSTEM_RENBAN;
                system.dataConfYmdhms = now;
                system.dataRecvYmdhms = now;
                system.updateProcName = "DataSync#receiveSyukkaData";
                system.updateYmd = now;
                systemDao.upsert(system);
            }
        });
    }

    //================================================================
    //　機　能　:　照合データを受信してDBへ反映する（トランザクション）
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void receiveSyougoData() throws Exception {
        // サービスから照合データを取得
        SyougoData data = svcWrapper.getSyougoData();
        if (data == null) {
            throw new IllegalStateException("照合データの取得に失敗しました");
        }

        // DB更新は一括トランザクションで整合性を保つ
        db.runInTransaction(() -> {
            // 既存データを全削除して入れ替え
            kakuninContainerDao.deleteAll();
            kakuninMeisaiDao.deleteAll();

            // データが空なら終了（ここまでの削除は仕様として受け入れる想定）
            if (data.syougoHeader.isEmpty()) {
                return;
            }

            String now = formatDbDate(new Date());

            // ヘッダ（コンテナ）を反映
            for (SyougoHeader header : data.syougoHeader) {
                KakuninContainerEntity entity = new KakuninContainerEntity();
                entity.containerId = header.containerID;
                entity.containerNo = header.containerNo;
                entity.bundleCnt = header.bundleCnt;
                entity.sagyouYmd = formatDbDate(header.sagyouYMD);
                entity.containerSyougoKanryo = header.syogoKanryo;
                entity.dataSendYmdhms = null;
                entity.insertProcName = "ReceiveSyougoData";
                entity.insertYmd = now;
                kakuninContainerDao.upsert(entity);
            }

            // 明細（束）を反映
            for (SyougoDtl detail : data.syogoDtl) {
                KakuninMeisaiEntity entity = new KakuninMeisaiEntity();
                entity.heatNo = detail.syogoDtlheatNo;
                entity.sokuban = detail.syogoDtlsokuban;
                entity.syukkaSashizuNo = detail.syougoDtlsyukkaSashizuNo;
                entity.bundleNo = detail.syougoDtlbundleNo;
                entity.jyuryo = detail.syougoDtljyuryo;
                entity.containerId = detail.syougoDtlcontainerID;

                // 受信直後は未確認（UI操作等で更新される想定）
                entity.containerSyougoKakunin = false;

                entity.insertProcName = "ReceiveSyougoData";
                entity.insertYmd = now;
                kakuninMeisaiDao.upsert(entity);
            }
        });
    }

    //================================================================
    //　機　能　:　送信用に画像データを取得する（存在しない場合はnull）
    //　引　数　:　containerId ..... int
    //　　　　　:　imgType ..... ImageType
    //　戻り値　:　[byte[]] ..... JPEGバイト配列（取得できない場合はnull）
    //================================================================
    private byte[] getPicture(int containerId, ImageType imgType) {
        // 画像ファイルを特定
        File file = getImageFile(containerId, imgType);
        if (file == null || !file.exists()) {
            return null;
        }

        // サイズ制限に収まるように縮小／回転補正／圧縮する
        try {
            return downscaleJpegIfNeeded(file, 700 * 1024, 1280, 80);

        } catch (IOException ex) {
            Log.e(TAG, "Image read failed: " + file.getAbsolutePath(), ex);
            return null;
        }
    }

    //================================================================
    //　機　能　:　JPEG画像を容量・サイズ制限に合わせて縮小し、必要なら回転補正する
    //　引　数　:　file ..... File
    //　　　　　:　maxBytes ..... int（最大バイト数）
    //　　　　　:　maxEdge ..... int（最大辺ピクセル）
    //　　　　　:　startQuality ..... int（初期圧縮品質）
    //　戻り値　:　[byte[]] ..... 変換後JPEGバイト配列
    //================================================================
    private byte[] downscaleJpegIfNeeded(File file, int maxBytes, int maxEdge, int startQuality)
            throws IOException {

        // ファイルサイズとExif回転角度を取得
        long length = file.length();
        int rotationDegrees = readExifRotation(file);

        // 容量が既に上限以下の場合
        if (length <= maxBytes) {
            // 回転不要ならファイルをそのまま読み込んで返す（変換コストを避ける）
            if (rotationDegrees == 0) {
                try (FileInputStream stream = new FileInputStream(file)) {
                    byte[] buffer = new byte[(int) length];
                    int read = stream.read(buffer);
                    if (read < 0) {
                        return null;
                    }
                    return buffer;
                }
            }

            // 回転が必要な場合はBitmap化して回転→圧縮して返す
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap == null) {
                return null;
            }

            Bitmap rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees);
            byte[] out = compressBitmap(rotated, maxBytes, startQuality);

            // メモリ解放
            rotated.recycle();
            return out;
        }

        // 容量が大きい場合：まず画像サイズを見て適切なinSampleSizeを決める
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        // 最大辺がmaxEdge以下になるようにサンプルを2倍刻みで上げる
        int sample = 1;
        int width = Math.max(bounds.outWidth, 1);
        int height = Math.max(bounds.outHeight, 1);
        int longest = Math.max(width, height);
        while (longest / sample > maxEdge) {
            sample *= 2;
        }

        // 指定サンプルでデコード（縮小読み込み）
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (bitmap == null) {
            return null;
        }

        // Exif回転を反映し、圧縮して最大バイト数に収める
        Bitmap rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees);
        byte[] out = compressBitmap(rotated, maxBytes, startQuality);

        // メモリ解放
        rotated.recycle();
        return out;
    }

    //================================================================
    //　機　能　:　Exifから回転角度を取得する（回転不要なら0）
    //　引　数　:　file ..... File
    //　戻り値　:　[int] ..... 回転角度（0/90/180/270）
    //================================================================
    private int readExifRotation(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            // Orientation値を角度へ変換
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
        } catch (IOException ex) {
            // Exif読み込み失敗時は回転なし扱いで継続
            Log.w(TAG, "Exif read failed: " + file.getAbsolutePath(), ex);
        }
        return 0;
    }

    //================================================================
    //　機　能　:　必要に応じてBitmapを回転する（回転不要なら入力を返す）
    //　引　数　:　bitmap ..... Bitmap
    //　　　　　:　degrees ..... int
    //　戻り値　:　[Bitmap] ..... 回転後Bitmap（回転不要なら入力のまま）
    //================================================================
    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, int degrees) {
        // 回転不要ならそのまま返す
        if (degrees == 0) {
            return bitmap;
        }

        // 回転行列を生成
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        // 回転後Bitmapを生成
        Bitmap rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );

        // createBitmapで別インスタンスになった場合は元Bitmapを解放
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    //================================================================
    //　機　能　:　BitmapをJPEG圧縮し、最大サイズまで品質を落として調整する
    //　引　数　:　bitmap ..... Bitmap
    //　　　　　:　maxBytes ..... int
    //　　　　　:　startQuality ..... int
    //　戻り値　:　[byte[]] ..... 圧縮後JPEGバイト配列
    //================================================================
    private byte[] compressBitmap(Bitmap bitmap, int maxBytes, int startQuality) {
        int quality = startQuality;
        byte[] out;

        // サイズが収まるまで品質を落として繰り返し圧縮（下限40）
        do {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            out = baos.toByteArray();
            quality -= 10;
        } while (out.length > maxBytes && quality >= 40);

        // 最終的に収まらない場合は警告ログ（送信側で弾かれる可能性）
        if (out.length > maxBytes) {
            Log.w(TAG, "Image still oversized after compression: " + out.length + " bytes");
        }
        return out;
    }

    //================================================================
    //　機　能　:　画像ファイルを削除する
    //　引　数　:　containerId ..... int
    //　　　　　:　imgType ..... ImageType
    //　戻り値　:　[void] ..... なし
    //================================================================
    private void deletePicture(int containerId, ImageType imgType) {
        // 対象ファイルを特定
        File file = getImageFile(containerId, imgType);

        // 削除できない場合はログのみ（致命ではない）
        if (file != null && file.exists() && !file.delete()) {
            Log.w(TAG, "Image delete failed: " + file.getAbsolutePath());
        }
    }

    //================================================================
    //　機　能　:　画像ファイルを取得する（containerIdと種別からファイル名を生成）
    //　引　数　:　containerId ..... int
    //　　　　　:　imgType ..... ImageType
    //　戻り値　:　[File] ..... 画像ファイル（ディレクトリ未確定ならnull）
    //================================================================
    private File getImageFile(int containerId, ImageType imgType) {
        if (imageDir == null) {
            return null;
        }
        String name = "container_" + containerId + "_" + imgType.getSuffix() + ".jpg";
        return new File(imageDir, name);
    }

    //================================================================
    //　機　能　:　画像保存ディレクトリを解決する（外部領域優先、無ければ内部領域）
    //　引　数　:　context ..... Context
    //　戻り値　:　[File] ..... 画像ディレクトリ
    //================================================================
    private File resolveImageDir(Context context) {
        // アプリ専用の外部領域（Pictures）が取れればそれを使う
        File external = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (external != null) {
            return external;
        }

        // 外部領域が使えない場合は内部領域
        return context.getFilesDir();
    }

    //================================================================
    //　機　能　:　DateをDB用日時文字列へ整形する
    //　引　数　:　date ..... Date
    //　戻り値　:　[String] ..... DB用日時文字列（dateがnullならnull）
    //================================================================
    private String formatDbDate(Date date) {
        if (date == null) {
            return null;
        }
        return dbDateFormat.format(date);
    }

    //================================================================
    //　機　能　:　DB用日時文字列をDateへ解析する（XSD形式も対応）
    //　引　数　:　value ..... String
    //　戻り値　:　[Date] ..... 解析結果（失敗時はnull）
    //================================================================
    private Date parseDbDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // XSD日時（例: 2024-01-01T12:34:56）を含む場合は専用パーサへ
            if (value.contains("T")) {
                return XsdDateTime.parse(value);
            }
            return dbDateFormat.parse(value);
        } catch (ParseException ex) {
            Log.w(TAG, "Failed to parse date: " + value, ex);
            return null;
        }
    }

    //================================================================
    //　機　能　:　日時文字列を解析する（失敗時は最小日時を返す）
    //　引　数　:　value ..... String
    //　戻り値　:　[Date] ..... 解析結果（失敗時はnew Date(0)）
    //================================================================
    private Date parseDbDateOrMin(String value) {
        Date parsed = parseDbDate(value);
        return parsed != null ? parsed : new Date(0);
    }

    //================================================================
    //　機　能　:　Integerをnull安全にintへ変換する
    //　引　数　:　value ..... Integer
    //　戻り値　:　[int] ..... nullの場合は0
    //================================================================
    private int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
