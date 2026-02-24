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


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　BundleSelectController ..... Bundle選択制御（保持/チェック/追加/削除/表示更新）
//　　　　　　:　getBundles ..... 保持データの取得
//　　　　　　:　getJyuryoSum ..... 重量合計の取得
//　　　　　　:　getDisplayRows ..... 表示行データの取得
//　　　　　　:　checkBundle ..... Bundle読取チェック（存在/重量超過/出荷済/20行/予約No相違）
//　　　　　　:　addBundleNo ..... BundleNo更新（空のもののみ）
//　　　　　　:　addBundle ..... Bundle追加（DB→保持→Work→表示）
//　　　　　　:　removeBundle ..... Bundle削除（Work→保持→表示）
//　　　　　　:　deleteBundles ..... Bundle全削除
//　　　　　　:　refreshDisplayRows ..... 表示行の再生成
//　　　　　　:　readWorkTblToList ..... Workテーブルから保持データ復元
//　　　　　　:　addWorkTable ..... Workテーブルへ追加（Upsert）
//　　　　　　:　removeWorkTable ..... Workテーブルから削除
//　　　　　　:　keyOf ..... heatNo+sokuban のキー生成
//　　　　　　:　safeStr ..... null安全な文字列化
//　　　　　　:　padLeft4AsSpaces ..... 4桁右寄せスペース埋め（C#互換）
//　　　　　　:　repeat ..... 文字列繰り返し生成
//　　　　　　:　nowAsText ..... 現在時刻文字列生成（yyyyMMddHHmmss）
//============================================================

//=======================================
//　処理概要　:　BundleSelectControllerクラス
//=======================================
public class BundleSelectController {

    public enum Mode {Normal, JyuryoCalc}

    private final SyukkaMeisaiDao syukkaMeisaiDao;
    private final SyukkaMeisaiWorkDao syukkaMeisaiWorkDao;
    private final Mode mode;

    // C#：Dictionary<string, BundleInfo>（順序が必要なため LinkedHashMap を使用）
    private final LinkedHashMap<String, BundleInfo> dataList = new LinkedHashMap<>();

    // 一覧表示用（RecyclerViewに渡す）
    private final List<BundleSelectRow> displayRows = new ArrayList<>();

    private static final int MAX_ROWS = 20;

    //==========================================================
    //　機　能　:　BundleSelectControllerの初期化処理
    //　引　数　:　syukkaMeisaiDao ..... SyukkaMeisaiDao
    //　　　　　:　syukkaMeisaiWorkDao ..... SyukkaMeisaiWorkDao
    //　　　　　:　mode ..... Mode
    //　戻り値　:　[BundleSelectController] ..... なし
    //==========================================================
    public BundleSelectController(@NonNull SyukkaMeisaiDao syukkaMeisaiDao,
                                  @NonNull SyukkaMeisaiWorkDao syukkaMeisaiWorkDao,
                                  @NonNull Mode mode) {
        this.syukkaMeisaiDao = syukkaMeisaiDao;
        this.syukkaMeisaiWorkDao = syukkaMeisaiWorkDao;
        this.mode = mode;

        // Normalモードの場合、Workテーブルから読取済みBundleを復元する
        if (mode == Mode.Normal) {
            readWorkTblToList();
        }

        // 初期表示用の行データを作成
        refreshDisplayRows();
    }

    // ============================================================
    // 公開：保持データ・表示データ
    // ============================================================

    //=================================
    //　機　能　:　bundlesを取得する
    //　引　数　:　なし
    //　戻り値　:　[LinkedHashMap<String, BundleInfo>] ..... 保持データ
    //=================================
    @NonNull
    public LinkedHashMap<String, BundleInfo> getBundles() {
        return dataList;
    }

    //============================
    //　機　能　:　jyuryo Sumを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 重量合計
    //============================
    public int getJyuryoSum() {
        int sum = 0;

        // 保持中のBundleを走査し、重量を加算
        for (BundleInfo b : dataList.values()) {
            sum += b.jyuryo;
        }

        return sum;
    }

    //===========================================
    //　機　能　:　display Rowsを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<BundleSelectRow>] ..... 表示用行データ（読み取り専用）
    //===========================================
    @NonNull
    public List<BundleSelectRow> getDisplayRows() {
        // 外部から改変されないよう unmodifiableList を返す
        return Collections.unmodifiableList(displayRows);
    }

    // ============================================================
    // C#：CheckBundle
    // ============================================================

