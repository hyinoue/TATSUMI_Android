package com.example.myapplication.grid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.db.dao.SyukkaMeisaiDao;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.entity.SyukkaMeisaiEntity;
import com.example.myapplication.db.entity.SyukkaMeisaiWorkEntity;
import com.example.myapplication.model.BundleInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;


/**
 * C# BundleSelectController を Room で再現（Android/Java版）
 * <p>
 * 役割（C#同等）：
 * - 保持データ（heatNo+sokuban → BundleInfo）
 * - CheckBundle（存在/重量超過/出荷済/20行/予約No相違）
 * - AddBundleNo（bundleNo空のものだけ更新）
 * - AddBundle（DB→保持→Work反映→表示行生成）
 * - RemoveBundle/DeleteBundles（Work削除→保持削除→表示更新）
 * - JyuryoSum / Bundles
 * - Normalモード起動時に Work → List 復元
 */
public class BundleSelectController {

    public enum Mode {Normal, JyuryoCalc}

    private final SyukkaMeisaiDao syukkaMeisaiDao;
    private final SyukkaMeisaiWorkDao syukkaMeisaiWorkDao;
    private final Mode mode;

    // C#：Dictionary<string, BundleInfo>（順序が欲しいので LinkedHashMap）
    private final LinkedHashMap<String, BundleInfo> dataList = new LinkedHashMap<>();

    // 一覧表示用（RecyclerViewに渡す）
    private final List<BundleGridRow> displayRows = new ArrayList<>();

    private static final int MAX_ROWS = 20;

    public BundleSelectController(@NonNull SyukkaMeisaiDao syukkaMeisaiDao,
                                  @NonNull SyukkaMeisaiWorkDao syukkaMeisaiWorkDao,
                                  @NonNull Mode mode) {
        this.syukkaMeisaiDao = syukkaMeisaiDao;
        this.syukkaMeisaiWorkDao = syukkaMeisaiWorkDao;
        this.mode = mode;

        if (mode == Mode.Normal) {
            readWorkTblToList();
        }
        refreshDisplayRows();
    }

    // ============================================================
    // 公開：保持データ・表示データ
    // ============================================================

    @NonNull
    public LinkedHashMap<String, BundleInfo> getBundles() {
        return dataList;
    }

    /**
     * C#：JyuryoSum
     */
    public int getJyuryoSum() {
        int sum = 0;
        for (BundleInfo b : dataList.values()) sum += b.jyuryo;
        return sum;
    }

    /**
     * RecyclerView表示用
     */
    @NonNull
    public List<BundleGridRow> getDisplayRows() {
        return Collections.unmodifiableList(displayRows);
    }

    // ============================================================
    // C#：CheckBundle
    // ============================================================

    /**
     * @return エラーなしなら ""（C#互換）
     */
    @NonNull
    public String checkBundle(@NonNull String heatNo,
                              @NonNull String sokuban,
                              int containerJyuryo,
                              int dunnageJyuryo,
                              int maxContainerJyuryo) {

        // 既に一覧に存在する場合
        if (dataList.containsKey(keyOf(heatNo, sokuban))) {
            return "既に読み込み済みです";
        }

        // 出荷束明細の存在チェック
        SyukkaMeisaiEntity e = syukkaMeisaiDao.findOneForCheck(heatNo, sokuban);
        if (e == null) {
            return "出荷束明細に存在していません";
        }

        int jyuryo = (e.jyuryo == null) ? 0 : e.jyuryo;

        // 重量超過チェック（C# tmpWt と同等）
        int tmpWt = getJyuryoSum() + containerJyuryo + dunnageJyuryo + jyuryo;
        if (tmpWt > maxContainerJyuryo) {
            return "積載重量を超過します";
        }

        // 既に出荷済（C#はCONTAINER_IDが空でない、ここでは Integer の null/非null）
        if (e.containerId != null) {
            return "既に出荷済です";
        }

        // 20行制限
        if (dataList.size() >= MAX_ROWS) {
            return "20行までしか読取できません";
        }

        // 予約No相違（先頭と比較）
        if (!dataList.isEmpty()) {
            String firstBooking = safeStr(dataList.values().iterator().next().bookingNo);
            String curBooking = safeStr(e.bookingNo);
            if (!firstBooking.equals(curBooking)) {
                return "予約№が相違しています";
            }
        }

        return "";
    }

    // ============================================================
    // C#：AddBundleNo（bundleNo空のものだけ更新）
    // ============================================================

    public void addBundleNo(@NonNull String heatNo,
                            @NonNull String sokuban,
                            @NonNull String bundleNoOrg) {
        // C#：ToInt32(bundleNoOrg).ToString().PadLeft(4,' ')
        String padded = padLeft4AsSpaces(bundleNoOrg);
        syukkaMeisaiDao.updateBundleNoIfEmpty(heatNo, sokuban, padded);
    }

    // ============================================================
    // C#：AddBundle（DB→保持→Work→表示更新）
    // ============================================================

