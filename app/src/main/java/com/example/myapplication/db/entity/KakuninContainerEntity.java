package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "T_KAKUNIN_CONTAINER", primaryKeys = {"CONTAINER_ID"})
public class KakuninContainerEntity {

    @NonNull
    @ColumnInfo(name = "CONTAINER_ID")
    public String containerId;

    @ColumnInfo(name = "CONTAINER_NO")
    public String containerNo;


    @ColumnInfo(name = "BUNDLE_CNT")
    public Integer bundleCnt;

    @ColumnInfo(name = "SAGYOU_YMD")
    public String sagyouYmd; // DATE→TEXT

    @ColumnInfo(name = "CONTAINER_SYOUGO_KANRYO")
    public Boolean containerSyougoKanryo; // BIT→INTEGER

    @ColumnInfo(name = "DATA_SEND_YMDHMS")
    public String dataSendYmdhms; // DATE→TEXT

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd; // DATE→TEXT

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;
    
}