    //=========================================
    //　機　能　:　check Bundleの処理
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　　　　　:　containerJyuryo ..... int
    //　　　　　:　dunnageJyuryo ..... int
    //　　　　　:　maxContainerJyuryo ..... int
    //　戻り値　:　[String] ..... エラーメッセージ（問題なしは ""）
    //=========================================
    @NonNull
    public String checkBundle(@NonNull String heatNo,
                              @NonNull String sokuban,
                              int containerJyuryo,
                              int dunnageJyuryo,
                              int maxContainerJyuryo) {

        // キー（heatNo+sokuban）を作成
        String key = keyOf(heatNo, sokuban);

        // 既に一覧に存在する場合
        if (dataList.containsKey(key)) {
            return "既に読み込み済みです";
        }

        // 出荷束明細の存在チェック（読取対象がDBに存在するか）
        SyukkaMeisaiEntity e = syukkaMeisaiDao.findOneForCheck(heatNo, sokuban);
        if (e == null) {
            return "出荷束明細に存在していません";
        }

        // DBの重量（nullは0扱い）
        int jyuryo = (e.jyuryo == null) ? 0 : e.jyuryo;

        // 重量超過チェック（保持中重量 + 容器 + 当て木 + 対象Bundle）
        int tmpWt = getJyuryoSum() + containerJyuryo + dunnageJyuryo + jyuryo;
        if (tmpWt > maxContainerJyuryo) {
            return "積載重量を超過します";
        }

        // 既に出荷済チェック（C#はCONTAINER_IDが空でない、ここは null/非null）
        if (e.containerId != null) {
            return "既に出荷済です";
        }

        // 20行制限（読み取り数の上限）
        if (dataList.size() >= MAX_ROWS) {
            return "20行までしか読取できません";
        }

        // 予約No相違（先頭行の予約Noと今回の予約Noを比較）
        if (!dataList.isEmpty()) {
            String firstBooking = safeStr(dataList.values().iterator().next().bookingNo);
            String curBooking = safeStr(e.bookingNo);

            if (!firstBooking.equals(curBooking)) {
                return "予約№が相違しています";
            }
        }

        // 問題なし
        return "";
    }

    // ============================================================
    // C#：AddBundleNo（bundleNo空のものだけ更新）
    // ============================================================

    //=====================================
    //　機　能　:　bundle Noを追加する
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　　　　　:　bundleNoOrg ..... String
    //　戻り値　:　[void] ..... なし
    //=====================================
    public void addBundleNo(@NonNull String heatNo,
                            @NonNull String sokuban,
                            @NonNull String bundleNoOrg) {

        // C#：ToInt32(bundleNoOrg).ToString().PadLeft(4,' ') を再現（4桁右寄せスペース埋め）
        String padded = padLeft4AsSpaces(bundleNoOrg);

        // bundleNoが空のものだけ更新（DB側の条件更新に委譲）
        syukkaMeisaiDao.updateBundleNoIfEmpty(heatNo, sokuban, padded);
    }

    // ============================================================
    // C#：AddBundle（DB→保持→Work→表示更新）
    // ============================================================

    //=================================
    //　機　能　:　bundleを追加する
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[void] ..... なし
    //=================================
    public void addBundle(@NonNull String heatNo, @NonNull String sokuban) {

        // DBから追加対象の出荷束明細を取得
        SyukkaMeisaiEntity e = syukkaMeisaiDao.findOneForAdd(heatNo, sokuban);
        if (e == null) {
            // checkBundle で弾いている想定だが、呼び出し順の違い等に備えて例外
            throw new IllegalStateException("出荷束明細に存在していません");
        }

        // 取得結果から保持用モデル(BundleInfo)を作成
        BundleInfo item = new BundleInfo();
        item.heatNo = safeStr(e.heatNo);
        item.sokuban = safeStr(e.sokuban);
        item.packingNo = safeStr(e.syukkaSashizuNo);
        item.bundleNo = safeStr(e.bundleNo);
        item.jyuryo = (e.jyuryo == null) ? 0 : e.jyuryo;
        item.bookingNo = safeStr(e.bookingNo);

        // 取消列（C# DataTableの "削除" 相当）
        item.torikeshi = "削除";

        // 保持データへ追加（順序維持）
        dataList.put(keyOf(heatNo, sokuban), item);

        // Normalモードの場合、Workテーブルへ反映（復元用）
        if (mode == Mode.Normal) {
            addWorkTable(item);
        }

        // 表示用行を再生成
        refreshDisplayRows();
    }

    // ============================================================
    // C#：RemoveBundle(row)
    // ============================================================

    //===============================
    //　機　能　:　bundleを削除する
    //　引　数　:　rowIndex ..... int
    //　戻り値　:　[void] ..... なし
    //===============================
    public void removeBundle(int rowIndex) {

        // 範囲外は何もしない
        if (rowIndex < 0 || rowIndex >= dataList.size()) {
            return;
        }

        // LinkedHashMap は index で直接取れないため keySet を配列化して取得
        String key = new ArrayList<>(dataList.keySet()).get(rowIndex);

        // 該当データを取得
        BundleInfo item = dataList.get(key);
        if (item == null) {
            return;
        }

        // Normalモードの場合、Workテーブルから削除
        if (mode == Mode.Normal) {
            removeWorkTable(item);
        }

        // 保持データから削除
        dataList.remove(key);

        // 表示用行を再生成
        refreshDisplayRows();
    }

    // ============================================================
    // C#：DeleteBundles()
    // ============================================================

