package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "W_KAKUNIN_MEISAI", primaryKeys = {"HEAT_NO", "SOKUBAN"})
public class KakuninMeisaiWorkEntity {

    @NonNull
    @ColumnInfo(name = "HEAT_NO")
    public String heatNo;

    @NonNull
    @ColumnInfo(name = "SOKUBAN")
    public String sokuban;

    @ColumnInfo(name = "KAKUNIN_CONTAINER_ID")
    public Integer kakuninContainerId;

    @ColumnInfo(name = "KAKUNIN_STATUS")
    public Integer kakuninStatus;

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;

    @ColumnInfo(name = "DELETE_FLG")
    public Integer deleteFlg;

    @ColumnInfo(name = "BOOKING_NO")
    public String bookingNo;

    @ColumnInfo(name = "CONTAINER_ID")
    public Integer containerId;
}
