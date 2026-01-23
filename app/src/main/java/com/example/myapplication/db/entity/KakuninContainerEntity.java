package com.example.myapplication.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "T_KAKUNIN_CONTAINER", primaryKeys = {"KAKUNIN_CONTAINER_ID"})
public class KakuninContainerEntity {
    @ColumnInfo(name = "KAKUNIN_CONTAINER_ID")
    public Integer kakuninContainerId;

    @ColumnInfo(name = "BOOKING_NO")
    public String bookingNo;

    @ColumnInfo(name = "CONTAINER_NO")
    public String containerNo;

    @ColumnInfo(name = "SEAL_NO")
    public String sealNo;

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
    public Integer deleteFlg; // BIT→INTEGER
}