    //============================
    //　機　能　:　bundlesを削除する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public void deleteBundles() {
        // 先頭から削除していく（内部でWork/表示も更新される）
        while (!dataList.isEmpty()) {
            removeBundle(0);
        }
    }

    // ============================================================
    // C#：refreshDataGrid()（表示行生成）
    // ============================================================

    //==============================
    //　機　能　:　display Rowsを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================
    private void refreshDisplayRows() {
        // 一旦クリアして作り直す
        displayRows.clear();

        // 保持データ順に表示行を生成
        for (BundleInfo item : dataList.values()) {

            // 表示する値を安全に取り出し
            String pNo = safeStr(item.packingNo);
            String bNo = safeStr(item.bundleNo);
            String idx = safeStr(item.sokuban);

            // 重量：3桁区切り + 左6桁幅スペース埋め（C#互換）
            String j = String.format(Locale.JAPAN, "%,d", item.jyuryo);
            if (j.length() < 6) {
                j = repeat(" ", 6 - j.length()) + j;
            }

            // 表示行を追加（取消列は固定で "削除"）
            displayRows.add(new BundleSelectRow(pNo, bNo, idx, j, "削除"));
        }
    }

    // ============================================================
    // C#：readWorkTblToList()
    // ============================================================

    //=====================================
    //　機　能　:　read Work Tbl To Listの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //=====================================
    private void readWorkTblToList() {

        // Work + 明細のJoin結果を取得（表示・チェックに必要な情報を含む想定）
        List<SyukkaMeisaiWorkDao.WorkJoinRow> rows = syukkaMeisaiWorkDao.selectWorkJoined();
        if (rows == null) {
            return;
        }

        // Workに残っている行を保持データへ復元
        for (SyukkaMeisaiWorkDao.WorkJoinRow r : rows) {

            // キー項目を安全に取得
            String heatNo = safeStr(r.heatNo);
            String sokuban = safeStr(r.sokuban);

            // 保持用キーを生成
            String k = keyOf(heatNo, sokuban);

            // 既に保持していない場合のみ追加
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

    //==================================
    //　機　能　:　work Tableを追加する
    //　引　数　:　item ..... BundleInfo
    //　戻り値　:　[void] ..... なし
    //==================================
    private void addWorkTable(@NonNull BundleInfo item) {

        // WorkEntity を作成
        SyukkaMeisaiWorkEntity w = new SyukkaMeisaiWorkEntity();
        w.heatNo = item.heatNo;
        w.sokuban = item.sokuban;

        // 必要なら保持（現状はC#互換で未使用のため null 運用）
        w.containerId = null;

        // 並び順/更新時刻用（C#のINSERT_YMD相当を updateYmd で代用）
        w.updateYmd = nowAsText();

        // upsert（存在すれば更新、なければ追加）
        syukkaMeisaiWorkDao.upsert(w);
    }

    //==================================
    //　機　能　:　work Tableを削除する
    //　引　数　:　item ..... BundleInfo
    //　戻り値　:　[void] ..... なし
    //==================================
    private void removeWorkTable(@NonNull BundleInfo item) {
        // heatNo+sokuban をキーにWorkの該当1件を削除
        syukkaMeisaiWorkDao.deleteOne(item.heatNo, item.sokuban);
    }

    // ============================================================
    // utils
    // ============================================================

    //=================================
    //　機　能　:　key Ofの処理
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[String] ..... 連結キー（heatNo+sokuban）
    //=================================
    private String keyOf(String heatNo, String sokuban) {
        return safeStr(heatNo) + safeStr(sokuban);
    }

    //==============================
    //　機　能　:　safe Strの処理
    //　引　数　:　s ..... String
    //　戻り値　:　[String] ..... nullなら空文字、非nullはそのまま
    //==============================
    private String safeStr(@Nullable String s) {
        return s == null ? "" : s;
    }

    //===================================
    //　機　能　:　pad Left4 As Spacesの処理
    //　引　数　:　org ..... String
    //　戻り値　:　[String] ..... 数値化→文字列化→4桁右寄せスペース埋め
    //===================================
    private String padLeft4AsSpaces(String org) {

        int n = 0;

        // 入力をトリムし、数値変換（不正値は 0 扱い）
        try {
            String t = org == null ? "" : org.trim();
            if (!t.isEmpty()) {
                n = Integer.parseInt(t);
            }
        } catch (Exception ignored) {
            // 変換できない場合は 0 のまま
        }

        // 数値→文字列
        String s = String.valueOf(n);

        // 4桁以上ならそのまま返す
        if (s.length() >= 4) {
            return s;
        }

        // 4桁未満は左スペース埋め
        return repeat(" ", 4 - s.length()) + s;
    }

    //==============================
    //　機　能　:　repeatの処理
    //　引　数　:　s ..... String
    //　　　　　:　n ..... int
    //　戻り値　:　[String] ..... s を n回繰り返した文字列
    //==============================
    private String repeat(String s, int n) {

        // 繰り返し回数が0以下の場合は空文字
        if (n <= 0) {
            return "";
        }

        // StringBuilderで連結
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }

        return sb.toString();
    }

    //==============================
    //　機　能　:　now As Textの処理
    //　引　数　:　なし
    //　戻り値　:　[String] ..... 現在時刻（yyyyMMddHHmmss）
    //==============================
    private String nowAsText() {
        // Workの並び順用の文字列時刻を作成
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
    }
}