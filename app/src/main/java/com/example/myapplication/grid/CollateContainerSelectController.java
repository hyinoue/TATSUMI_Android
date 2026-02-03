package com.example.myapplication.grid;

import androidx.annotation.NonNull;

import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.time.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//=================================================
//　処理概要　:　CollateContainerSelectControllerクラス
//=================================================

public class CollateContainerSelectController {

    private final KakuninContainerDao kakuninContainerDao;
    private final List<KakuninContainerEntity> containers = new ArrayList<>();
    private final List<CollateContainerRow> displayRows = new ArrayList<>();

    private String selectedContainerId;
    private String selectedContainerNo;
    private int selectedBundleCnt;
    private String selectedSagyouYmd;
    //==========================================================
    //　機　能　:　CollateContainerSelectControllerの初期化処理
    //　引　数　:　kakuninContainerDao ..... KakuninContainerDao
    //　戻り値　:　[CollateContainerSelectController] ..... なし
    //==========================================================

    public CollateContainerSelectController(@NonNull KakuninContainerDao kakuninContainerDao) {
        this.kakuninContainerDao = kakuninContainerDao;
    }
    //============================
    //　機　能　:　containersを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================

    public void loadContainers() {
        containers.clear();
        containers.addAll(kakuninContainerDao.findUncollated());
        refreshDisplayRows();
    }

    //====================================================
    //　機　能　:　containersを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninContainerEntity>] ..... なし
    //====================================================
    @NonNull
    public List<KakuninContainerEntity> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    //=================================================
    //　機　能　:　display Rowsを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<CollateContainerRow>] ..... なし
    //=================================================
    @NonNull
    public List<CollateContainerRow> getDisplayRows() {
        return Collections.unmodifiableList(displayRows);
    }

    //=================================
    //　機　能　:　check Selected Noの処理
    //　引　数　:　selectedNo ..... int
    //　戻り値　:　[String] ..... なし
    //=================================
    @NonNull
    public String checkSelectedNo(int selectedNo) {
        if (selectedNo <= 0 || selectedNo > containers.size()) {
            return "照合対象№が存在しません";
        }
        return "OK";
    }

    //=================================
    //　機　能　:　select Containerの処理
    //　引　数　:　selectedNo ..... int
    //　戻り値　:　[String] ..... なし
    //=================================
    @NonNull
    public String selectContainer(int selectedNo) {
        String check = checkSelectedNo(selectedNo);
        if (!"OK".equals(check)) {
            return check;
        }

        KakuninContainerEntity entity = containers.get(selectedNo - 1);
        selectedContainerId = safeStr(entity.containerId);
        selectedContainerNo = safeStr(entity.containerNo);
        selectedBundleCnt = entity.bundleCnt != null ? entity.bundleCnt : 0;
        selectedSagyouYmd = safeStr(entity.sagyouYmd);
        return "OK";
    }
    //=======================================
    //　機　能　:　selected Container Idを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //=======================================

    public String getSelectedContainerId() {
        return selectedContainerId;
    }
    //=======================================
    //　機　能　:　selected Container Noを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //=======================================

    public String getSelectedContainerNo() {
        return selectedContainerNo;
    }
    //=====================================
    //　機　能　:　selected Bundle Cntを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... なし
    //=====================================

    public int getSelectedBundleCnt() {
        return selectedBundleCnt;
    }
    //=====================================
    //　機　能　:　selected Sagyou Ymdを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... なし
    //=====================================

    public String getSelectedSagyouYmd() {
        return selectedSagyouYmd;
    }
    //==============================
    //　機　能　:　display Rowsを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================

    private void refreshDisplayRows() {
        displayRows.clear();
        for (int i = 0; i < containers.size(); i++) {
            KakuninContainerEntity entity = containers.get(i);
            displayRows.add(new CollateContainerRow(
                    String.valueOf(i + 1),
                    safeStr(entity.containerNo),
                    String.valueOf(entity.bundleCnt != null ? entity.bundleCnt : 0),
                    DateTimeFormatUtil.formatSagyouYmdForDisplay(entity.sagyouYmd)
            ));
        }
    }
    //===============================
    //　機　能　:　safe Strの処理
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... なし
    //===============================

    private String safeStr(String value) {
        return value == null ? "" : value;
    }
}
