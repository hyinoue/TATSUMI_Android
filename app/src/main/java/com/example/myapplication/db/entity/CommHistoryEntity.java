package com.example.myapplication.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

//========================
//　処理概要　:　通信履歴エンティティ
//========================
@Entity(
        tableName = "C_COMM_HISTORY",
        primaryKeys = {"START_YMDHMS", "END_YMDHMS", "PROC_NAME"}
)

//========================
//　処理概要　:　通信履歴エンティティ
//========================

public class CommHistoryEntity {
    @ColumnInfo(name = "START_YMDHMS")
    public String startYmdhms;

    @ColumnInfo(name = "END_YMDHMS")
    public String endYmdhms;

    @ColumnInfo(name = "PROC_NAME")
    public String procName;

    @ColumnInfo(name = "ARGUMENT")
    public String argument;

    @ColumnInfo(name = "RETURN_VALUE")
    public String returnValue;

    @ColumnInfo(name = "ERR_NUMBER")
    public String errNumber;

    @ColumnInfo(name = "ERR_DESCRIPTION")
    public String errDescription;
}
