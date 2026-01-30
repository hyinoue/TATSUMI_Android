package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

//============================================================
//　処理概要　:　予定エンティティ
//============================================================


@Entity(tableName = "T_YOTEI", primaryKeys = {"BOOKING_NO"})

//============================================================
//　処理概要　:　予定エンティティ
//============================================================

public class YoteiEntity {

    @NonNull
    @ColumnInfo(name = "BOOKING_NO")
    public String bookingNo;

    @ColumnInfo(name = "SAGYOU_YOTEI_YMD")
    public String sagyouYoteiYmd; // DATE→TEXT

    @ColumnInfo(name = "CONTAINER_COUNT")
    public Integer containerCount;

    @ColumnInfo(name = "GOUKEI_BUNDOLE")
    public Integer goukeiBundole;

    @ColumnInfo(name = "GOUKEI_JYURYO")
    public Integer goukeiJyuryo;

    @ColumnInfo(name = "KANRYO_CONTAINER")
    public Integer kanryoContainer;

    @ColumnInfo(name = "KANRYO_BUNDOLE")
    public Integer kanryoBundole;

    @ColumnInfo(name = "KANRYO_JYURYO")
    public Integer kanryoJyuryo;

    @ColumnInfo(name = "LAST_UPD_YMDHMS")
    public String lastUpdYmdhms; // DATE→TEXT

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd; // DATE→TEXT

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd; // DATE→TEXT
}
