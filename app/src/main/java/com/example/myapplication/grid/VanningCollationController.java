package com.example.myapplication.grid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.dao.KakuninMeisaiWorkDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.db.entity.KakuninMeisaiEntity;
import com.example.myapplication.db.entity.KakuninMeisaiWorkEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


//===========================================
//　処理概要　:　VanningCollationControllerクラス
//===========================================

public class VanningCollationController {

    private final KakuninMeisaiDao kakuninMeisaiDao;
    private final KakuninMeisaiWorkDao kakuninMeisaiWorkDao;
    private final List<KakuninMeisaiWorkEntity> details = new ArrayList<>();
    private final List<VanningCollationRow> displayRows = new ArrayList<>();
    //============================================================
    //　機　能　:　VanningCollationControllerの初期化処理
    //　引　数　:　kakuninMeisaiDao ..... KakuninMeisaiDao
    //　　　　　:　kakuninMeisaiWorkDao ..... KakuninMeisaiWorkDao
    //　戻り値　:　[VanningCollationController] ..... なし
    //============================================================

    public VanningCollationController(@NonNull KakuninMeisaiDao kakuninMeisaiDao,
                                      @NonNull KakuninMeisaiWorkDao kakuninMeisaiWorkDao) {
        this.kakuninMeisaiDao = kakuninMeisaiDao;
        this.kakuninMeisaiWorkDao = kakuninMeisaiWorkDao;
    }
    //=====================================
    //　機　能　:　loadの処理
    //　引　数　:　containerId ..... String
    //　戻り値　:　[void] ..... なし
    //=====================================

    public void load(@Nullable String containerId) {
        details.clear();
        if (containerId != null) {
            setT_KAKUNIN_MEISAItoW_KAKUNIN_MEISAI(containerId);
            readW_KAKUNIN_MEISAItoList();
        }
        refreshDisplayRows();
    }

    //=====================================================
    //　機　能　:　detailsを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninMeisaiWorkEntity>] ..... なし
    //=====================================================
    @NonNull
    public List<KakuninMeisaiWorkEntity> getDetails() {
        return Collections.unmodifiableList(details);
    }

    //=================================================
    //　機　能　:　display Rowsを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<VanningCollationRow>] ..... なし
    //=================================================
    @NonNull
    public List<VanningCollationRow> getDisplayRows() {
        return Collections.unmodifiableList(displayRows);
    }
    //=================================
    //　機　能　:　check Soku Dtlの処理
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[String] ..... なし
    //=================================

