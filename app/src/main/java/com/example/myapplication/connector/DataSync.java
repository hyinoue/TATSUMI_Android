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


//=========================
//　処理概要　:　DataSyncクラス
//=========================

public class DataSync {
    private static final String TAG = "DataSync";
    private static final int SYSTEM_RENBAN = 1;

    public interface ErrorHandler {
        void onError(String message);
    }

    private final AppDatabase db;
    private final SvcHandyWrapper svcWrapper;
    private final CommHistoryDao commHistoryDao;
    private final SyukkaContainerDao syukkaContainerDao;
    private final SyukkaMeisaiDao syukkaMeisaiDao;
    private final YoteiDao yoteiDao;
    private final KakuninContainerDao kakuninContainerDao;
    private final KakuninMeisaiDao kakuninMeisaiDao;
    private final SystemDao systemDao;
    private final File imageDir;

    private final ErrorHandler errorHandler;
    private final SimpleDateFormat dbDateFormat =
            //========================================
            //　機　能　:　Simple Date Formatの処理
            //　引　数　:　HH:mm:ss" ..... "yyyy-MM-dd
            //　　　　　:　Locale.JAPAN .....
            //　戻り値　:　[new] ..... なし
            //========================================
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);

    public enum ImageType {
        CONTAINER("container"),
        SEAL("seal");

        private final String suffix;

        ImageType(String suffix) {
            this.suffix = suffix;
        }
        //==============================
        //　機　能　:　suffixを取得する
        //　引　数　:　なし
        //　戻り値　:　[String] ..... なし
        //==============================

        public String getSuffix() {
            return suffix;
        }
    }
    //==================================
    //　機　能　:　DataSyncの初期化処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[DataSync] ..... なし
    //==================================

    public DataSync(Context context) {
        this(context, AppDatabase.getInstance(context), null, null);
    }

    public DataSync(Context context, ErrorHandler errorHandler) {
        this(context, AppDatabase.getInstance(context), null, errorHandler);
    }
    //=============================================
    //　機　能　:　DataSyncの初期化処理
    //　引　数　:　context ..... Context
    //　　　　　:　db ..... AppDatabase
    //　　　　　:　svcWrapper ..... SvcHandyWrapper
    //　戻り値　:　[DataSync] ..... なし
    //=============================================

    public DataSync(Context context, AppDatabase db, SvcHandyWrapper svcWrapper) {
        this(context, db, svcWrapper, null);
    }

    public DataSync(Context context, AppDatabase db, SvcHandyWrapper svcWrapper, ErrorHandler errorHandler) {
        this.db = db;
        this.commHistoryDao = db.commHistoryDao();
        this.svcWrapper = svcWrapper != null
                ? svcWrapper
                : new SvcHandyWrapper(new SvcHandyRepository(), this.commHistoryDao);
        this.syukkaContainerDao = db.syukkaContainerDao();
        this.syukkaMeisaiDao = db.syukkaMeisaiDao();
        this.yoteiDao = db.yoteiDao();
        this.kakuninContainerDao = db.kakuninContainerDao();
        this.kakuninMeisaiDao = db.kakuninMeisaiDao();
        this.systemDao = db.systemDao();
        this.imageDir = resolveImageDir(context);
        this.errorHandler = errorHandler;
    }

    //=============================
    //　機　能　:　syukka Onlyを送信する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================
    public void sendSyukkaOnly() throws Exception {
        Date sagyouYmd = sagyouYotei();
        dataSousinAll(sagyouYmd);
    }

    //=============================
    //　機　能　:　syougo Onlyを送信する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=============================
    public void sendSyougoOnly() {
        dataSousinSyougo();
    }

    //============================
    //　機　能　:　run Syncの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public boolean runSync() {
        historyDel();

        boolean hasError = false;
        Date sagyouYmd = null;
        try {
            sagyouYmd = sagyouYotei();
        } catch (Exception ex) {
            hasError = true;
            reportError(ex);
            reportError("作業予定が登録されていません");
        }

        if (sagyouYmd != null) {
            try {
                dataSousinAll(sagyouYmd);
            } catch (Exception ex) {
                hasError = true;
                reportError(ex);
            }
        }

        dataSousinSyougo();

        if (sagyouYmd != null) {
            try {
                receiveSyukkaData(sagyouYmd);
                dataUpdate(sagyouYmd);
            } catch (Exception ex) {
                hasError = true;
                reportError(ex);
            }
        }

        try {
            receiveSyougoData();
        } catch (Exception ex) {
            hasError = true;
            reportError(ex);
        }
        return !hasError;
    }

    private void reportError(Exception ex) {
        Log.e(TAG, "DataSync failed", ex);
        String msg = ex.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = ex.getClass().getSimpleName();
        }
        reportError(msg);
    }

    private void reportError(String msg) {
        if (errorHandler != null && msg != null && !msg.trim().isEmpty()) {
            errorHandler.onError(msg);
        }
    }

    //============================
    //　機　能　:　sagyou Yoteiの処理
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... なし
    //============================
    private Date sagyouYotei() throws Exception {
        YoteiEntity existing = yoteiDao.findFirst();
        if (existing != null && existing.sagyouYoteiYmd != null) {
            Date parsed = parseDbDate(existing.sagyouYoteiYmd);
            if (parsed != null) {
                return parsed;
            }
        }

        Date sagyouYmd = svcWrapper.getSagyouYmd();
        if (sagyouYmd == null) {
            throw new IllegalStateException("作業予定が登録されていません");
        }
        return sagyouYmd;
    }

    //============================
    //　機　能　:　history Delの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    private void historyDel() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        commHistoryDao.deleteBefore(formatDbDate(cal.getTime()));
    }

    //=================================
    //　機　能　:　data Sousin Allの処理
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[void] ..... なし
    //=================================
    private void dataSousinAll(Date sagyouYmd) {
        List<SyukkaContainerEntity> containers = syukkaContainerDao.findUnsent();
        for (SyukkaContainerEntity container : containers) {
            if (!dataSousinOnce(container, sagyouYmd)) {
                break;
            }
        }
    }

    //==================================================
    //　機　能　:　data Sousin Onceの処理
    //　引　数　:　container ..... SyukkaContainerEntity
    //　　　　　:　sagyouYmd ..... Date
    //　戻り値　:　[boolean] ..... なし
    //==================================================
    private boolean dataSousinOnce(SyukkaContainerEntity container, Date sagyouYmd) {
        try {
            if (container.containerId == null) {
                Log.w(TAG, "Container ID is null; skip send. containerNo="
                        + formatContainerNo(container.containerNo));
                return false;
            }
            BunningData data = new BunningData();
            data.syukkaYmd = sagyouYmd;
            data.containerNo = container.containerNo;
            data.containerJyuryo = intOrZero(container.containerJyuryo);
            data.dunnageJyuryo = intOrZero(container.dunnageJyuryo);
            data.sealNo = container.sealNo;
            data.containerPhoto = getPicture(container.containerId, ImageType.CONTAINER);
            data.sealPhoto = getPicture(container.containerId, ImageType.SEAL);

            List<SyukkaMeisaiEntity> detailRows =
                    syukkaMeisaiDao.findByContainerId(container.containerId);

            String fallbackBookingNo = container.bookingNo != null
                    ? container.bookingNo.trim()
                    : "";
            int missingBookingCount = 0;
            String missingBookingSample = "";

            for (SyukkaMeisaiEntity row : detailRows) {
                SyukkaMeisai detail = new SyukkaMeisai();
                detail.heatNo = row.heatNo;
                detail.sokuban = row.sokuban;
                String bookingNo = row.bookingNo != null ? row.bookingNo.trim() : "";
                if (bookingNo.isEmpty()) {
                    bookingNo = fallbackBookingNo;
                }
                if (bookingNo.isEmpty()) {
                    missingBookingCount++;
                    if (missingBookingSample.isEmpty()) {
                        missingBookingSample = row.heatNo + "/" + row.sokuban;
                    }
                }
                detail.bookingNo = bookingNo;
                detail.bundleNo = row.bundleNo;
                detail.syukkaSashizuNo = row.syukkaSashizuNo;
                detail.jyuryo = intOrZero(row.jyuryo);
                data.bundles.add(detail);
            }

            if (data.bundles.isEmpty()) {
                Log.w(TAG, "No bundle details; skip SendSyukkaData. containerId="
                        + container.containerId + " containerNo="
                        + formatContainerNo(container.containerNo));
                return false;
            }
            if (missingBookingCount > 0) {
                Log.w(TAG, "BookingNo missing in bundle details; skip SendSyukkaData. containerId="
                        + container.containerId + " containerNo="
                        + formatContainerNo(container.containerNo)
                        + " missingCount=" + missingBookingCount
                        + " sampleHeatSokuban=" + missingBookingSample);
                return false;
            }


            if (svcWrapper.sendSyukkaData(data)) {
                String now = formatDbDate(new Date());
                syukkaContainerDao.markSent(container.containerId, now);
                deletePicture(container.containerId, ImageType.CONTAINER);
                deletePicture(container.containerId, ImageType.SEAL);
                return true;
            }
            Log.w(TAG, "SendSyukkaDataResult=false. containerId=" + container.containerId
                    + " containerNo=" + formatContainerNo(container.containerNo)
                    + " bundleCount=" + data.bundles.size());
            return false;
        } catch (Exception ex) {
            Log.e(TAG, "DataSousinOnce failed", ex);
            return false;
        }
    }

    //=====================================
    //　機　能　:　container Noを整形する
    //　引　数　:　containerNo ..... String
    //　戻り値　:　[String] ..... なし
    //=====================================
    private String formatContainerNo(String containerNo) {
        if (containerNo == null || containerNo.trim().isEmpty()) {
            return "<empty>";
        }
        return containerNo.trim();
    }

    //==================================
    //　機　能　:　data Sousin Syougoの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==================================
    private void dataSousinSyougo() {
        List<KakuninContainerEntity> containers = kakuninContainerDao.findUnsentCompleted();
        for (KakuninContainerEntity container : containers) {
            if (!dataSousinSyougoOnce(container)) {
                break;
            }
        }
    }

    //===================================================
    //　機　能　:　data Sousin Syougo Onceの処理
    //　引　数　:　container ..... KakuninContainerEntity
    //　戻り値　:　[boolean] ..... なし
    //===================================================
    private boolean dataSousinSyougoOnce(KakuninContainerEntity container) {
        try {
            CollateData collateData = new CollateData();
            collateData.containerID = container.containerId;
            collateData.syogoKanryo = Boolean.TRUE.equals(container.containerSyougoKanryo);

            List<KakuninMeisaiEntity> detailRows =
                    kakuninMeisaiDao.findByContainerId(container.containerId);
            for (KakuninMeisaiEntity row : detailRows) {
                CollateDtl detail = new CollateDtl();
                detail.collateDtlheatNo = row.heatNo;
                detail.collateDtlsokuban = row.sokuban;
                detail.collateDtlsyougoKakunin = Boolean.TRUE.equals(row.containerSyougoKakunin);
                collateData.collateDtls.add(detail);
            }

            if (svcWrapper.sendSyougoData(collateData)) {
                String now = formatDbDate(new Date());
                kakuninContainerDao.markSent(container.containerId, now);
                return true;
            }
            Log.w(TAG, "SendSyougoDataResult=false");
            return false;
        } catch (Exception ex) {
            Log.e(TAG, "DataSousinSyougoOnce failed", ex);
            return false;
        }
    }

    //=================================
    //　機　能　:　data Updateの処理
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[void] ..... なし
    //=================================
    private void dataUpdate(Date sagyouYmd) {
        List<YoteiEntity> candidates = yoteiDao.findWithNullLastUpd();
        if (candidates.isEmpty()) {
            return;
        }

        try {
            svcWrapper.getSagyouYmd();
        } catch (Exception ex) {
            Log.e(TAG, "GetSagyouYmd failed", ex);
        }

        Date lastUpd = parseDbDateOrMin(candidates.get(0).lastUpdYmdhms);
        Date getUpdate;
        try {
            getUpdate = svcWrapper.getUpdateYmdHms(sagyouYmd);
        } catch (Exception ex) {
            Log.e(TAG, "GetUpdateYmdHms failed", ex);
            return;
        }

        if (getUpdate != null && lastUpd != null && getUpdate.after(lastUpd)) {
            return;
        }

        String now = formatDbDate(new Date());
        if (systemDao.updateDataConf(SYSTEM_RENBAN, now) == 0) {
            SystemEntity system = new SystemEntity();
            system.renban = SYSTEM_RENBAN;
            system.dataConfYmdhms = now;
            systemDao.upsert(system);
        }
    }

    //===================================
    //　機　能　:　receive Syukka Dataの処理
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[void] ..... なし
    //===================================
    private void receiveSyukkaData(Date sagyouYmd) throws Exception {

        try {
            svcWrapper.getSagyouYmd();
        } catch (Exception ex) {
            Log.e(TAG, "GetSagyouYmd failed", ex);
        }
        SyukkaData data = svcWrapper.getSyukkaData(sagyouYmd);
        if (data == null) {
            throw new IllegalStateException("出荷データの取得に失敗しました");
        }

        db.runInTransaction(() -> {
            yoteiDao.deleteAll();
            syukkaMeisaiDao.deleteSentLinked();
            syukkaContainerDao.deleteSent();

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

            String now = formatDbDate(new Date());
            if (systemDao.updateDataSync(SYSTEM_RENBAN, now, now) == 0) {
                SystemEntity system = new SystemEntity();
                system.renban = SYSTEM_RENBAN;
                system.dataConfYmdhms = now;
                system.dataRecvYmdhms = now;
                systemDao.upsert(system);
            }
        });
    }

    //===================================
    //　機　能　:　receive Syougo Dataの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //===================================
    private void receiveSyougoData() throws Exception {
        SyougoData data = svcWrapper.getSyougoData();
        if (data == null) {
            throw new IllegalStateException("照合データの取得に失敗しました");
        }

        db.runInTransaction(() -> {
            kakuninContainerDao.deleteAll();
            kakuninMeisaiDao.deleteAll();

            if (data.syougoHeader.isEmpty()) {
                return;
            }

            String now = formatDbDate(new Date());
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

            for (SyougoDtl detail : data.syogoDtl) {
                KakuninMeisaiEntity entity = new KakuninMeisaiEntity();
                entity.heatNo = detail.syogoDtlheatNo;
                entity.sokuban = detail.syogoDtlsokuban;
                entity.syukkaSashizuNo = detail.syougoDtlsyukkaSashizuNo;
                entity.bundleNo = detail.syougoDtlbundleNo;
                entity.jyuryo = detail.syougoDtljyuryo;
                entity.containerId = detail.syougoDtlcontainerID;
                entity.containerSyougoKakunin = false;
                entity.insertProcName = "ReceiveSyougoData";
                entity.insertYmd = now;
                kakuninMeisaiDao.upsert(entity);
            }
        });
    }

    //====================================
    //　機　能　:　pictureを取得する
    //　引　数　:　containerId ..... int
    //　　　　　:　imgType ..... ImageType
    //　戻り値　:　[byte[]] ..... なし
    //====================================
    private byte[] getPicture(int containerId, ImageType imgType) {
        File file = getImageFile(containerId, imgType);
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            return downscaleJpegIfNeeded(file, 700 * 1024, 1280, 80);

        } catch (IOException ex) {
            Log.e(TAG, "Image read failed: " + file.getAbsolutePath(), ex);
            return null;
        }
    }

    //========================================
    //　機　能　:　downscale Jpeg If Neededの処理
    //　引　数　:　file ..... File
    //　　　　　:　maxBytes ..... int
    //　　　　　:　maxEdge ..... int
    //　　　　　:　startQuality ..... int
    //　戻り値　:　[byte[]] ..... なし
    //========================================
    private byte[] downscaleJpegIfNeeded(File file, int maxBytes, int maxEdge, int startQuality)
            throws IOException {
        long length = file.length();
        int rotationDegrees = readExifRotation(file);
        if (length <= maxBytes) {
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
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap == null) {
                return null;
            }
            Bitmap rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees);
            byte[] out = compressBitmap(rotated, maxBytes, startQuality);
            rotated.recycle();
            return out;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        int sample = 1;
        int width = Math.max(bounds.outWidth, 1);
        int height = Math.max(bounds.outHeight, 1);
        int longest = Math.max(width, height);
        while (longest / sample > maxEdge) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (bitmap == null) {
            return null;
        }

        Bitmap rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees);
        byte[] out = compressBitmap(rotated, maxBytes, startQuality);
        rotated.recycle();
        return out;
    }

    //==================================
    //　機　能　:　read Exif Rotationの処理
    //　引　数　:　file ..... File
    //　戻り値　:　[int] ..... なし
    //==================================
    private int readExifRotation(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
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
            Log.w(TAG, "Exif read failed: " + file.getAbsolutePath(), ex);
        }
        return 0;
    }

    //=======================================
    //　機　能　:　rotate Bitmap If Neededの処理
    //　引　数　:　bitmap ..... Bitmap
    //　　　　　:　degrees ..... int
    //　戻り値　:　[Bitmap] ..... なし
    //=======================================
    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, int degrees) {
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    //===================================
    //　機　能　:　compress Bitmapの処理
    //　引　数　:　bitmap ..... Bitmap
    //　　　　　:　maxBytes ..... int
    //　　　　　:　startQuality ..... int
    //　戻り値　:　[byte[]] ..... なし
    //===================================
    private byte[] compressBitmap(Bitmap bitmap, int maxBytes, int startQuality) {
        int quality = startQuality;
        byte[] out;
        do {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            out = baos.toByteArray();
            quality -= 10;
        } while (out.length > maxBytes && quality >= 40);
        if (out.length > maxBytes) {
            Log.w(TAG, "Image still oversized after compression: " + out.length + " bytes");
        }
        return out;
    }

    //====================================
    //　機　能　:　pictureを削除する
    //　引　数　:　containerId ..... int
    //　　　　　:　imgType ..... ImageType
    //　戻り値　:　[void] ..... なし
    //====================================
    private void deletePicture(int containerId, ImageType imgType) {
        File file = getImageFile(containerId, imgType);
        if (file != null && file.exists() && !file.delete()) {
            Log.w(TAG, "Image delete failed: " + file.getAbsolutePath());
        }
    }

    //====================================
    //　機　能　:　image Fileを取得する
    //　引　数　:　containerId ..... int
    //　　　　　:　imgType ..... ImageType
    //　戻り値　:　[File] ..... なし
    //====================================
    private File getImageFile(int containerId, ImageType imgType) {
        if (imageDir == null) {
            return null;
        }
        String name = "container_" + containerId + "_" + imgType.getSuffix() + ".jpg";
        return new File(imageDir, name);
    }

    //==================================
    //　機　能　:　resolve Image Dirの処理
    //　引　数　:　context ..... Context
    //　戻り値　:　[File] ..... なし
    //==================================
    private File resolveImageDir(Context context) {
        File external = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (external != null) {
            return external;
        }
        return context.getFilesDir();
    }

    //==============================
    //　機　能　:　db Dateを整形する
    //　引　数　:　date ..... Date
    //　戻り値　:　[String] ..... なし
    //==============================
    private String formatDbDate(Date date) {
        if (date == null) {
            return null;
        }
        return dbDateFormat.format(date);
    }

    //===============================
    //　機　能　:　db Dateを解析する
    //　引　数　:　value ..... String
    //　戻り値　:　[Date] ..... なし
    //===============================
    private Date parseDbDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            if (value.contains("T")) {
                return XsdDateTime.parse(value);
            }
            return dbDateFormat.parse(value);
        } catch (ParseException ex) {
            Log.w(TAG, "Failed to parse date: " + value, ex);
            return null;
        }
    }

    //================================
    //　機　能　:　db Date Or Minを解析する
    //　引　数　:　value ..... String
    //　戻り値　:　[Date] ..... なし
    //================================
    private Date parseDbDateOrMin(String value) {
        Date parsed = parseDbDate(value);
        return parsed != null ? parsed : new Date(0);
    }

    //================================
    //　機　能　:　int Or Zeroの処理
    //　引　数　:　value ..... Integer
    //　戻り値　:　[int] ..... なし
    //================================
    private int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
