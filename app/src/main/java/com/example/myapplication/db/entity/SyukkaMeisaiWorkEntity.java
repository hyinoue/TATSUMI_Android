package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

//===========================
//　処理概要　:　出荷明細ワークエンティティ
//===========================
@Entity(tableName = "W_SYUKKA_MEISAI", primaryKeys = {"HEAT_NO", "SOKUBAN"})

//===========================
//　処理概要　:　出荷明細ワークエンティティ
//===========================
public class SyukkaMeisaiWorkEntity {

    @NonNull
    @ColumnInfo(name = "HEAT_NO")
    public String heatNo;

    @NonNull
    @ColumnInfo(name = "SOKUBAN")
    public String sokuban;

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;

    @Ignore
    public Integer containerId;

    @Ignore
    public String updateYmd;
}
