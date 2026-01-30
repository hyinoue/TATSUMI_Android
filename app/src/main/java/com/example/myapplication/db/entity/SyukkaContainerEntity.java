package com.example.myapplication.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

//=================================
//　処理概要　:　出荷コンテナエンティティ
//=================================
@Entity(tableName = "T_SYUKKA_CONTAINER", primaryKeys = {"CONTAINER_ID"})
public class SyukkaContainerEntity {
    @ColumnInfo(name = "CONTAINER_ID")
    public Integer containerId;

    @ColumnInfo(name = "BOOKING_NO")
    public String bookingNo;

    @ColumnInfo(name = "CONTAINER_NO")
    public String containerNo;

    @ColumnInfo(name = "SEAL_NO")
    public String sealNo;

    @ColumnInfo(name = "CONTAINER_SIZE")
    public Integer containerSize;

    @ColumnInfo(name = "CONTAINER_JYURYO")
    public Integer containerJyuryo;

    @ColumnInfo(name = "DUNNAGE_JYURYO")
    public Integer dunnageJyuryo;

    @ColumnInfo(name = "SYUKKA_STATUS")
    public Integer syukkaStatus;

    @ColumnInfo(name = "DATA_SEND_YMDHMS")
    public String dataSendYmdhms; // DATE→TEXT(ISO)

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;
}
