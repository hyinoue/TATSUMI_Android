package com.example.myapplication.grid;

import androidx.annotation.NonNull;

import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.time.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//============================================================
//　処理概要　:　CollateContainerSelectControllerクラス
//============================================================

public class CollateContainerSelectController {

    private final KakuninContainerDao kakuninContainerDao;
    private final List<KakuninContainerEntity> containers = new ArrayList<>();
    private final List<CollateContainerRow> displayRows = new ArrayList<>();

    private String selectedContainerId;
    private String selectedContainerNo;
    private int selectedBundleCnt;
    private String selectedSagyouYmd;

    public CollateContainerSelectController(@NonNull KakuninContainerDao kakuninContainerDao) {
        this.kakuninContainerDao = kakuninContainerDao;
    }

    public void loadContainers() {
        containers.clear();
        containers.addAll(kakuninContainerDao.findUncollated());
        refreshDisplayRows();
    }

    @NonNull
    public List<KakuninContainerEntity> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    @NonNull
    public List<CollateContainerRow> getDisplayRows() {
        return Collections.unmodifiableList(displayRows);
    }

    @NonNull
    public String checkSelectedNo(int selectedNo) {
        if (selectedNo <= 0 || selectedNo > containers.size()) {
            return "照合対象№が存在しません";
        }
        return "OK";
    }

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

    public String getSelectedContainerId() {
        return selectedContainerId;
    }

    public String getSelectedContainerNo() {
        return selectedContainerNo;
    }

    public int getSelectedBundleCnt() {
        return selectedBundleCnt;
    }

    public String getSelectedSagyouYmd() {
        return selectedSagyouYmd;
    }

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

    private String safeStr(String value) {
        return value == null ? "" : value;
    }
}
