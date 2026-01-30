package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

//============================================================
//　処理概要　:　出荷明細エンティティ
//============================================================


@Entity(tableName = "T_SYUKKA_MEISAI", primaryKeys = {"HEAT_NO", "SOKUBAN"})

//============================================================
//　処理概要　:　出荷明細エンティティ
//============================================================

public class SyukkaMeisaiEntity {

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

    @ColumnInfo(name = "BOOKING_NO")
    public String bookingNo;

    @ColumnInfo(name = "CONTAINER_ID")
    public Integer containerId;

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;
}
