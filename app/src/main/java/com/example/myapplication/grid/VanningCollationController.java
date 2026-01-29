package com.example.myapplication.grid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.db.entity.KakuninMeisaiEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VanningCollationController {

    private final KakuninMeisaiDao kakuninMeisaiDao;
    private final List<KakuninMeisaiEntity> details = new ArrayList<>();
    private final List<VanningCollationRow> displayRows = new ArrayList<>();

    public VanningCollationController(@NonNull KakuninMeisaiDao kakuninMeisaiDao) {
        this.kakuninMeisaiDao = kakuninMeisaiDao;
    }

    public void load(@Nullable String containerId) {
        details.clear();
        if (containerId != null) {
            details.addAll(kakuninMeisaiDao.findByContainerId(containerId));
        }
        refreshDisplayRows();
    }

    @NonNull
    public List<KakuninMeisaiEntity> getDetails() {
        return Collections.unmodifiableList(details);
    }

    @NonNull
    public List<VanningCollationRow> getDisplayRows() {
        return Collections.unmodifiableList(displayRows);
    }

    public String checkSokuDtl(String heatNo, String sokuban) {
        if (isBlank(heatNo) || isBlank(sokuban)) {
            return "照合対象が見つかりません";
        }
        for (KakuninMeisaiEntity entity : details) {
            if (heatNo.equals(safeStr(entity.heatNo)) && sokuban.equals(safeStr(entity.sokuban))) {
                return "OK";
            }
        }
        return "照合対象が見つかりません";
    }

    public void updateSyougo(String heatNo, String sokuban) {
        for (KakuninMeisaiEntity entity : details) {
            if (heatNo.equals(safeStr(entity.heatNo)) && sokuban.equals(safeStr(entity.sokuban))) {
                if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                    return;
                }
                entity.containerSyougoKakunin = true;
                entity.updateProcName = "VanningCollation";
                entity.updateYmd = nowAsText();
                kakuninMeisaiDao.upsert(entity);
                refreshDisplayRows();
                return;
            }
        }
    }

    public int getSyougouSumiCount() {
        int count = 0;
        for (KakuninMeisaiEntity entity : details) {
            if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                count++;
            }
        }
        return count;
    }

    public int getUncollatedCount() {
        int count = 0;
        for (KakuninMeisaiEntity entity : details) {
            if (!Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                count++;
            }
        }
        return count;
    }

    public void markContainerCollated(@NonNull KakuninContainerDao kakuninContainerDao) {
        if (details.isEmpty()) return;
        KakuninMeisaiEntity first = details.get(0);
        if (first.containerId == null) return;

        KakuninContainerEntity container = kakuninContainerDao.findByContainerId(first.containerId);
        if (container == null) return;

        container.containerSyougoKanryo = true;
        container.dataSendYmdhms = null;
        container.updateProcName = "VanningCollation";
        container.updateYmd = nowAsText();
        kakuninContainerDao.upsert(container);
    }

    private void refreshDisplayRows() {
        displayRows.clear();
        for (KakuninMeisaiEntity entity : details) {
            String pNo = safeStr(entity.syukkaSashizuNo);
            String bNo = safeStr(entity.bundleNo);
            String idx = safeStr(entity.sokuban);
            String j = String.format(Locale.JAPAN, "%,d", entity.jyuryo != null ? entity.jyuryo : 0);
            if (j.length() < 6) j = repeat(" ", 6 - j.length()) + j;
            String confirmed = Boolean.TRUE.equals(entity.containerSyougoKakunin) ? "○" : "";
            displayRows.add(new VanningCollationRow(pNo, bNo, idx, j, confirmed));
        }
    }

    private String safeStr(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private String nowAsText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(new Date());
    }
}