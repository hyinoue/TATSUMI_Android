package com.example.myapplication.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

//=================================
//　処理概要　:　システム設定エンティティ
//=================================

@Entity(tableName = "M_SYSTEM", primaryKeys = {"RENBAN"})
public class SystemEntity {
    @ColumnInfo(name = "RENBAN")
    public Integer renban;

    @ColumnInfo(name = "WEB_SVC_URL")
    public String webSvcUrl;

    @ColumnInfo(name = "DEFAULT_CONTAINER_JYURYO")
    public Integer defaultContainerJyuryo;

    @ColumnInfo(name = "DEFAULT_DUNNAGE_JYURYO")
    public Integer defaultDunnageJyuryo;

    @ColumnInfo(name = "MAX_CONTAINER_JYURYO")
    public Integer maxContainerJyuryo;

    @ColumnInfo(name = "DATA_CONF_YMDHMS")
    public String dataConfYmdhms; // DATE→TEXT(ISO)

    @ColumnInfo(name = "DATA_RECV_YMDHMS")
    public String dataRecvYmdhms; // DATE→TEXT(ISO)

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd; // DATE→TEXT(ISO)
}