    public void addBundle(@NonNull String heatNo, @NonNull String sokuban) {
        SyukkaMeisaiEntity e = syukkaMeisaiDao.findOneForAdd(heatNo, sokuban);
        if (e == null) {
            throw new IllegalStateException("出荷束明細に存在していません");
        }

        BundleInfo item = new BundleInfo();
        item.heatNo = safeStr(e.heatNo);
        item.sokuban = safeStr(e.sokuban);
        item.packingNo = safeStr(e.syukkaSashizuNo);
        item.bundleNo = safeStr(e.bundleNo);
        item.jyuryo = (e.jyuryo == null) ? 0 : e.jyuryo;
        item.bookingNo = safeStr(e.bookingNo);

        // C#："取消" 列（DataTableに "削除" を入れてた）に相当
        item.torikeshi = "削除";

        dataList.put(keyOf(heatNo, sokuban), item);

        if (mode == Mode.Normal) {
            addWorkTable(item);
        }

        refreshDisplayRows();
    }

    // ============================================================
    // C#：RemoveBundle(row)
    // ============================================================

    public void removeBundle(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= dataList.size()) return;

        String key = new ArrayList<>(dataList.keySet()).get(rowIndex);
        BundleInfo item = dataList.get(key);
        if (item == null) return;

        if (mode == Mode.Normal) {
            removeWorkTable(item);
        }

        dataList.remove(key);
        refreshDisplayRows();
    }

    // ============================================================
    // C#：DeleteBundles()
    // ============================================================

    public void deleteBundles() {
        while (!dataList.isEmpty()) {
            removeBundle(0);
        }
    }

    // ============================================================
    // C#：refreshDataGrid()（表示行生成）
    // ============================================================

    private void refreshDisplayRows() {
        displayRows.clear();

        for (BundleInfo item : dataList.values()) {
            String pNo = safeStr(item.packingNo);
            String bNo = safeStr(item.bundleNo);
            String idx = safeStr(item.sokuban);

            // C#：ToString("#,0").PadLeft(6,' ')
            String j = String.format(Locale.JAPAN, "%,d", item.jyuryo);
            if (j.length() < 6) j = repeat(" ", 6 - j.length()) + j;

            displayRows.add(new BundleGridRow(pNo, bNo, idx, j, "削除"));
        }
    }

    // ============================================================
    // C#：readWorkTblToList()
    // ============================================================

    private void readWorkTblToList() {
        List<SyukkaMeisaiWorkDao.WorkJoinRow> rows = syukkaMeisaiWorkDao.selectWorkJoined();
        if (rows == null) return;

        for (SyukkaMeisaiWorkDao.WorkJoinRow r : rows) {
            String heatNo = safeStr(r.heatNo);
            String sokuban = safeStr(r.sokuban);
            String k = keyOf(heatNo, sokuban);

            if (!dataList.containsKey(k)) {
                BundleInfo item = new BundleInfo();
                item.heatNo = heatNo;
                item.sokuban = sokuban;
                item.packingNo = safeStr(r.packingNo);
                item.bundleNo = safeStr(r.bundleNo);
                item.jyuryo = (r.jyuryo == null) ? 0 : r.jyuryo;
                item.bookingNo = safeStr(r.bookingNo);
                item.torikeshi = "削除";

                dataList.put(k, item);
            }
        }
    }

    // ============================================================
    // Workテーブル操作（C# addWorkTable/removeWorkTable）
    // ※ WorkEntityが少カラムなので、heat/sokuban + updateYmd だけで運用
    // ============================================================

    private void addWorkTable(@NonNull BundleInfo item) {
        SyukkaMeisaiWorkEntity w = new SyukkaMeisaiWorkEntity();
        w.heatNo = item.heatNo;
        w.sokuban = item.sokuban;

        // 必要なら持つ（今はC#互換では使ってないのでnullでOK）
        w.containerId = null;

        // 並び順用（C#のINSERT_YMD相当の役割を updateYmd で代用）
        w.updateYmd = nowAsText();

        syukkaMeisaiWorkDao.upsert(w);
    }

    private void removeWorkTable(@NonNull BundleInfo item) {
        syukkaMeisaiWorkDao.deleteOne(item.heatNo, item.sokuban);
    }

    // ============================================================
    // utils
    // ============================================================

    private String keyOf(String heatNo, String sokuban) {
        return safeStr(heatNo) + safeStr(sokuban);
    }

    private String safeStr(@Nullable String s) {
        return s == null ? "" : s;
    }

    /**
     * C#：ToInt32(bundleNoOrg).ToString().PadLeft(4,' ') 相当
     */
    private String padLeft4AsSpaces(String org) {
        int n = 0;
        try {
            String t = org == null ? "" : org.trim();
            if (!t.isEmpty()) n = Integer.parseInt(t);
        } catch (Exception ignored) {
        }
        String s = String.valueOf(n);
        if (s.length() >= 4) return s;
        return repeat(" ", 4 - s.length()) + s;
    }

    private String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private String nowAsText() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
    }
}