    public String checkSokuDtl(String heatNo, String sokuban) {
        heatNo = heatNo != null ? heatNo.trim() : "";
        sokuban = sokuban != null ? sokuban.trim() : "";

        if (isBlank(heatNo) || isBlank(sokuban)) {
            return "照合対象に存在していません";
        }

        KakuninMeisaiWorkEntity entity =
                kakuninMeisaiWorkDao.findOne(heatNo, sokuban);

        if (entity == null) {
            return "照合対象に存在していません";
        }
        if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
            return "既に確認済みです";
        }
        return "OK";
    }
    //=================================
    //　機　能　:　syougoを更新する
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[void] ..... なし
    //=================================


    public void updateSyougo(String heatNo, String sokuban) {
        KakuninMeisaiWorkEntity entity = kakuninMeisaiWorkDao.findOne(heatNo, sokuban);
        if (entity == null) {
            return;
        }
        if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
            return;
        }
        entity.containerSyougoKakunin = true;
        entity.updateProcName = "VanningCollationController";
        entity.updateYmd = nowAsText();
        kakuninMeisaiWorkDao.upsert(entity);
        readW_KAKUNIN_MEISAItoList();
        refreshDisplayRows();
    }
    //====================================
    //　機　能　:　syougou Sumi Countを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... なし
    //====================================

    public int getSyougouSumiCount() {
        int count = 0;
        for (KakuninMeisaiWorkEntity entity : details) {
            if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                count++;
            }
        }
        return count;
    }
    //==================================
    //　機　能　:　uncollated Countを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... なし
    //==================================

    public int getUncollatedCount() {
        int count = 0;
        for (KakuninMeisaiWorkEntity entity : details) {
            if (!Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                count++;
            }
        }
        return count;
    }
    //==========================================================
    //　機　能　:　mark Container Collatedの処理
    //　引　数　:　kakuninContainerDao ..... KakuninContainerDao
    //　戻り値　:　[void] ..... なし
    //==========================================================

    public void markContainerCollated(@NonNull KakuninContainerDao kakuninContainerDao) {
        if (details.isEmpty()) return;
        KakuninMeisaiWorkEntity first = details.get(0);
        if (first.containerId == null) return;

        KakuninContainerEntity container = kakuninContainerDao.findByContainerId(first.containerId);
        if (container == null) return;

        container.containerSyougoKanryo = true;
        container.dataSendYmdhms = null;
        container.updateProcName = "VanningCollation";
        container.updateYmd = nowAsText();
        kakuninContainerDao.upsert(container);
    }
    //==============================
    //　機　能　:　display Rowsを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================

    private void refreshDisplayRows() {
        displayRows.clear();
        for (KakuninMeisaiWorkEntity entity : details) {
            String pNo = safeStr(entity.syukkaSashizuNo);
            String bNo = safeStr(entity.bundleNo);
            String idx = safeStr(entity.sokuban);
            String j = String.format(Locale.JAPAN, "%,d", entity.jyuryo != null ? entity.jyuryo : 0);
            if (j.length() < 6) j = repeat(" ", 6 - j.length()) + j;
            String confirmed = Boolean.TRUE.equals(entity.containerSyougoKakunin) ? "済" : "　";
            displayRows.add(new VanningCollationRow(pNo, bNo, idx, j, confirmed));
        }
    }
    //=====================================================
    //　機　能　:　t_KAKUNIN_MEISAIto W_KAKUNIN_MEISAIを設定する
    //　引　数　:　containerId ..... String
    //　戻り値　:　[void] ..... なし
    //=====================================================

    private void setT_KAKUNIN_MEISAItoW_KAKUNIN_MEISAI(@NonNull String containerId) {
        kakuninMeisaiWorkDao.deleteAll();
        List<KakuninMeisaiEntity> source = kakuninMeisaiDao.findByContainerId(containerId);
        for (KakuninMeisaiEntity entity : source) {
            kakuninMeisaiWorkDao.upsert(toWorkEntity(entity));
        }
    }
    //============================================
    //　機　能　:　read W_KAKUNIN_MEISAIto Listの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================

    private void readW_KAKUNIN_MEISAItoList() {
        details.clear();
        details.addAll(kakuninMeisaiWorkDao.findAllOrdered());
    }
    //===============================================
    //　機　能　:　to Work Entityの処理
    //　引　数　:　entity ..... KakuninMeisaiEntity
    //　戻り値　:　[KakuninMeisaiWorkEntity] ..... なし
    //===============================================

    private KakuninMeisaiWorkEntity toWorkEntity(KakuninMeisaiEntity entity) {
        KakuninMeisaiWorkEntity work = new KakuninMeisaiWorkEntity();
        work.heatNo = entity.heatNo != null ? entity.heatNo.trim() : null;
        work.sokuban = entity.sokuban != null ? entity.sokuban.trim() : null;
        work.syukkaSashizuNo = entity.syukkaSashizuNo;
        work.bundleNo = entity.bundleNo;
        work.jyuryo = entity.jyuryo;
        work.containerId = entity.containerId;
        work.containerSyougoKakunin = entity.containerSyougoKakunin;
        work.kakuninContainerId = entity.kakuninContainerId;
        work.kakuninStatus = entity.kakuninStatus;
        work.insertProcName = entity.insertProcName;
        work.insertYmd = entity.insertYmd;
        work.updateProcName = entity.updateProcName;
        work.updateYmd = entity.updateYmd;
        work.deleteFlg = entity.deleteFlg;
        return work;
    }
    //===============================
    //　機　能　:　safe Strの処理
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... なし
    //===============================

    private String safeStr(String value) {
        return value == null ? "" : value;
    }
    //===============================
    //　機　能　:　blankを判定する
    //　引　数　:　value ..... String
    //　戻り値　:　[boolean] ..... なし
    //===============================

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    //==============================
    //　機　能　:　repeatの処理
    //　引　数　:　s ..... String
    //　　　　　:　n ..... int
    //　戻り値　:　[String] ..... なし
    //==============================

    private String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
    //==============================
    //　機　能　:　now As Textの処理
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //==============================

    private String nowAsText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(new Date());
    }
}
