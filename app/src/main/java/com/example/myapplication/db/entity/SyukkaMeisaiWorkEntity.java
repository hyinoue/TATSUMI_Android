package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

//============================================================
//　処理概要　:　出荷明細ワークエンティティ
//============================================================
@Entity(tableName = "W_SYUKKA_MEISAI", primaryKeys = {"HEAT_NO", "SOKUBAN"})

//============================================================
//　処理概要　:　出荷明細ワークエンティティ
//============================================================
public class SyukkaMeisaiWorkEntity {

    @NonNull
    @ColumnInfo(name = "HEAT_NO")
    public String heatNo;

    @NonNull
    @ColumnInfo(name = "SOKUBAN")
    public String sokuban;

    @ColumnInfo(name = "CONTAINER_ID")
    public Integer containerId;

    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;
}
