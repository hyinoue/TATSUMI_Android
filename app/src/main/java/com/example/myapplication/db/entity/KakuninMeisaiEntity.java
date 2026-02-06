package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

//========================
//　処理概要　:　確認明細エンティティ
//========================

@Entity(tableName = "T_KAKUNIN_MEISAI", primaryKeys = {"HEAT_NO", "SOKUBAN"})

//========================
//　処理概要　:　確認明細エンティティ
//========================

public class KakuninMeisaiEntity {

    @NonNull
    @ColumnInfo(name = "HEAT_NO")
    public String heatNo;

    @NonNull
    @ColumnInfo(name = "SOKUBAN")
    public String sokuban;

    @ColumnInfo(name = "SYUKKA_SASHIZU_NO")
    public String syukkaSashizuNo;

    @ColumnInfo(name = "BUNDLE_NO")
    public String bundleNo;

    @ColumnInfo(name = "JYURYO")
    public Integer jyuryo;

    @ColumnInfo(name = "CONTAINER_ID")
    public String containerId;

    @ColumnInfo(name = "CONTAINER_SYOUGO_KAKUNIN")
    public Boolean containerSyougoKakunin; // BIT→INTEGER

    @Ignore
    public Integer kakuninContainerId;

    @Ignore
    public Integer kakuninStatus;

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;

    @Ignore
    public Integer deleteFlg;

}
