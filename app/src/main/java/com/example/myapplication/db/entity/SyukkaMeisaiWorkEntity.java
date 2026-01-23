package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "W_SYUKKA_MEISAI", primaryKeys = {"HEAT_NO", "SOKUBAN"})
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
