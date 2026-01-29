package com.example.myapplication.connector;

import android.content.Context;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataSync {
    private static final String TAG = "DataSync";
    private static final int SYSTEM_RENBAN = 1;

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
    private final SimpleDateFormat dbDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);

    public enum ImageType {
        CONTAINER("container"),
        SEAL("seal");

        private final String suffix;

        ImageType(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    public DataSync(Context context) {
        this(context, AppDatabase.getInstance(context), new SvcHandyWrapper());
    }

    public DataSync(Context context, AppDatabase db, SvcHandyWrapper svcWrapper) {
        this.db = db;
        this.svcWrapper = svcWrapper;
        this.commHistoryDao = db.commHistoryDao();
        this.syukkaContainerDao = db.syukkaContainerDao();
        this.syukkaMeisaiDao = db.syukkaMeisaiDao();
        this.yoteiDao = db.yoteiDao();
        this.kakuninContainerDao = db.kakuninContainerDao();
        this.kakuninMeisaiDao = db.kakuninMeisaiDao();
        this.systemDao = db.systemDao();
        this.imageDir = resolveImageDir(context);
    }

    public void sendSyukkaOnly() throws Exception {
        Date sagyouYmd = sagyouYotei();
        dataSousinAll(sagyouYmd);
    }

    public void sendSyougoOnly() {
        dataSousinSyougo();
    }
    
    public void runSync() throws Exception {
        try {
            historyDel();
            Date sagyouYmd = sagyouYotei();
            dataSousinAll(sagyouYmd);
            dataSousinSyougo();
            if (sagyouYmd != null) {
                receiveSyukkaData(sagyouYmd);
                dataUpdate(sagyouYmd);
            }
            receiveSyougoData();
        } catch (Exception ex) {
            Log.e(TAG, "DataSync failed", ex);
            throw ex;
        }
    }

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

    private void historyDel() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        commHistoryDao.deleteBefore(formatDbDate(cal.getTime()));
    }

    private void dataSousinAll(Date sagyouYmd) {
        List<SyukkaContainerEntity> containers = syukkaContainerDao.findUnsent();
        for (SyukkaContainerEntity container : containers) {
            if (!dataSousinOnce(container, sagyouYmd)) {
                break;
            }
        }
    }

    private boolean dataSousinOnce(SyukkaContainerEntity container, Date sagyouYmd) {
        try {
            if (container.containerId == null) {
                Log.w(TAG, "Container ID is null; skip send");
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
            for (SyukkaMeisaiEntity row : detailRows) {
                SyukkaMeisai detail = new SyukkaMeisai();
                detail.heatNo = row.heatNo;
                detail.sokuban = row.sokuban;
                detail.bookingNo = row.bookingNo;
                detail.bundleNo = row.bundleNo;
                detail.syukkaSashizuNo = row.syukkaSashizuNo;
                detail.jyuryo = intOrZero(row.jyuryo);
                data.bundles.add(detail);
            }

            if (svcWrapper.sendSyukkaData(data)) {
                String now = formatDbDate(new Date());
                syukkaContainerDao.markSent(container.containerId, now);
                deletePicture(container.containerId, ImageType.CONTAINER);
                deletePicture(container.containerId, ImageType.SEAL);
                return true;
            }
            Log.w(TAG, "SendSyukkaDataResult=false");
            return false;
        } catch (Exception ex) {
            Log.e(TAG, "DataSousinOnce failed", ex);
            return false;
        }
    }

    private void dataSousinSyougo() {
        List<KakuninContainerEntity> containers = kakuninContainerDao.findUnsentCompleted();
        for (KakuninContainerEntity container : containers) {
            if (!dataSousinSyougoOnce(container)) {
                break;
            }
        }
    }

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

    private void dataUpdate(Date sagyouYmd) {
        List<YoteiEntity> candidates = yoteiDao.findWithNullLastUpd();
        if (candidates.isEmpty()) {
            return;
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

    private void receiveSyukkaData(Date sagyouYmd) throws Exception {
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

    private byte[] getPicture(int containerId, ImageType imgType) {
        File file = getImageFile(containerId, imgType);
        if (file == null || !file.exists()) {
            return null;
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int read = stream.read(buffer);
            if (read < 0) {
                return null;
            }
            return buffer;
        } catch (IOException ex) {
            Log.e(TAG, "Image read failed: " + file.getAbsolutePath(), ex);
            return null;
        }
    }

    private void deletePicture(int containerId, ImageType imgType) {
        File file = getImageFile(containerId, imgType);
        if (file != null && file.exists() && !file.delete()) {
            Log.w(TAG, "Image delete failed: " + file.getAbsolutePath());
        }
    }

    private File getImageFile(int containerId, ImageType imgType) {
        if (imageDir == null) {
            return null;
        }
        String name = "container_" + containerId + "_" + imgType.getSuffix() + ".jpg";
        return new File(imageDir, name);
    }

    private File resolveImageDir(Context context) {
        File external = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (external != null) {
            return external;
        }
        return context.getFilesDir();
    }

    private String formatDbDate(Date date) {
        if (date == null) {
            return null;
        }
        return dbDateFormat.format(date);
    }

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

    private Date parseDbDateOrMin(String value) {
        Date parsed = parseDbDate(value);
        return parsed != null ? parsed : new Date(0);
    }

    private int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
